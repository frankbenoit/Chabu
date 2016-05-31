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
package org.chabu.prot.v1;

import org.chabu.prot.v1.internal.ChabuChannelImpl;
import org.chabu.prot.v1.internal.ChabuImpl;
import org.chabu.prot.v1.internal.ChabuReceiver;
import org.chabu.prot.v1.internal.ChabuXmitter;
import org.chabu.prot.v1.internal.Setup;
import org.chabu.prot.v1.internal.Utils;

/**
 * To configure an IChabu instance, this class is used.
 * <pre><code>
 *     IChabu chabu = ChabuBuilder
 *     .start( 0x123, "MyApplication", ChabuBuilder.DEFAULT_CHABU_RECVSZ, ChabuBuilder.DEFAULT_PRIORITY_COUNT )
 *     .addChannel( 0, DEFAULT_CHANNEL_RECVSZ, DEFAULT_PRIORITY, new org.chabu.ChabuChannelUser() )
 *     .addXmitRequest( this::myXmitListener )
 *     .build();
 * </code></pre>
 */
public final class ChabuBuilder {

	public static final int DEFAULT_CHABU_RECVSZ   = 1400;
	public static final int DEFAULT_CHANNEL_RECVSZ = 10000;
	public static final int DEFAULT_PRIORITY_COUNT = 1;
	public static final int DEFAULT_PRIORITY       = 0;

	private ChabuImpl chabu;
	private int nextChannelId;
	private int priorityCount;

	private ChabuBuilder( ChabuSetupInfo ci, int priorityCount ){
		this.priorityCount = priorityCount;
		ChabuXmitter xmitter = new ChabuXmitter();
		ChabuReceiver receiver = new ChabuReceiver();
		Setup setup = new Setup( xmitter, xmitter );
		chabu = new ChabuImpl( xmitter, receiver, setup, ci );
	}

	/**
	 * Create a new instance of ChabuBuilder which is used to create a IChabu instance.
	 * Use subsequence calls on the returned instance and as the last call #build() to get the IChabu instance.
	 *
	 * @param applicationVersion the application specific version number, seen by the communication
	 *                           partner.
	 * @param applicationName the application specific name, seen by the communication partner.
	 *
	 * @param recvBufferSz the size of the receive buffer held in chabu. This defines the maximum
	 *                     size of packets received.
	 *
	 * @param priorityCount the number of priorities that is maintained by chabu. The allowed value
	 *                      in #addChannel is then 0 .. (priorityCount-1)
	 *
	 * @return this ChabuBuilder instance. Use for fluent API style.
	 */
	public static ChabuBuilder start( int applicationVersion, String applicationName, int recvBufferSz, int priorityCount ){
		ChabuSetupInfo ci = new ChabuSetupInfo( recvBufferSz, applicationVersion, applicationName );
		return new ChabuBuilder(ci, priorityCount);
	}

	/**
	 * Add a channel to prepared IChabu instance.
	 *
	 * @param channelId must be given in correct sequence, starting with  zero for the first
	 *                  channel. This is redundant, but helps to avoid wrong use.
	 *
	 * @param recvBufferSize the size of the buffer that shall be held within chabu for this
	 *                       channel.
	 *
	 * @param priority gives the priority for xmit data. Channels with the same priority will send
	 *                 in a round robin. The channel with the lower priority number is handled
	 *                 first.
	 *
	 * @param user is the interface to the user code. Give an implementation of IChabuChannelUser.
	 *
	 * @return this ChabuBuilder instance. Use for fluent API style.
	 *
	 */
	public ChabuBuilder addChannel( int channelId, int recvBufferSize, int priority, ChabuRecvByteTarget recvTarget, ChabuXmitByteSource xmitSource ){
		Utils.ensure( channelId == this.nextChannelId, ChabuErrorCode.CONFIGURATION_CH_ID, "Channel ID must be ascending, expected %s, but was %s", this.nextChannelId, channelId );
		ChabuChannelImpl channel = new ChabuChannelImpl( recvBufferSize, priority, recvTarget, xmitSource );
		chabu.addChannel( channel );
		recvTarget.setChannel(channel);
		xmitSource.setChannel(channel);
		this.nextChannelId++;
		return this;
	}
	public ChabuBuilder addChannel( int channelId, int recvBufferSize, int priority, ChabuByteExchange byteExchange ){
		return addChannel(channelId, recvBufferSize, priority, byteExchange, byteExchange);
	}

	/**
	 * A single IChabuConnectingValidator instance can be configured. This is optional.
	 * When chabu is about to accept the connection, it calls this validator, so it can verify it
	 * can work with the application version number and name given by the communication partner.
	 *
	 * @param val the instance of the validator. Must not be <code>null</code>.
	 *
	 * @return this ChabuBuilder instance. Use for fluent API style.
	 */
	public ChabuBuilder setConnectionValidator( ChabuConnectingValidator val ) {
		chabu.setConnectingValidator( val );
		return this;
	}

	/**
	 * Add a listener that is called, whenever chabu needs to send data. This is used in the
	 * application to configure a selector for OP_WRITE interest.
	 *
	 * @param r callback
	 *
	 * @return this ChabuBuilder instance. Use for fluent API style.
	 */
	public ChabuBuilder addXmitRequestListener( Runnable r ) {
		chabu.addXmitRequestListener(r);
		return this;
	}

	/**
	 * Last call to the ChabuBuilder instance, creating the IChabu instance with the information
	 * given in the previous calls.
	 *
	 * @return the IChabu instance.
	 */
	public Chabu build() {
		ChabuImpl res = chabu;
		chabu = null;
		res.activate(priorityCount);
		return res;
	}
}
