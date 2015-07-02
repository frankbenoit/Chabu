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
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;

import org.chabu.ChabuConnectionAcceptInfo;
import org.chabu.ChabuErrorCode;
import org.chabu.ChabuException;
import org.chabu.ChabuSetupInfo;
import org.chabu.Chabu;
import org.chabu.ChabuChannel;
import org.chabu.ChabuConnectingValidator;


/**
*
* @author Frank Benoit
*/
public final class ChabuImpl implements Chabu {

	private static final int   PT_MAGIC = 0x77770000;

	private static final int   ABORT_MSGLEN_MAX = 48;

	public static final String PROTOCOL_NAME    = "CHABU";
	
	/**
	 * Number constant for the current procotcol version.<br/>
	 * Actually this is version 0.1.
	 */
	public static final int    PROTOCOL_VERSION = 0x0000_0001;

	private ArrayList<ChabuChannelImpl> channels = new ArrayList<>(256);
	
	private int      priorityCount = 1;
	private BitSet[] xmitChannelRequestData;
	private BitSet[] xmitChannelRequestArm;
	private int      xmitLastChannelIdx = 0;
	
	private ByteBuffer xmitBuf = ByteBuffer.allocate( 0x100 );
	private ByteBuffer recvBuf;

	private Runnable xmitRequestListener;
	private boolean  xmitRequestPending = false;
	
	/**
	 * The setup data is completely received.
	 */
	private RecvState recvSetupCompleted = RecvState.WAITING;
	/**
	 * The startup data is completely sent.
	 */
	private XmitState xmitStartupCompleted = XmitState.IDLE;

	
	private boolean activated = false;

	/**
	 * Have sent the ACCEPT packet
	 */
	private XmitState xmitAccepted = XmitState.IDLE;
	/**
	 * Have recv the ACCEPT packet
	 */
	private RecvState recvAccepted = RecvState.IDLE;

	int maxChannelId;
//	String instanceName = "Chabu";
	
	private ChabuSetupInfo infoLocal;
	private ChabuSetupInfo infoRemote;

	private XmitState    xmitAbortPending = XmitState.IDLE;
	private int          xmitAbortCode    = 0;
	private String       xmitAbortMessage = "";

	
	private PrintWriter traceWriter;

	private ChabuConnectingValidator val;
	
	public ChabuImpl( ChabuSetupInfo info ){
		
		Utils.ensure( info.maxReceiveSize >= 0x100, ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE, 
				"maxReceiveSize must be at least 0x100, but is %s", info.maxReceiveSize );
		
		Utils.ensure( ( info.maxReceiveSize & 3 ) == 0, ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE, 
				"maxReceiveSize must 4-byte aligned: %s", info.maxReceiveSize );
		
		Utils.ensure( info.applicationName != null, ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME, 
				"applicationName must not be null" );
		
		int nameBytes = info.applicationName.getBytes( StandardCharsets.UTF_8).length;
		Utils.ensure( nameBytes <= 200, ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME, 
				"applicationName must have length at maximum 200 UTF8 bytes, but has %s", nameBytes );
		
		this.infoLocal = info;
		
		recvBuf = ByteBuffer.allocate( info.maxReceiveSize );
		recvBuf.order(ByteOrder.BIG_ENDIAN);
		recvBuf.clear();
		
		xmitBuf.order(ByteOrder.BIG_ENDIAN);
		xmitBuf.clear().limit(0);
		
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
	public void setPriorityCount( int priorityCount ){
		Utils.ensure( !activated, ChabuErrorCode.IS_ACTIVATED, "Priority count cannot be set when alread activated." );
		Utils.ensure( priorityCount >= 1 && priorityCount <= 20, ChabuErrorCode.CONFIGURATION_PRIOCOUNT, "Priority count must be in range 1..20, but is %s", priorityCount );
		this.priorityCount = priorityCount;
	}
	
	/**
	 * Priority that forces the processing to be done with highest possible priority.
	 * Lower priority values result in later processing.
	 */
	public int getHighestValidPriority(){
		return priorityCount-1;
	}
	
	/**
	 * When activate is called, org.chabu enters operation. No subsequent calls to {@link #addChannel(ChabuChannelImpl)} or {@link #setPriorityCount(int)} are allowed.
	 */
	public void activate(){
		Utils.ensure( !activated, ChabuErrorCode.ASSERT, "activated called twice" );
		Utils.ensure( channels.size() > 0, ChabuErrorCode.CONFIGURATION_NO_CHANNELS, "No channels are set." );
		
		xmitChannelRequestData = new BitSet[ priorityCount ];
		xmitChannelRequestArm  = new BitSet[ priorityCount ];
		for (int i = 0; i < xmitChannelRequestData.length; i++) {
			xmitChannelRequestData[i] = new BitSet(channels.size());
			xmitChannelRequestArm [i] = new BitSet(channels.size());
		}
		
		for( int i = 0; i < channels.size(); i++ ){
			ChabuChannelImpl ch = channels.get(i);
			Utils.ensure( ch.getPriority() < priorityCount, ChabuErrorCode.CONFIGURATION_CH_PRIO, "Channel %s has higher priority (%s) as the max %s", i, ch.getPriority(), priorityCount );
			ch.activate(this, i );
		}
		
		maxChannelId = channels.size() -1;
		activated = true;
		
		processXmitSetup();
	}
	
//	@Override
	public int recv(ByteChannel channel) {
		// TODO Add impl
		return 0;
	}

	@Override
	public void recv(ByteBuffer buf) {
		// prepare trace
		PrintWriter trc = traceWriter;
		int trcStartPos = buf.position();
		
		// start real work
		
		int oldRemaining = -1;
		while( oldRemaining != buf.remaining() ){
			oldRemaining = buf.remaining();
			

			// ensure we have len+type
			Utils.transferUpTo( buf, recvBuf, 8 );
			if( recvBuf.position() < 8 ){
				break;
			}
			
			int ps = recvBuf.getInt(0);
			if( ps > recvBuf.capacity() ){
				delayedAbort(ChabuErrorCode.PROTOCOL_LENGTH, String.format("Packet with too much data: len %s", ps ));
				// set all recv to be consumed.
				buf.position( buf.limit() );
			}

			recvBuf.limit(ps);
			Utils.transferRemaining(buf, recvBuf);


			if( !recvBuf.hasRemaining() ){
				
				// completed, now start processing
				recvBuf.flip();
				recvBuf.position(4);

				try{
					
					int packetTypeId = recvBuf.getInt() & 0xFF;
					PacketType packetType = PacketType.findPacketType(packetTypeId);
					if( packetType == null ){
						delayedAbort( ChabuErrorCode.PROTOCOL_PCK_TYPE, String.format("Packet type cannot be found 0x%02X", packetTypeId ));
						return;
					}

					if( recvSetupCompleted != RecvState.RECVED && packetType != PacketType.SETUP ){
						delayedAbort( ChabuErrorCode.PROTOCOL_EXPECTED_SETUP, String.format("Recveived %s, but SETUP was expected", packetType ));
						return;
					}
					
					switch( packetType ){
					case SETUP : processRecvSetup();  break;
					case ACCEPT: processRecvAccept(); break; 
					case ABORT : processRecvAbort();  break; 
					case ARM   : processRecvArm();    break; 
					case SEQ   : processRecvSeq();    break; 
					default    : throw new ChabuException(String.format("Packet type 0x%02X unexpected: ps %s", packetTypeId, ps ));
					}
		
					if( recvBuf.hasRemaining() ){
						throw new ChabuException(String.format("Packet type 0x%02X left some bytes unconsumed: %s bytes", packetTypeId, recvBuf.remaining() ));
					}
				}
				finally {
					recvBuf.clear();
				}
			}
		}
		
		// write out trace info
		if( trc != null ){
			trc.printf( "WIRE_RX: {}%n");
			Utils.printTraceHexData(trc, buf, trcStartPos, buf.position());
		}
	}

	private String protocolVersionToString( int version ){
		return String.format("%d.%d", version >>> 16, version & 0xFFFF );
	}
	private void processRecvSetup() {
		
		/// when is startupRx set before?
		Utils.ensure( recvSetupCompleted != RecvState.RECVED, ChabuErrorCode.PROTOCOL_SETUP_TWICE, "Recveived SETUP twice" );
		Utils.ensure( activated, ChabuErrorCode.NOT_ACTIVATED, "While receiving the SETUP block, org.chabu was not activated." );

		String pn = getRecvString();
		if( !PROTOCOL_NAME.equals(pn) ) {
			delayedAbort( ChabuErrorCode.SETUP_REMOTE_CHABU_NAME.getCode(), String.format("Chabu protocol name mismatch. Expected %s, received %d", PROTOCOL_NAME, pn ));
			return;
		}
		
		int pv = recvBuf.getInt();
		
		int rs = recvBuf.getInt();
		int av = recvBuf.getInt();
		String an = getRecvString();

		ChabuSetupInfo info = new ChabuSetupInfo( rs, av, an );

		this.infoRemote = info;

		recvSetupCompleted = RecvState.RECVED;	

		if(( pv >>> 16 ) != (PROTOCOL_VERSION >>> 16 )) {
			delayedAbort( ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION.getCode(), String.format("Chabu protocol version mismatch. Expected %s, received %s", 
					protocolVersionToString(PROTOCOL_VERSION), protocolVersionToString(pv) ));
			return;
		}
				
		checkConnectingValidator();
	}

	private void processRecvAccept() {
		Utils.ensure( recvAccepted != RecvState.RECVED, ChabuErrorCode.PROTOCOL_ACCEPT_TWICE, "Recveived ACCEPT twice" );
		recvAccepted = RecvState.RECVED;
	}

	private void processRecvAbort() {
		
		int code =  recvBuf.getInt();
		String message = getRecvString();
		
		throw new ChabuException( ChabuErrorCode.REMOTE_ABORT, code, String.format("Recveived ABORT Code=0x%08X: %s", code, message ));
	}

	private void processRecvArm() {
		
		Utils.ensure( recvAccepted == RecvState.RECVED, ChabuErrorCode.ASSERT, "" );
		
		if( recvBuf.limit() != 16 ){
			throw new ChabuException(String.format("Packet type ARM with unexpected len field: %s", 16 ));
		}

		int channelId = recvBuf.getInt();
		int arm       = recvBuf.getInt();

		ChabuChannelImpl channel = channels.get(channelId);
		channel.handleRecvArm(arm);
	}

	private void processRecvSeq() {
		final int MIN_SZ = 20;
		if( recvBuf.limit() < MIN_SZ ){
			throw new ChabuException(String.format("Packet type SEQ with unexpected len field (too small): %s", recvBuf.limit() ));
		}

		int channelId = recvBuf.getInt();
		int seq       = recvBuf.getInt();
		int pls       = recvBuf.getInt();

		if( channelId >= channels.size() ){
			throw new ChabuException(String.format("Packet type SEQ with invalid channel ID %s, available channels %s", channelId, channels.size() ));
		}

		if( recvBuf.limit() != Utils.alignUpTo4( MIN_SZ+pls )){
			throw new ChabuException(String.format("Packet type SEQ with unexpected len field: %s, PLS %s", recvBuf.limit(), pls ));
		}

		ChabuChannelImpl channel = channels.get(channelId);
		channel.handleRecvSeq( seq, recvBuf, pls );
	}
	private void checkConnectingValidator() {
		if( xmitAccepted != XmitState.XMITTED && recvSetupCompleted == RecvState.RECVED && xmitStartupCompleted == XmitState.XMITTED ){
			
			if( infoRemote.maxReceiveSize < 0x100 || infoRemote.maxReceiveSize >= 0x10000 ){
			delayedAbort(ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE.getCode(), 
					String.format("MaxReceiveSize must be on range 0x100 .. 0xFFFF bytes, but SETUP from remote contained 0x%02X", 
					infoRemote.maxReceiveSize));
			}
			Utils.ensure( ( infoRemote.maxReceiveSize & 3 ) == 0, ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE, 
					"maxReceiveSize must 4-byte aligned: %s", infoRemote.maxReceiveSize );

			boolean isOk = true;
			if( val != null ){
				ChabuConnectionAcceptInfo acceptInfo = val.isAccepted(infoLocal, infoRemote);
				if( acceptInfo != null && acceptInfo.code != 0 ){
					isOk = false;

					delayedAbort(acceptInfo.code, acceptInfo.message );
					
				}
			}
			val = null;

			if( isOk && this.xmitBuf.capacity() != infoRemote.maxReceiveSize ){
				this.xmitBuf = ByteBuffer.allocate( infoRemote.maxReceiveSize );
				this.xmitBuf.order( ByteOrder.BIG_ENDIAN );
				this.xmitBuf.limit( 0 );
			}

			prepareXmitAccept();
			xmitAccepted = XmitState.PREPARED;
		}
	}

	private void delayedAbort(ChabuErrorCode ec, String message) {
		delayedAbort(ec.getCode(), message);
	}
	private void delayedAbort(int code, String message) {
		Utils.ensure( xmitAbortPending == XmitState.IDLE, ChabuErrorCode.ASSERT, "Abort is already pending while generating Abort from Validator");
		
		xmitAbortCode    = code;
		xmitAbortMessage = message;
		xmitAbortPending = XmitState.PENDING;
		
		callXmitRequestListener();

	}

	private void callXmitRequestListener() {
		xmitRequestPending = true;
		if( xmitRequestListener != null ){
			xmitRequestListener.run();
		}
	}

	void channelXmitRequestData(int channelId){
		synchronized(this){
			int priority = channels.get(channelId).getPriority();
			Utils.ensure(priority < xmitChannelRequestData.length, ChabuErrorCode.ASSERT, "priority:%s < xmitChannelRequestData.length:%s", priority, xmitChannelRequestData.length );
			xmitChannelRequestData[priority].set( channelId );
		}
		callXmitRequestListener();
	}
	
	void channelXmitRequestArm(int channelId){
		synchronized(this){
			int priority = channels.get(channelId).getPriority();
			Utils.ensure(priority < xmitChannelRequestArm.length, ChabuErrorCode.ASSERT, "priority:%s < xmitChannelRequestData.length:%s", priority, xmitChannelRequestArm.length );
			xmitChannelRequestArm[priority].set( channelId );
		}
		callXmitRequestListener();
	}

//	@Override
	public int xmit(ByteChannel channel) {
		// TODO Add impl
		return 0;
	}

	@Override
	public void xmit(ByteBuffer buf) {
		
		// now we are here, so reset the request
		xmitRequestPending = false;
		
		// prepare trace
		PrintWriter trc = traceWriter;
		int trcStartPos = buf.position();

		// start real work
		while( buf.hasRemaining() ){
			
			Utils.transferRemaining( xmitBuf, buf );

			if( !buf.hasRemaining() ){
				// given xmit buffer is full
				// -> not able to send more
				break;
			}
			
			if( xmitBuf.hasRemaining() ){
				// xmitBuf not yet completely copied
				// -> 
				break;
			}

			if( xmitStartupCompleted != XmitState.IDLE ){
				if( xmitStartupCompleted == XmitState.PREPARED ){
					xmitStartupCompleted = XmitState.XMITTED;
					checkConnectingValidator();
					continue;
				}
			}
			if( recvSetupCompleted != RecvState.RECVED ){
				break;
			}
			if( xmitAbortPending == XmitState.PENDING ){
				
				int code = xmitAbortCode;
				String msg = xmitAbortMessage;
				xmitAbortCode = 0;
				xmitAbortMessage = "";
				xmitAbortPending = XmitState.XMITTED;
				throw new ChabuException( ChabuErrorCode.REMOTE_ABORT, code, String.format("Remote Abort: Code:%d %s", code, msg));
			}
			if( xmitAbortPending != XmitState.IDLE ){				
				if( xmitAbortPending == XmitState.PENDING ){
					processXmitAbort();
					continue;
				}
			}

			ChabuChannelImpl ch = calcNextXmitChannel(xmitChannelRequestArm);
			if( ch != null ){
				ch.handleXmitArm();
				continue;
			}
			
			ch = calcNextXmitChannel(xmitChannelRequestData);
			if( ch != null ){
				ch.handleXmitData();
				continue;
			}

			// nothing more to do, go out
			break;
		}
		// write out trace info
		if( trc != null ){
			trc.printf( "WIRE_TX: {}%n");
			Utils.printTraceHexData(trc, buf, trcStartPos, buf.position());
		}
	}

	/**
	 * Put on the buffer the needed org.chabu protocol informations: org.chabu version, byte order, payloadsize, channel count
	 * 
	 * These values must be set previous to infoLocal
	 * 
	 */
	private void processXmitSetup(){

		if( xmitBuf.hasRemaining() ){
			throw new ChabuException("Cannot xmit SETUP, buffer is not empty");
		}
		
		byte[] anlBytes = infoLocal.applicationName.getBytes( StandardCharsets.UTF_8 );
		Utils.ensure( anlBytes.length <= 200, ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME, "SETUP the local application name must be less than 200 UTF8 bytes, but is %s bytes.", anlBytes.length );
		
		xmitBuf.clear();
		xmitBuf.putInt  ( 0 );
		xmitBuf.putInt  ( PT_MAGIC | PacketType.SETUP.id );
	
		putXmitString( PROTOCOL_NAME );
		xmitBuf.putInt  ( PROTOCOL_VERSION );
		
		xmitBuf.putInt  ( infoLocal.maxReceiveSize       );
		
		xmitBuf.putInt  ( infoLocal.applicationVersion );
		putXmitString( infoLocal.applicationName );
		
		// set packet size
		xmitBuf.putInt  ( 0, xmitBuf.position() );
		xmitBuf.flip();
		
		xmitStartupCompleted = XmitState.PREPARED;
		
	}

	private void prepareXmitAccept(){

		if( xmitBuf.hasRemaining() ){
			throw new ChabuException("Cannot xmit ACCEPT, buffer is not empty");
		}
		
		xmitBuf.clear();
		xmitBuf.putInt( 8 );
		xmitBuf.putInt( PT_MAGIC | PacketType.ACCEPT.id );
		xmitBuf.flip();
	}

	private void processXmitAbort(){

		final int MIN_SZ = 16; // PS, PT, CODE, MSG_LEN

		byte[] msgBytes = xmitAbortMessage.getBytes( StandardCharsets.UTF_8 );

		Utils.ensure( msgBytes.length <= ABORT_MSGLEN_MAX, ChabuErrorCode.PROTOCOL_ABORT_MSG_LENGTH,
				"Xmit Abort message, text must be less than %s UTF8 bytes, but is %s",
				ABORT_MSGLEN_MAX, msgBytes.length );

		xmitBuf.clear();
		xmitBuf.position(4);
		xmitBuf.putInt( MIN_SZ + msgBytes.length );
		xmitBuf.putInt( PT_MAGIC | PacketType.ABORT.id );
		xmitBuf.putInt(xmitAbortCode );
		putXmitString(msgBytes);
		xmitBuf.putInt( 0, xmitBuf.position() );
		xmitBuf.flip();
		
		xmitAbortMessage = "";
		xmitAbortCode    = 0;
		xmitAbortPending = XmitState.PREPARED;
		
	}
	
	/** 
	 * Called by channel
	 */
	void processXmitArm( int channelId, int arm ){

		if( xmitBuf.hasRemaining() ){
			throw new ChabuException("Cannot xmit ACCEPT, buffer is not empty");
		}
		
		xmitBuf.clear();
		xmitBuf.putInt( 16 );
		xmitBuf.putInt( PT_MAGIC | PacketType.ARM.id );
		xmitBuf.putInt( channelId );
		xmitBuf.putInt( arm );
		xmitBuf.flip();
	}
	
	/** 
	 * Called by channel
	 */
	int processXmitSeq( int channelId, int seq, int maxPayload, ConsumerByteBuffer user ){
		
		if( xmitBuf.hasRemaining() ){
			throw new ChabuException("Cannot xmit SEQ, buffer is not empty");
		}
		
		final int HDR_SZ = 20;
		xmitBuf.clear();
		xmitBuf.limit( Math.min( xmitBuf.capacity(), Utils.alignUpTo4( HDR_SZ + maxPayload )));
		xmitBuf.position( HDR_SZ );
		
		user.accept( xmitBuf );
		
		int pls = xmitBuf.position() - HDR_SZ;
		
		// add padding
		while( (xmitBuf.position() & 3) != 0 ){
			xmitBuf.put((byte)0);
		}

		int ps = xmitBuf.position();
		
		if( pls > 0 ){
			xmitBuf.putInt( 0, ps );
			xmitBuf.putInt( 4, PT_MAGIC | PacketType.SEQ.id );
			xmitBuf.putInt( 8, channelId );
			xmitBuf.putInt( 12, seq );
			xmitBuf.putInt( 16, pls );
			xmitBuf.flip();
		}
		else {
			xmitBuf.clear().limit(0);
		}
		return pls;
	}


	private ChabuChannelImpl calcNextXmitChannel(BitSet[] prioBitSets) {
		synchronized(this){
			for( int prio = priorityCount-1; prio >= 0; prio-- ){
				int idxCandidate = -1;
				BitSet prioBitSet = prioBitSets[prio];

				// search from last channel pos on
				if( xmitLastChannelIdx+1 < prioBitSet.size() ){
					idxCandidate = prioBitSet.nextSetBit(xmitLastChannelIdx+1);
				}

				// try from idx zero
				if( idxCandidate < 0 ){
					idxCandidate = prioBitSet.nextSetBit(0);
				}

				// if found, clear and use it
				if( idxCandidate >= 0 ){
					prioBitSet.clear(idxCandidate);
					xmitLastChannelIdx = idxCandidate;
					ChabuChannelImpl channel = channels.get(idxCandidate);
					Utils.ensure( channel.getPriority() == prio, ChabuErrorCode.ASSERT, "Channels prio does not match the store prio." );
					return channel;
				}
			}
			return null;
		}
	}
	
	@Override
	public void addXmitRequestListener( Runnable r) {
		Utils.ensure( this.xmitRequestListener == null && r != null, ChabuErrorCode.ASSERT, "Listener passed in is null" );
		this.xmitRequestListener = r;
	}
	
	private String getRecvString(){
		
		int len = recvBuf.getInt();
		if( recvBuf.remaining() < len ){
			throw new ChabuException(String.format("Chabu string length exceeds packet length len:%d data-remaining:%d", len, recvBuf.remaining() ));
		}
			
		byte[] bytes = new byte[len];
		recvBuf.get( bytes );
		while( (len & 3) != 0 ){
			len++;
			recvBuf.get();
		}
		return new String( bytes, StandardCharsets.UTF_8 );
	}

	@Override
	public void setTracePrinter(PrintWriter writer) {
		this.traceWriter = writer;
	}

	public PrintWriter getTraceWriter() {
		return traceWriter;
	}

	public void setConnectingValidator(ChabuConnectingValidator val) {
		Utils.ensure( val      != null, ChabuErrorCode.CONFIGURATION_VALIDATOR, "ConnectingValidator passed in is null" );
		Utils.ensure( this.val == null, ChabuErrorCode.CONFIGURATION_VALIDATOR, "ConnectingValidator is already set" );
		this.val = val;
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
	public boolean isXmitRequestPending() {
		return xmitRequestPending;
	}
	private void putXmitString(String str) {
		putXmitString(str.getBytes(StandardCharsets.UTF_8));
	}
	private void putXmitString(byte[] bytes) {
		xmitBuf.putInt  ( bytes.length );
		xmitBuf.put     ( bytes );
		int len = bytes.length;
		while( (len&3) != 0 ){
			len++;
			xmitBuf.put((byte)0);
		}
	}

	@Override
	public String toString() {
		return String.format("Chabu[ recv:%s xmit:%s ]", recvBuf, xmitBuf );
	}
}