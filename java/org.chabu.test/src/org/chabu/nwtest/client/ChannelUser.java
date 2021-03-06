package org.chabu.nwtest.client;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.chabu.PseudoRandom;
import org.chabu.nwtest.Const;
import org.chabu.prot.v1.ChabuByteExchange;
import org.chabu.prot.v1.ChabuChannel;

class ChannelUser implements ChabuByteExchange {
	ChabuChannel channel;

	ByteBuffer xmitBuffer = ByteBuffer.allocate(1000);
	ByteBuffer recvBuffer = ByteBuffer.allocate(1000);
	private org.chabu.PseudoRandom xmitRandom;
	private org.chabu.PseudoRandom recvRandom;

	private long recvStreamPosition = 0;
	private long xmitStreamPosition = 0;
	private AtomicInteger recvPending = new AtomicInteger();
	private AtomicInteger xmitPending = new AtomicInteger();

	private int initialRecvAmount;

	public ChannelUser( int initialRecvAmount ){
		this.initialRecvAmount = initialRecvAmount;
	}
	public void setChannel(ChabuChannel channel) {
		if( this.channel == null ){
			channel.addRecvLimit(initialRecvAmount);
		}
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
//		System.out.printf("cli: TX %d %s%n", channel.getChannelId(), TestUtils.toHexString(xmitBuffer.array(), 0, 8, true));
		return xmitBuffer;
	}


	@Override
	public void xmitCompleted() {
	}

	@Override
	public void xmitReset() {
	}

	
	@Override
	public ByteBuffer getRecvBuffer(int size) {
		int putSz = Math.min( size, recvPending.get() );
		
		if( recvBuffer.capacity() < putSz ){
			recvBuffer = ByteBuffer.allocate(putSz);
		}
		recvBuffer.clear();
		recvBuffer.limit(putSz);
		return recvBuffer;
	}

	@Override
	public void recvCompleted() {
		recvBuffer.flip();
//		System.out.printf("cli: RX %d RX %s%n", channel.getChannelId(), TestUtils.toHexString(recvBuffer.array(), 0, 8, true));
		int putSz = recvBuffer.remaining();
		if( Const.DATA_RANDOM ){
			recvRandom.nextBytesVerify(recvBuffer.array(), recvBuffer.arrayOffset()+recvBuffer.position(), recvBuffer.remaining(), "Channel[%d] evRecv data corruption", channel.getChannelId() );
		}
		recvStreamPosition+=putSz;
		recvPending.addAndGet(-putSz);
	}

	@Override
	public void recvReset() {

	}
}