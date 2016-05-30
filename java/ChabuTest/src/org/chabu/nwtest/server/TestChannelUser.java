package org.chabu.nwtest.server;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.chabu.PseudoRandom;
import org.chabu.nwtest.Const;
import org.chabu.prot.v1.ByteExchange;
import org.chabu.prot.v1.ChabuChannel;
import org.json.JSONObject;

class TestChannelUser implements ByteExchange {
	
	private ChabuChannel    channel;
	private final PseudoRandom     xmitRandom;
	private final PseudoRandom     recvRandom;
	
	private byte[] recvTestBytes = new byte[0x2000];
	ByteBuffer xmitBuffer = ByteBuffer.allocate(1000);
	ByteBuffer recvBuffer = ByteBuffer.allocate(1000);
	
	private AtomicInteger  xmitPending        = new AtomicInteger();
	private long           xmitStreamPosition = 0;
	private AtomicInteger  recvPending        = new AtomicInteger();
	private long           recvStreamPosition = 0;
	private Consumer<String> errorReporter;
	
	public TestChannelUser(int channelId, int xmitBufferSz, Consumer<String> errorReporter ) {
		this.errorReporter = errorReporter;
		xmitRandom = new PseudoRandom(channelId*2+0);
		recvRandom = new PseudoRandom(channelId*2+1);
	}
	@Override
	public void setChannel(ChabuChannel channel) {
		this.channel = channel;
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
	public JSONObject getState() {
		return new JSONObject()
		.put("Channel", channel.getChannelId())
		.put("RecvPending", recvPending.get())
		.put("RecvStreamPosition", recvStreamPosition)
		.put("XmitPending", xmitPending.get())
		.put("XmitStreamPosition", xmitStreamPosition);
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
		return recvBuffer;
	}

	@Override
	public void xmitCompleted() {
	}

	@Override
	public ByteBuffer getRecvBuffer(int size) {
		int putSz = Math.min( size, recvPending.get() );
		if( recvBuffer.capacity() < putSz ){
			recvBuffer = ByteBuffer.allocate(putSz);
		}
		recvBuffer.clear();
		recvBuffer.limit(size);
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
					errorReporter.accept( String.format("Channel[%d] evRecv data corruption recv:0x%02X expt:0x%02X @0x%04X", channel.getChannelId(), exp, recvTestBytes[i], recvStreamPosition ));
				}
			}
		}
		else {
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
	
}