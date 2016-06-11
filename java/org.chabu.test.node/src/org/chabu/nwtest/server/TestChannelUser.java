package org.chabu.nwtest.server;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.chabu.PseudoRandom;
import org.chabu.nwtest.Const;
import org.chabu.nwtest.Parameter;
import org.chabu.nwtest.ParameterValue;
import org.chabu.nwtest.XferItem;
import org.chabu.nwtest.XferItem.Category;
import org.chabu.prot.v1.ChabuByteExchange;
import org.chabu.prot.v1.ChabuChannel;

class TestChannelUser implements ChabuByteExchange {
	
	private ChabuChannel    channel;
	private PseudoRandom     xmitRandom;
	private PseudoRandom     recvRandom;
	
	ByteBuffer xmitBuffer = ByteBuffer.allocate(1000);
	ByteBuffer recvBuffer = ByteBuffer.allocate(1000);
	
	private AtomicInteger  xmitPending        = new AtomicInteger();
	private long           xmitStreamPosition = 0;
	private AtomicInteger  recvPending        = new AtomicInteger();
	private long           recvStreamPosition = 0;
	private Consumer<String> errorReporter;
	
	public TestChannelUser(Consumer<String> errorReporter ) {
		this.errorReporter = errorReporter;
	}
	@Override
	public void setChannel(ChabuChannel channel) {
		this.channel = channel;
		xmitRandom = new PseudoRandom(channel.getChannelId()*2+0);
		recvRandom = new PseudoRandom(channel.getChannelId()*2+1);
	}

	public void addXmitAmount( int amount ){
		//System.out.printf("chabu xmit addXmitAmount %d\n", amount );
		xmitPending.addAndGet(amount);
		channel.addXmitLimit(amount);
		if( Const.LOG_TIMING) NwtUtil.log("evUserXmitRequest" );
	}
	public void addRecvAmount( int amount ){
		recvPending.addAndGet(amount);
		channel.addRecvLimit(amount);
	}
	public XferItem getState() {
		XferItem xi = new XferItem();
		xi.setCategory(Category.EVT);
		xi.setName("Status");
		xi.setCallIndex(-1);
		xi.setParameters(new Parameter[]{
				new ParameterValue("Channel", channel.getChannelId()),
				new ParameterValue("RecvPending", recvPending.get()),
				new ParameterValue("RecvStreamPosition", recvStreamPosition),
				new ParameterValue("XmitPending", xmitPending.get()),
				new ParameterValue("XmitStreamPosition", xmitStreamPosition)
		});
		return xi;
	}
	public void ensureCompleted() {
		if( recvPending.get() > 0 ){
			errorReporter.accept(String.format("Channel[%d] evRecv data not available, missing %d bytes @0x%04X", channel.getChannelId(), recvPending.get(), recvStreamPosition ));
		}
		if( xmitPending.get() > 0 ){
			errorReporter.accept(String.format("Channel[%d] xmit data still pending. %d bytes @0x%04X", channel.getChannelId(), xmitPending.get(), xmitStreamPosition ));
		}
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
//		System.out.printf("srv: %d TX %s%n", channel.getChannelId(), TestUtils.toHexString(xmitBuffer.array(), 0, 8, true));
		return xmitBuffer;
	}

	@Override
	public void xmitCompleted() {
	}

	@Override
	public ByteBuffer getRecvBuffer(int size) {
		if( recvPending.get() == 0 ){
			return null;
		}
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
		//System.out.printf("srv: %d RX %s%n", channel.getChannelId(), TestUtils.toHexString(recvBuffer.array(), 0, 8, true));
		int putSz = recvBuffer.remaining();
		if( Const.DATA_RANDOM ){
			recvRandom.nextBytesVerify(recvBuffer.array(), recvBuffer.arrayOffset()+recvBuffer.position(), recvBuffer.remaining(), "Channel[%d] evRecv data corruption", channel.getChannelId() );
		}
		recvStreamPosition+=putSz;
		recvPending.addAndGet(-putSz);
	}
	@Override
	public void xmitReset() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void recvReset() {
		// TODO Auto-generated method stub
		
	}
	public int getChannelId() {
		return channel.getChannelId();
	}
	public long getSumRecved() {
		return recvStreamPosition;
	}
	public long getSumXmitted() {
		return xmitStreamPosition;
	}
	
}