/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit, Stuttgart, Germany <fr@nk-benoit.de>
 * 
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 * 
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
namespace Org.Chabu.Prot.V1
{

    using System;
    using Runnable = System.Action;
    using Org.Chabu.Prot.V1.Internal;
    using System.Collections.Generic;

    ///
    ///<summary>To configure an IChabu instance, this class is used.</summary>
    ///<pre><code>
    ///    IChabu chabu = ChabuBuilder
    ///    .start( 0x123, "MyApplication", ChabuBuilder.DEFAULT_CHABU_RECVSZ, ChabuBuilder.DEFAULT_PRIORITY_COUNT )
    ///    .addChannel( 0, DEFAULT_CHANNEL_RECVSZ, DEFAULT_PRIORITY, new org.chabu.ChabuChannelUser() )
    ///    .addXmitRequest( this::myXmitListener )
    ///    .build();
    ///</code></pre>
    ///
    public sealed class ChabuBuilder {

	public static readonly int DEFAULT_CHABU_RECVSZ   = 1400;
	public static readonly int DEFAULT_CHANNEL_RECVSZ = 10000;
    public static readonly int DEFAULT_PRIORITY_COUNT = 1;
    public static readonly int MAX_PRIORITY_COUNT     = 20;
    public static readonly int DEFAULT_PRIORITY       = 0;

	private int nextChannelId;
	private readonly Runnable _xmitRequestListener;
	private ChabuConnectingValidator _connectingValidator;
	private readonly List<ChabuChannelImpl> _channels = new List<ChabuChannelImpl>(20);
	private readonly ChabuSetupInfo _localSetupInfo;
	private readonly int _priorityCount;

	public static String GetChabuVersion(){
		var major = Constants.PROTOCOL_VERSION >> 16;
		var minor = Constants.PROTOCOL_VERSION & 0xFFFF;
		return $"{major}.{minor:D2}";
	}
	
	private ChabuBuilder( ChabuSetupInfo localSetupInfo, int priorityCount, Runnable xmitListener ){
		this._localSetupInfo = localSetupInfo;
		this._priorityCount = priorityCount;
		this._xmitRequestListener = xmitListener;
        }

	///
    ///<summary>Create a new instance of ChabuBuilder which is used to create a IChabu instance.</summary> 
	/// Use subsequence calls on the returned instance and as the last call #build() to get the IChabu instance.
	///
    /// <param name="applicationVersion">the application specific version number, seen by the communication partner.</param> 
    /// <param name="applicationProtocolName">the application specific name, seen by the communication partner.</param> 
    /// <param name="recvPacketSize">the size of the receive buffer held in chabu. This defines the maximum size of packets received.</param> 
    /// <param name="priorityCount">the number of priorities that is maintained by chabu. The allowed value in #addChannel is then 0 .. (_priorityCount-1)</param> 
    /// <param name="xmitListener">Listener called, when chabu want to be notified on a write possiblitiy.</param> 
    /// <returns>this ChabuBuilder instance. Use for fluent API style.</returns>
    /// 
	public static ChabuBuilder Start( int applicationVersion, String applicationProtocolName, int recvPacketSize, int priorityCount, Runnable xmitListener ){
		var ci = new ChabuSetupInfo( recvPacketSize, applicationVersion, applicationProtocolName );
		return new ChabuBuilder(ci, priorityCount, xmitListener);
    }

    ///
    ///<summary>Add a channel to prepared IChabu instance.</summary> 
    /// 
    /// <param name="channelId"> must be given in correct sequence, starting with  zero for the first
    /// channel. This is redundant, but helps to avoid wrong use.</param> 
    /// <param name="priority"> gives the priority for xmit data. Channels with the same priority will send
    /// in a round robin. The channel with the lower priority number is handled 
    /// first.</param>
    /// <param name="recvTarget">The adapter to pass the received data</param> 
    /// <param name="xmitSource">The adapter to pass the data to xmit</param> 
    /// <returns>this ChabuBuilder instance. Use for fluent API style.</returns>
    /// 
    public ChabuBuilder AddChannel( int channelId, int priority, ChabuRecvByteTarget recvTarget, ChabuXmitByteSource xmitSource ){
		Utils.ensure( channelId == this.nextChannelId, ChabuErrorCode.CONFIGURATION_CH_ID, "Channel ID must be ascending, expected {0}, but was {1}", this.nextChannelId, channelId );
		var channel = new ChabuChannelImpl( priority, recvTarget, xmitSource );
		_channels.add( channel );
		this.nextChannelId++;
		return this;
	}

    ///
    ///<summary>Add a channel to prepared IChabu instance.</summary> 
    /// 
    /// <param name="channelId"> must be given in correct sequence, starting with  zero for the first
    /// channel. This is redundant, but helps to avoid wrong use.</param> 
    /// <param name="priority"> gives the priority for xmit data. Channels with the same priority will send
    /// in a round robin. The channel with the lower priority number is handled 
    /// first.</param>
    /// <param name="byteExchange">The adapter to pass the received data and the data to xmit</param> 
    /// <returns>this ChabuBuilder instance. Use for fluent API style.</returns>
    /// 
    public ChabuBuilder AddChannel( int channelId, int priority, ChabuByteExchange byteExchange ){
		return AddChannel(channelId, priority, byteExchange, byteExchange);
        }

    ///
    ///<summary>A single IChabuConnectingValidator instance can be configured. This is optional.</summary> 
    /// 
    /// When chabu is about to accept the connection, it calls this validator, so it can verify it
    /// can work with the application version number and name given by the communication partner.
    ///
    /// <param name="connectingValidator">the instance of the validator. Must not be <c>null</c>.</param>
    ///
    /// <returns>this ChabuBuilder instance. Use for fluent API style.</returns>
    ///
    public ChabuBuilder SetConnectionValidator( ChabuConnectingValidator connectingValidator ) {
		Utils.ensure( connectingValidator != null, ChabuErrorCode.CONFIGURATION_VALIDATOR, "passed in null");
		Utils.ensure( this._connectingValidator == null, ChabuErrorCode.CONFIGURATION_VALIDATOR, "already set. Only one is allowed.");
		this._connectingValidator = connectingValidator;
		return this;
	}

	/**
	 * Last call to the ChabuBuilder instance, creating the IChabu instance with the information
	 * given in the previous calls.
	 *
	 * @return the IChabu instance.
	 */
	public Chabu Build() {
		var factory = new ChabuFactory();
		return new ChabuImpl( factory, _localSetupInfo, _priorityCount, _channels, _xmitRequestListener, _connectingValidator );
	}
}
}