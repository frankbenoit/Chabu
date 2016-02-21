package org.chabu.internal;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.function.BiFunction;

import org.chabu.ChabuErrorCode;
import org.chabu.ChabuException;
import org.chabu.ChabuSetupInfo;

public class ChabuXmitter {
	
	private static final int   PT_MAGIC = 0x77770000;

	private static final int   ABORT_MSGLEN_MAX = 48;
	/**
	 * The startup data is completely sent.
	 */
	private XmitState xmitStartupCompleted = XmitState.IDLE;
	
	private Priorizer xmitChannelRequestData;
	private Priorizer xmitChannelRequestArm;
	
	private ByteBuffer xmitBuf = ByteBuffer.allocate( 0x100 );

	private Runnable xmitRequestListener;
	private boolean  xmitRequestPending = false;
	private ArrayList<ChabuChannelImpl> channels;
	
	/**
	 * Have sent the ACCEPT packet
	 */
	private XmitState xmitAccepted = XmitState.IDLE;
	
	private XmitState    xmitAbortPending = XmitState.IDLE;
	private int          xmitAbortCode    = 0;
	private String       xmitAbortMessage = "";

	private PrintWriter traceWriter;

	private Setup setup;

	public ChabuXmitter(){
		
		xmitBuf.order(ByteOrder.BIG_ENDIAN);
		xmitBuf.clear().limit(0);
		

	}

	void activate(int priorityCount, ArrayList<ChabuChannelImpl> channels, Setup setup, BiFunction< Integer, Integer, Priorizer> priorizerFactory ) {
		this.channels = channels;
		this.setup = setup;
		xmitChannelRequestData = priorizerFactory.apply(priorityCount, channels.size());
		xmitChannelRequestArm  = priorizerFactory.apply(priorityCount, channels.size());
	}
	
	void ensureXmitBufMatchesReceiveSize( int remoteMaxReceiveSize ) {
		if( this.xmitBuf.capacity() != remoteMaxReceiveSize ){
			this.xmitBuf = ByteBuffer.allocate( remoteMaxReceiveSize );
			this.xmitBuf.order( ByteOrder.BIG_ENDIAN );
			this.xmitBuf.limit( 0 );
		}
	}

	void callXmitRequestListener() {
		xmitRequestPending = true;
		if( xmitRequestListener != null ){
			xmitRequestListener.run();
		}
	}

	void channelXmitRequestData(int channelId){
		synchronized(this){
			int priority = channels.get(channelId).getPriority();
			xmitChannelRequestData.reqest( priority, channelId );
			
		}
		callXmitRequestListener();
	}
	
	void channelXmitRequestArm(int channelId){
		synchronized(this){
			int priority = channels.get(channelId).getPriority();
			xmitChannelRequestArm.reqest( priority, channelId );
		}
		callXmitRequestListener();
	}

	public void xmit(ByteBuffer buf) {
		// now we are here, so reset the request
		xmitRequestPending = false;
		
		// prepare trace
		PrintWriter trc = traceWriter;
		int trcStartPos = buf.position();

		xmitProcessing(buf);
		
		// write out trace info
		if( trc != null ){
			trc.printf( "WIRE_TX: {}%n");
			Utils.printTraceHexData(trc, buf, trcStartPos, buf.position());
		}
	}
	private void xmitProcessing(ByteBuffer buf) {
		// start real work
		while( buf.hasRemaining() ){
			
			ByteBufferUtils.transferRemaining( xmitBuf, buf );

			if( !canPrepareMoreXmitData(buf)){
				break;
			}

			boolean dataAvail = tryFillXmitBuf();
			if( !dataAvail ){
				break;
			}

		}
	}

	private boolean canPrepareMoreXmitData(ByteBuffer buf) {
		if( !buf.hasRemaining() ){
			// given xmit buffer is full
			// -> not able to send more
			return false;
		}
		
		if( xmitBuf.hasRemaining() ){
			// xmitBuf not yet completely copied
			// -> 
			return false;
		}
		return true;
	}

	private boolean tryFillXmitBuf() {
		
		if( xmitStartupCompleted != XmitState.IDLE ){
			if( xmitStartupCompleted == XmitState.PREPARED ){
				xmitStartupCompleted = XmitState.XMITTED;
				setup.checkConnectingValidator(this);
				if( xmitBuf.hasRemaining() ){
					return true;
				}
			}
		}
		
		if( !setup.isRemoteSetupReceived() ){
			return false;
		}
		
		if( xmitAbortPending == XmitState.PENDING ){
			
			int code = xmitAbortCode;
			String msg = xmitAbortMessage;
			xmitAbortCode = 0;
			xmitAbortMessage = "";
			xmitAbortPending = XmitState.XMITTED;
			throw new ChabuException( ChabuErrorCode.REMOTE_ABORT, code, 
					String.format("Remote Abort: Code:%d %s", code, msg));
		}

		if( xmitAbortPending != XmitState.IDLE ){				
			if( xmitAbortPending == XmitState.PENDING ){
				processXmitAbort();
				return true;
			}
		}

		while( true ){
			ChabuChannelImpl ch = calcNextXmitChannel(xmitChannelRequestArm);
			if( ch == null ){
				break;
			}
			ch.handleXmitArm();
			return true;
		}
		
		while( true ){
			ChabuChannelImpl ch = calcNextXmitChannel(xmitChannelRequestData);
			if( ch == null ){
				break;
			}
			ch.handleXmitData();
			return true;
		}

		return false;
	}


	private void xmitFillAbortPacket(byte[] msgBytes) {
		xmitFillStart( PacketType.ABORT );
		xmitBuf.putInt(xmitAbortCode );
		xmitFillAddString(msgBytes);
		xmitFillComplete();
	}
	
	private void setAbortSendingPrepared() {
		xmitAbortMessage = "";
		xmitAbortCode    = 0;
		xmitAbortPending = XmitState.PREPARED;
	}

	/** 
	 * Called by channel
	 */
	void processXmitArm( int channelId, int arm ){
		checkXmitBufEmptyOrThrow("Cannot xmit ACCEPT, buffer is not empty");
		xmitFillArmPacket(channelId, arm);
	}

	private void xmitFillArmPacket(int channelId, int arm) {
		xmitFillStart( PacketType.ARM );
		xmitBuf.putInt( channelId );
		xmitBuf.putInt( arm );
		xmitFillComplete();
	}
	
	private void xmitFillStart( PacketType type ){
		xmitBuf.clear();
		xmitBuf.putInt( -1 );
		xmitBuf.putInt( PT_MAGIC | type.id );
	}

	private void xmitFillComplete(){
		xmitFillAligned();
		xmitBuf.putInt( 0, xmitBuf.position() );
		xmitBuf.flip();
	}
	
	private void xmitFillAligned() {
		while( xmitBuf.position() % 4 != 0 ){
			xmitBuf.put( (byte)0 );
		}
	}

	/** 
	 * Called by channel
	 */
	int processXmitSeq( int channelId, int seq, int maxPayload, ConsumerByteBuffer user ){
		
		checkXmitBufEmptyOrThrow("Cannot xmit SEQ, buffer is not empty");
		
		xmitFillStart( PacketType.SEQ );
		xmitFillAddInt( channelId );
		xmitFillAddInt( seq );
		xmitFillAddInt( 0 ); // Data available 
		int idxPls = xmitBuf.position();
		xmitFillAddInt( 0 ); // PLS
		int idxPld = xmitBuf.position();
		xmitBuf.limit( Math.min( xmitBuf.capacity(), Utils.alignUpTo4( idxPld + maxPayload )));
		
		user.accept( xmitBuf );

		int pls = xmitBuf.position() - idxPld;
		
		if( pls > 0 ){
			xmitBuf.putInt( idxPls, pls );
			xmitFillComplete();
		}
		else {
			xmitBuf.clear().limit(0);
		}
		return pls;
	}


	private ChabuChannelImpl calcNextXmitChannel(Priorizer priorizer) {
		synchronized(this){
			int reqChannel = priorizer.popNextRequest();
			if( reqChannel < 0 ) return null;
			return channels.get( reqChannel );
		}
	}

	
	public void addXmitRequestListener( Runnable r) {
		Utils.ensure( this.xmitRequestListener == null && r != null, ChabuErrorCode.ASSERT, 
				"Listener passed in is null" );
		this.xmitRequestListener = r;
	}
	/**
	 * Put on the buffer the needed org.chabu protocol informations: org.chabu version, 
	 * byte order, payloadsize, channel count
	 * 
	 * These values must be set previous to infoLocal
	 * 
	 */
	void processXmitSetup(){
		checkXmitBufEmptyOrThrow("Cannot xmit SETUP, buffer is not empty");
		checkLocalAppNameLength();
		xmitFillSetupPacket();
		xmitStartupCompleted = XmitState.PREPARED;
	}

	private void xmitFillSetupPacket() {
		
		ChabuSetupInfo infoLocal = setup.getInfoLocal();
		
		xmitFillStart( PacketType.SETUP );
		xmitFillAddString( ChabuImpl.PROTOCOL_NAME );
		xmitFillAddInt( ChabuImpl.PROTOCOL_VERSION );
		xmitFillAddInt( infoLocal.maxReceiveSize       );
		xmitFillAddInt( infoLocal.applicationVersion );
		xmitFillAddString( infoLocal.applicationName );
		xmitFillComplete();
	}
	private void checkXmitBufEmptyOrThrow(String message) {
		if( xmitBuf.hasRemaining() ){
			throw new ChabuException(message);
		}
	}

	void prepareXmitAccept(){
		checkXmitBufEmptyOrThrow("Cannot xmit ACCEPT, buffer is not empty");
		xmitFillStart( PacketType.ACCEPT );
		xmitFillComplete();
		xmitAccepted = XmitState.PREPARED;
	}

	private void processXmitAbort(){
		byte[] msgBytes = getAbortMessageBytesAndCheckLength();
		xmitFillAbortPacket(msgBytes);
		setAbortSendingPrepared();
	}

	private byte[] getAbortMessageBytesAndCheckLength() {
		byte[] msgBytes = xmitAbortMessage.getBytes( StandardCharsets.UTF_8 );

		Utils.ensure( msgBytes.length <= ABORT_MSGLEN_MAX, ChabuErrorCode.PROTOCOL_ABORT_MSG_LENGTH,
				"Xmit Abort message, text must be less than %s UTF8 bytes, but is %s",
				ABORT_MSGLEN_MAX, msgBytes.length );
		return msgBytes;
	}
	private void xmitFillAddInt(int value) {
		xmitBuf.putInt( value );
	}
	private void xmitFillAddString(String str) {
		xmitFillAddString(str.getBytes(StandardCharsets.UTF_8));
	}
	private void xmitFillAddString(byte[] bytes) {
		xmitBuf.putInt  ( bytes.length );
		xmitBuf.put     ( bytes );
		xmitFillAligned();
	}

	public boolean isXmitRequestPending() {
		return xmitRequestPending;
	}
	
	void delayedAbort(int code, String message) {
		Utils.ensure( xmitAbortPending == XmitState.IDLE, 
				ChabuErrorCode.ASSERT, 
				"Abort is already pending while generating Abort from Validator");
		
		xmitAbortCode    = code;
		xmitAbortMessage = message;
		xmitAbortPending = XmitState.PENDING;
		
		callXmitRequestListener();

	}

	void delayedAbort(ChabuErrorCode ec, String message) {
		delayedAbort(ec.getCode(), message);
	}
	private void checkLocalAppNameLength() {
		byte[] anlBytes = setup.getInfoLocal().applicationName.getBytes( StandardCharsets.UTF_8 );
		Utils.ensure( anlBytes.length <= 200, 
				ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME, 
				"SETUP the local application name must be less than 200 UTF8 bytes, but is %s bytes.",
				anlBytes.length );
	}

	public void setTracePrinter(PrintWriter writer) {
		this.traceWriter = writer;
	}

	@Override
	public String toString() {
		return xmitBuf.toString();
	}

	public boolean isAcceptedXmitted() {
		return xmitAccepted == XmitState.XMITTED;
	}

	public boolean isStartupXmitted() {
		return xmitStartupCompleted == XmitState.XMITTED;
	}
}