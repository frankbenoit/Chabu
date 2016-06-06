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
package org.chabu.prot.v1.internal;

import java.io.IOException;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.chabu.prot.v1.Chabu;
import org.chabu.prot.v1.ChabuChannel;
import org.chabu.prot.v1.ChabuConnectingValidator;
import org.chabu.prot.v1.ChabuErrorCode;
import org.chabu.prot.v1.ChabuSetupInfo;


/**
 * 
 * @author Frank Benoit
 */
public final class ChabuImpl implements Chabu {
	
	public  static final int SEQ_MIN_SZ = 20;

	private final ArrayList<ChabuChannelImpl> channels = new ArrayList<>(256);
	
	private final ChabuXmitter xmitter;	
	
	private final ChabuReceiver receiver;	
	
	private boolean activated = false;

	private final Setup setup;
	
	public ChabuImpl( ChabuXmitter xmitter, ChabuReceiver receiver, Setup setup, ChabuSetupInfo info ){
		
		this.xmitter = xmitter;
		this.receiver = receiver;
		this.setup = setup;
		
		Utils.ensure( info.recvPacketSize >= Constants.MAX_RECV_LIMIT_LOW, ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_TOO_LOW, 
				"maxReceiveSize must be at least 0x100, but is %s", info.recvPacketSize );
		
		Utils.ensure( info.recvPacketSize <= Constants.MAX_RECV_LIMIT_HIGH, ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_TOO_HIGH, 
				"maxReceiveSize must be max 0x%X, but is %s", Constants.MAX_RECV_LIMIT_HIGH, info.recvPacketSize );
		
		Utils.ensure( Utils.isAligned4( info.recvPacketSize ), ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_NOT_ALIGNED, 
				"maxReceiveSize must 4-byte aligned: %s", info.recvPacketSize );
		
		Utils.ensure( info.applicationProtocolName != null, ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME_NULL, 
				"applicationName must not be null" );
		
		int nameBytes = info.applicationProtocolName.getBytes( StandardCharsets.UTF_8).length;
		Utils.ensure( nameBytes <= Constants.APV_MAX_LENGTH, ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME_TOO_LONG, 
				"applicationName must have length at maximum 200 UTF8 bytes, but has %s", nameBytes );
		
		setup.setLocal(info);
		receiver.initRecvBuf( info.recvPacketSize );
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
	public void handleChannel(ByteChannel channel) throws IOException {
		receiver.recv(channel);
		xmitter.xmit(channel);
	}
	
	void channelXmitRequestArm(int channelId){
		xmitter.channelXmitRequestArm(channelId);
	}
//	public void recvArmShallBeXmitted(ChabuChannelImpl channel) {
//		xmitter.recvArmShallBeXmitted(channel);
//	}


	void channelXmitRequestData(int channelId){
		xmitter.channelXmitRequestData(channelId);
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

}
