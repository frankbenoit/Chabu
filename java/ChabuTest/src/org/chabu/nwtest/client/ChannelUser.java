package org.chabu.nwtest.client;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.chabu.ChabuChannel;
import org.chabu.ChabuChannelUser;
import org.chabu.PseudoRandom;
import org.chabu.container.ByteQueueOutputPort;
import org.chabu.nwtest.Const;

class ChannelUser implements ChabuChannelUser {
		ChabuChannel channel;
		
		private org.chabu.PseudoRandom xmitRandom;
		private org.chabu.PseudoRandom recvRandom;
		private byte[] recvTestBytes = new byte[0x2000];

		private long recvStreamPosition = 0;
		private long xmitStreamPosition = 0;
		private AtomicInteger recvPending = new AtomicInteger();
		private AtomicInteger xmitPending = new AtomicInteger();
		
		public void setChannel(ChabuChannel channel) {
			this.channel = channel;
			xmitRandom = new PseudoRandom(channel.getChannelId()*2 + 1 );
			recvRandom = new PseudoRandom(channel.getChannelId()*2 + 0 );
		}

		@Override
		public void recvEvent(ByteQueueOutputPort queue) {
			
//			int r = bufferToConsume.remaining();
			int putSz = Math.min( queue.availableUncommitted(), recvPending.get() );
			recvPending.addAndGet(-putSz);
			
			if( Const.DATA_RANDOM ){

				if( recvTestBytes.length < putSz ){
					recvTestBytes = new byte[ putSz ];
				}
				recvRandom.nextBytes(recvTestBytes, 0, putSz);
				boolean ok = true;
				for( int i = 0; i < putSz; i++ ){
					byte val = queue.readByte();
					if( ok && recvTestBytes[i] != val ){
						throw new RuntimeException( 
								String.format("Channel[%d] evRecv data corruption recv:0x%02X expt:0x%02X @0x%04X", 
										channel.getChannelId(), 
										val, 
										recvTestBytes[i], 
										recvStreamPosition ));
					}
				}
			}
			else {
				queue.skip(putSz);
			}
			recvStreamPosition+=putSz;
			queue.commit();
		}

		@Override
		public boolean xmitEvent(ByteBuffer bufferToFill) {
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
				channel.xmitRegisterRequest();
			}
//			System.out.printf("xmit %d\n", r - bufferToFill.remaining() );
			return false;
		}
		
		public void addXmitAmount( int amount ){
			xmitPending.addAndGet( amount );
			channel.xmitRegisterRequest();
		}
		public void addRecvAmount( int amount ){
			recvPending.addAndGet( amount );
			channel.recvRegisterRequest();
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