package org.chabu.nwtest.client;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.chabu.PseudoRandom;
import org.chabu.nwtest.Const;
import org.chabu.prot.v1.ByteExchange;
import org.chabu.prot.v1.ChabuChannel;

class ChannelUser implements ByteExchange {
	ChabuChannel channel;

	ByteBuffer xmitBuffer = ByteBuffer.allocate(1000);
	ByteBuffer recvBuffer = ByteBuffer.allocate(1000);
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

	public void addXmitAmount( int amount ){
		xmitPending.addAndGet( amount );
		channel.addXmitLimit( amount );
	}
	public void addRecvAmount( int amount ){
		recvPending.addAndGet( amount );
		channel.addRecvLimit( amount );
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

	@Override
	public ByteBuffer getXmitBuffer(int size) {
		int putSz = Math.min( size, xmitPending.get() );
		xmitPending.addAndGet(-putSz);
		if( xmitBuffer.capacity() < putSz ){
			xmitBuffer = ByteBuffer.allocate(putSz);
		}
		xmitBuffer.clear();
		if( Const.DATA_RANDOM ){
			xmitRandom.nextBytes( xmitBuffer.array(), xmitBuffer.arrayOffset()+xmitBuffer.position(), putSz );
		}
		xmitBuffer.limit(putSz);
		xmitStreamPosition+=putSz;
		return xmitBuffer;
	}


	@Override
	public void xmitCompleted() {
		// TODO Auto-generated method stub

	}

	@Override
	public void xmitReset() {
		// TODO Auto-generated method stub

	}

	
	@Override
	public ByteBuffer getRecvBuffer(int size) {
		int putSz = Math.min( size, recvPending.get() );
		
		if( recvBuffer.capacity() < putSz ){
			recvBuffer = ByteBuffer.allocate(putSz);
		}
		recvBuffer.clear();
		return recvBuffer;
	}

	@Override
	public void recvCompleted() {
		recvBuffer.flip();
		int putSz = recvBuffer.remaining();
		if( Const.DATA_RANDOM ){
			if( recvTestBytes.length < putSz ){
				recvTestBytes = new byte[ putSz ];
			}
			recvRandom.nextBytes(recvTestBytes, 0, putSz);
			boolean ok = true;
			for( int i = 0; i < putSz; i++ ){
				byte exp = recvBuffer.get();
				if( ok && recvTestBytes[i] != exp ){
					throw new RuntimeException( 
							String.format("Channel[%d] evRecv data corruption recv:0x%02X expt:0x%02X @0x%04X", 
									channel.getChannelId(), exp, recvTestBytes[i], recvStreamPosition ));
				}
			}
		}
		else {
		}
		recvStreamPosition+=putSz;
		recvPending.addAndGet(-putSz);
	}

	@Override
	public void recvReset() {
		// TODO Auto-generated method stub

	}
}