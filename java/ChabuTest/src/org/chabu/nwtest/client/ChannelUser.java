package org.chabu.nwtest.client;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.chabu.IChabuChannel;
import org.chabu.IChabuChannelUser;
import org.chabu.Random;

class ChannelUser implements IChabuChannelUser {
		private IChabuChannel channel;
		
		private org.chabu.Random xmitRandom;
		private org.chabu.Random recvRandom;
		
		private long recvStreamPosition = 0;
		private long xmitStreamPosition = 0;
		private AtomicInteger recvPending = new AtomicInteger();
		private AtomicInteger xmitPending = new AtomicInteger();
		
		public void setChannel(IChabuChannel channel) {
			this.channel = channel;
			xmitRandom = new Random(channel.getChannelId()*2 + 1 );
			recvRandom = new Random(channel.getChannelId()*2 + 0 );
		}

		public void evRecv(ByteBuffer bufferToConsume) {
			
//			int r = bufferToConsume.remaining();
			
			while( bufferToConsume.hasRemaining() && recvPending.get() > 0 ){
				int recvByte = bufferToConsume.get() & 0xFF;
				int exptByte = recvRandom.nextInt() & 0xFF;
				if( recvByte != exptByte ){
					throw new RuntimeException(String.format("Channel[%d] evRecv data corruption recv:0x%02X expt:0x%02X @0x%04X", channel.getChannelId(), recvByte, exptByte, recvStreamPosition ));
				}
				recvStreamPosition++;
				recvPending.decrementAndGet();
			}
//			System.out.printf("recv %d\n", r-bufferToConsume.remaining() );
//			if( recvPending.get() > 0 ){
//				throw new RuntimeException(String.format("Channel[%d] evRecv data not available, missing %d bytes @0x%04X", channel.getChannelId(), recvPending.get(), recvStreamPosition ));
//			}
		}

		public boolean evXmit(ByteBuffer bufferToFill) {
//			int r = bufferToFill.remaining();
			while( bufferToFill.hasRemaining() && xmitPending.get() > 0 ){
				bufferToFill.put( (byte)xmitRandom.nextInt() );
				xmitStreamPosition++;
				xmitPending.decrementAndGet();
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
	}