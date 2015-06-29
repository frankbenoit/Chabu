package org.chabu.nwtest.client;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.chabu.IChabuChannel;
import org.chabu.IChabuChannelUser;
import org.chabu.PseudoRandom;
import org.chabu.nwtest.Const;

class ChannelUser implements IChabuChannelUser {
		IChabuChannel channel;
		
		private org.chabu.PseudoRandom xmitRandom;
		private org.chabu.PseudoRandom recvRandom;
		private byte[] recvTestBytes = new byte[0x2000];

		private long recvStreamPosition = 0;
		private long xmitStreamPosition = 0;
		private AtomicInteger recvPending = new AtomicInteger();
		private AtomicInteger xmitPending = new AtomicInteger();
		
		public void setChannel(IChabuChannel channel) {
			this.channel = channel;
			xmitRandom = new PseudoRandom(channel.getChannelId()*2 + 1 );
			recvRandom = new PseudoRandom(channel.getChannelId()*2 + 0 );
		}

		public void evRecv(ByteBuffer bufferToConsume) {
			
//			int r = bufferToConsume.remaining();
			
			if( Const.DATA_RANDOM ){
				int putSz = Math.min( bufferToConsume.remaining(), recvPending.get() );
				if( recvTestBytes.length < putSz ){
					recvTestBytes = new byte[ putSz ];
				}
				recvRandom.nextBytes(recvTestBytes, 0, putSz);
				boolean ok = true;
				byte[] readArray = bufferToConsume.array();
				int readIdx = bufferToConsume.arrayOffset() + bufferToConsume.position();
				for( int i = 0; i < putSz; i++ , readIdx++ ){
					if( ok && recvTestBytes[i] != readArray[ readIdx ] ){
						throw new RuntimeException( String.format("Channel[%d] evRecv data corruption recv:0x%02X expt:0x%02X @0x%04X", channel.getChannelId(), readArray[ readIdx ], recvTestBytes[i], recvStreamPosition ));
					}
				}
				recvPending.addAndGet(-putSz);
				bufferToConsume.position(bufferToConsume.position() + putSz );
				recvStreamPosition+=putSz;
			}
			else {
				int putSz = Math.min( bufferToConsume.remaining(), recvPending.get() );
				recvPending.addAndGet(-putSz);
				bufferToConsume.position(bufferToConsume.position() + putSz );
				recvStreamPosition+=putSz;
			}
//			System.out.printf("recv %d\n", r-bufferToConsume.remaining() );
//			if( recvPending.get() > 0 ){
//				throw new RuntimeException(String.format("Channel[%d] evRecv data not available, missing %d bytes @0x%04X", channel.getChannelId(), recvPending.get(), recvStreamPosition ));
//			}
		}

		public boolean evXmit(ByteBuffer bufferToFill) {
//			int r = bufferToFill.remaining();
			if( Const.DATA_RANDOM ){
				int putSz = Math.min( bufferToFill.remaining(), xmitPending.get() );
				xmitPending.addAndGet(-putSz);
				xmitRandom.nextBytes( bufferToFill.array(), bufferToFill.arrayOffset()+bufferToFill.position(), putSz );
				bufferToFill.position(bufferToFill.position() + putSz );
				xmitStreamPosition+=putSz;
			}
			else {
				int putSz = Math.min( bufferToFill.remaining(), xmitPending.get() );
				xmitPending.addAndGet(-putSz);
				bufferToFill.position(bufferToFill.position() + putSz );
				xmitStreamPosition+=putSz;
			}
			if( xmitPending.get() > 0 ){
				channel.evUserXmitRequest();
			}
//			System.out.printf("xmit %d\n", r - bufferToFill.remaining() );
			return false;
		}
		
		public void addXmitAmount( int amount ){
			xmitPending.addAndGet( amount );
			channel.evUserXmitRequest();
		}
		public void addRecvAmount( int amount ){
			recvPending.addAndGet( amount );
			channel.evUserRecvRequest();
		}

		public void ensureCompleted() {
			if( recvPending.get() != 0 ){
				throw new RuntimeException(String.format("Channel[%d] recv missing %d bytes @0x%04X", channel.getChannelId(), recvPending.get(), recvStreamPosition ));
			}
			if( xmitPending.get() != 0 ){
				throw new RuntimeException(String.format("Channel[%d] xmit data not yet sent, %d bytes remaining @0x%04X", channel.getChannelId(), xmitPending.get(), xmitStreamPosition ));
			}
		}

		public boolean hasPending() {
			return ( recvPending.get() != 0 ) || ( xmitPending.get() != 0 );
		}

		public long getRecvStreamPosition() {
			return recvStreamPosition;
		}
		public int getRecvPending() {
			return recvPending.get();
		}
		public long getXmitStreamPosition() {
			return xmitStreamPosition;
		}
		public int getXmitPending() {
			return xmitPending.get();
		}
		
		@Override
		public String toString() {
			return String.format("xmit:%s recv:%s %s", xmitPending, recvPending, channel );
		}
	}