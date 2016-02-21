/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit, Stuttgart, Germany <keinfarbton@gmail.com>
 * 
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 * 
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
package org.chabu.internal;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.chabu.Chabu;
import org.chabu.ChabuChannel;
import org.chabu.ChabuConnectingValidator;
import org.chabu.ChabuErrorCode;
import org.chabu.ChabuSetupInfo;


/**
 * 
 * @author Frank Benoit
 */
public final class ChabuImpl implements Chabu {

	private final ArrayList<ChabuChannelImpl> channels = new ArrayList<>(256);
	
	private final ChabuXmitter xmitter;	
	
	private final ChabuReceiver receiver;	
	
	private boolean activated = false;

	private final Setup setup;
	
	private PrintWriter traceWriter;

	
	public ChabuImpl( ChabuXmitter xmitter, ChabuReceiver receiver, Setup setup, ChabuSetupInfo info ){
		
		this.xmitter = xmitter;
		this.receiver = receiver;
		this.setup = setup;
		
		Utils.ensure( info.maxReceiveSize >= 0x100, ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE, 
				"maxReceiveSize must be at least 0x100, but is %s", info.maxReceiveSize );
		
		Utils.ensure( Utils.isAligned4( info.maxReceiveSize ), ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE, 
				"maxReceiveSize must 4-byte aligned: %s", info.maxReceiveSize );
		
		Utils.ensure( info.applicationName != null, ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME, 
				"applicationName must not be null" );
		
		int nameBytes = info.applicationName.getBytes( StandardCharsets.UTF_8).length;
		Utils.ensure( nameBytes <= 200, ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME, 
				"applicationName must have length at maximum 200 UTF8 bytes, but has %s", nameBytes );
		
		setup.setLocal(info);
		receiver.initRecvBuf( info.maxReceiveSize );
		
	}
	
	/**
	 * The firmware does not allow to add channels during the operational time.
	 * The ModuleLink feature need to allow to add further channels during the runtime.
	 * The Cte must ensure itself that the channel is available at firmware side.
	 * @param channel
	 */
	public void addChannel( ChabuChannelImpl channel ){
		channels.add(channel);
	}
	
	/**
	 * When activate is called, org.chabu enters operation. No subsequent calls to {@link #addChannel(ChabuChannelImpl)} or {@link #setPriorityCount(int)} are allowed.
	 */
	public void activate( int priorityCount){
		consistencyChecks();
		xmitter.activate(priorityCount, channels, setup, Priorizer::new );
		activateAllChannels();
		activated = true;
		xmitter.processXmitSetup();
		receiver.activate( channels, xmitter, setup );
	}

	private void consistencyChecks() {
		Utils.ensure( !activated, ChabuErrorCode.ASSERT, "activated called twice" );
		Utils.ensure( channels.size() > 0, ChabuErrorCode.CONFIGURATION_NO_CHANNELS, "No channels are set." );
	}

	private void activateAllChannels() {
		for( int i = 0; i < channels.size(); i++ ){
			ChabuChannelImpl ch = channels.get(i);
			ch.activate(this, i );
		}
	}

	@Override
	public void recv(ByteBuffer buf) {
		receiver.recv(buf);
	}

	@Override
	public void xmit(ByteBuffer buf) {
		xmitter.xmit( buf );
	}

	void channelXmitRequestArm(int channelId){
		xmitter.channelXmitRequestArm(channelId);
	}

	void channelXmitRequestData(int channelId){
		xmitter.channelXmitRequestData(channelId);
	}

	@Override
	public void setTracePrinter(PrintWriter writer) {
		this.traceWriter = writer;
		xmitter.setTracePrinter(writer);
		receiver.setTracePrinter(writer);
	}

	public PrintWriter getTraceWriter() {
		return traceWriter;
	}

	public void setConnectingValidator(ChabuConnectingValidator val) {
		Utils.ensure( val      != null, ChabuErrorCode.CONFIGURATION_VALIDATOR, 
				"ConnectingValidator passed in is null" );
		Utils.ensure( setup.getValidator() == null, ChabuErrorCode.CONFIGURATION_VALIDATOR, 
				"ConnectingValidator is already set" );
		setup.setValidator(val);
	}

	@Override
	public int getChannelCount() {
		return channels.size();
	}

	@Override
	public ChabuChannel getChannel(int channelId) {
		return channels.get(channelId);
	}

	@Override
	public String toString() {
		return String.format("Chabu[ recv:%s xmit:%s ]", receiver, xmitter );
	}

	@Override
	public void addXmitRequestListener(Runnable r) {
		xmitter.addXmitRequestListener(r);
	}

	void processXmitArm(int channelId, int recvArm) {
		xmitter.processXmitArm(channelId, recvArm);
	}

	void processXmitSeq(int channelId, int xmitSeq, int armSize, ConsumerByteBuffer userBuffer) {
		xmitter.processXmitSeq(channelId, xmitSeq, armSize, userBuffer);		
	}
}
