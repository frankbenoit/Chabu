package org.chabu.nwtest.server;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.chabu.IChabuChannel;
import org.chabu.IChabuChannelUser;
import org.chabu.Random;
import org.json.JSONObject;

class ChabuChannelUser implements IChabuChannelUser {
	
	private IChabuChannel    channel;
	private final Random     xmitRandom;
	private final Random     recvRandom;
	
	private AtomicInteger  xmitPending        = new AtomicInteger();
	private long           xmitStreamPosition = 0;
	private AtomicInteger  recvPending        = new AtomicInteger();
	private long           recvStreamPosition = 0;
	private Consumer<String> errorReporter;
	
	public ChabuChannelUser(int channelId, int xmitBufferSz, Consumer<String> errorReporter ) {
		this.errorReporter = errorReporter;
		xmitRandom = new Random(channelId*2+0);
		recvRandom = new Random(channelId*2+1);
	}
	@Override
	public void setChannel(IChabuChannel channel) {
		this.channel = channel;
	}
	@Override
	public void evRecv(ByteBuffer bufferToConsume) {
		
		System.out.printf("chabu recv %d\n", bufferToConsume.remaining() );
		
		while( bufferToConsume.hasRemaining() && recvPending.get() > 0 ){
			int recvByte = bufferToConsume.get() & 0xFF;
			int exptByte = recvRandom.nextInt() & 0xFF;
			if( recvByte != exptByte ){
				errorReporter.accept( String.format("Channel[%d] evRecv data corruption recv:0x%02X expt:0x%02X @0x%04X", channel.getChannelId(), recvByte, exptByte, recvStreamPosition ));
			}
			recvStreamPosition++;
			recvPending.decrementAndGet();
		}
	}
	@Override
	public boolean evXmit(ByteBuffer bufferToFill) {
		int r = bufferToFill.remaining();
		while( bufferToFill.hasRemaining() && xmitPending.get() > 0 ){
			bufferToFill.put( (byte)xmitRandom.nextInt() );
			recvStreamPosition++;
			xmitPending.decrementAndGet();
		}
		System.out.printf("chabu xmit %d\n", r - bufferToFill.remaining() );
		return false;
	}
	
	public void addXmitAmount( int amount ){
		System.out.printf("chabu xmit addXmitAmount %d\n", amount );
		xmitPending.addAndGet(amount);
		channel.evUserXmitRequest();
	}
	public void addRecvAmount( int amount ){
		recvPending.addAndGet(amount);
		channel.evUserRecvRequest();
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
	
}