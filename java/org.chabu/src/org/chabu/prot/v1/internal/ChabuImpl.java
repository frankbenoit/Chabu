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

	private final ArrayList<ChabuChannelImpl> channels;
	private final Setup setup;
	private final ChabuFactory factory;
	private final SingleEventNotifierFromTwoSources notifierWhenRecvAndXmitCompletedStartup = new SingleEventNotifierFromTwoSources( this::eventCompletedStartup );
	private final AbortMessage xmitAbortMessage;
	private final int priorityCount;
	
	private ChabuXmitter xmitter;	
	private ChabuReceiver receiver;

	private Runnable xmitRequestListener;

	public ChabuImpl( ChabuFactory factory, ChabuSetupInfo localSetupInfo, int priorityCount, 
			ArrayList<ChabuChannelImpl> channels, Runnable xmitRequestListener, ChabuConnectingValidator connectingValidator ){

		this.xmitRequestListener = xmitRequestListener;
		verifyLocalSetup(localSetupInfo);
		xmitAbortMessage = new AbortMessage(xmitRequestListener);
		this.channels = channels;
		this.priorityCount = priorityCount;
		this.factory = factory;
		
		this.setup = new Setup(localSetupInfo, xmitAbortMessage, connectingValidator );
		
		this.xmitter = factory.createXmitterStartup(xmitAbortMessage, xmitRequestListener, setup, this::xmitCompletedStartup);
		
		this.receiver = factory.createReceiverStartup(xmitAbortMessage, setup, this::recvCompletedStartup);
		verifyPriorityCount();
		verifyChannels();
	}
	
	private void verifyPriorityCount() {
		Utils.ensure( priorityCount >= 1 && priorityCount <= 20, 
				ChabuErrorCode.CONFIGURATION_PRIOCOUNT, 
				"Priority count must be in range 1..20, but is %s", priorityCount );
	}

	private static void verifyLocalSetup(ChabuSetupInfo localSetupInfo) {
		
		Utils.ensure( localSetupInfo.recvPacketSize >= Constants.MAX_RECV_LIMIT_LOW, ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_TOO_LOW, 
				"maxReceiveSize must be at least 0x100, but is %s", localSetupInfo.recvPacketSize );
		
		Utils.ensure( localSetupInfo.recvPacketSize <= Constants.MAX_RECV_LIMIT_HIGH, ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_TOO_HIGH, 
				"maxReceiveSize must be max 0x%X, but is %s", Constants.MAX_RECV_LIMIT_HIGH, localSetupInfo.recvPacketSize );
		
		Utils.ensure( Utils.isAligned4( localSetupInfo.recvPacketSize ), ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_NOT_ALIGNED, 
				"maxReceiveSize must 4-byte aligned: %s", localSetupInfo.recvPacketSize );
		
		Utils.ensure( localSetupInfo.applicationProtocolName != null, ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME_NULL, 
				"applicationName must not be null" );
		
		int nameBytes = localSetupInfo.applicationProtocolName.getBytes( StandardCharsets.UTF_8).length;
		Utils.ensure( nameBytes <= Constants.APV_MAX_LENGTH, ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME_TOO_LONG, 
				"applicationName must have length at maximum 200 UTF8 bytes, but has %s", nameBytes );
	}

	private void verifyChannels() {
		Utils.ensure( channels.size() > 0, ChabuErrorCode.CONFIGURATION_NO_CHANNELS, "No channels are set." );

		for( ChabuChannelImpl ch : channels ){
			Utils.ensure( ch.getPriority() < priorityCount, ChabuErrorCode.CONFIGURATION_CH_PRIO, 
					"Channel %s has higher priority (%s) as the max %s", 
					ch.getChannelId(), ch.getPriority(), priorityCount );
		}

	}
	
	private void eventCompletedStartup(){
		activateAllChannels();
	}

	private void activateAllChannels() {
		for( int i = 0; i < channels.size(); i++ ){
			ChabuChannelImpl ch = channels.get(i);
			ch.activate(this, i );
		}
	}
	
	private void xmitCompletedStartup(){
		xmitter = factory.createXmitterNormal( xmitAbortMessage, xmitRequestListener, priorityCount, channels, Priorizer::new, setup.getRemoteMaxReceiveSize());
		notifierWhenRecvAndXmitCompletedStartup.event1();
	}
	
	private void recvCompletedStartup(){
		receiver = factory.createReceiverNormal( receiver, channels, xmitAbortMessage, setup);		
		notifierWhenRecvAndXmitCompletedStartup.event2();
	}
	
	void channelXmitRequestArm(int channelId){
		xmitter.channelXmitRequestArm(channelId);
	}

	void channelXmitRequestData(int channelId){
		xmitter.channelXmitRequestData(channelId);
	}
	
	/////////////////////////////////////////////////////////////////////////////////
	// public interface methods
	
	@Override
	public void handleChannel(ByteChannel channel) throws IOException {
		receiver.recv(channel);
		xmitter.xmit(channel);
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

}
