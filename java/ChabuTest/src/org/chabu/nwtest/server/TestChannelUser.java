package org.chabu.nwtest.server;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.chabu.ByteExchange;
import org.chabu.ChabuChannel;
import org.chabu.ChabuChannelUser;
import org.chabu.PseudoRandom;
import org.chabu.container.ByteQueueOutputPort;
import org.chabu.nwtest.Const;
import org.json.JSONObject;

class TestChannelUser implements ByteExchange {
	
	private ChabuChannel    channel;
	private final PseudoRandom     xmitRandom;
	private final PseudoRandom     recvRandom;
	
	private byte[] recvTestBytes = new byte[0x2000];
	
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
	@Override
	public void recvEvent(ByteQueueOutputPort queue) {

		//System.out.printf("chabu recv %d\n", bufferToConsume.remaining() );

		int putSz = Math.min( queue.available(), recvPending.get() );
		if( Const.DATA_RANDOM ){
			if( recvTestBytes.length < putSz ){
				recvTestBytes = new byte[ putSz ];
			}
			recvRandom.nextBytes(recvTestBytes, 0, putSz);
			boolean ok = true;
			for( int i = 0; i < putSz; i++ ){
				byte exp = queue.readByte();
				if( ok && recvTestBytes[i] != exp ){
					errorReporter.accept( String.format("Channel[%d] evRecv data corruption recv:0x%02X expt:0x%02X @0x%04X", channel.getChannelId(), exp, recvTestBytes[i], recvStreamPosition ));
				}
			}
		}
		else {
			queue.skip(putSz);
		}
		recvStreamPosition+=putSz;
		recvPending.addAndGet(-putSz);
		queue.commit();
	}
	@Override
	public boolean xmitEvent(ByteBuffer bufferToFill) {
		
		int putSz = Math.min( bufferToFill.remaining(), xmitPending.get() );
		xmitPending.addAndGet(-putSz);
		
		if( Const.DATA_RANDOM ){
			xmitRandom.nextBytes( bufferToFill.array(), bufferToFill.arrayOffset()+bufferToFill.position(), putSz );
		}
		
		bufferToFill.position(bufferToFill.position() + putSz );
		xmitStreamPosition+=putSz;
		
		if( xmitPending.get() > 0 ){
			channel.xmitRegisterRequest();
		}

		return false;
	}
	
	public void addXmitAmount( int amount ){
		//System.out.printf("chabu xmit addXmitAmount %d\n", amount );
		xmitPending.addAndGet(amount);
		channel.xmitRegisterRequest();
		if( Const.LOG_TIMING) NwtUtil.log("evUserXmitRequest" );
	}
	public void addRecvAmount( int amount ){
		recvPending.addAndGet(amount);
		channel.recvRegisterRequest();
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
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void xmitCompleted() {
		// TODO Auto-generated method stub
		
	}
	@Override
	public ByteBuffer getRecvBuffer(int size) {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void recvCompleted() {
		// TODO Auto-generated method stub
		
	}
	
}