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
package org.chabu.prot.v1.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.chabu.prot.v1.ChabuErrorCode;
import org.chabu.prot.v1.ChabuException;
import org.chabu.prot.v1.ChabuSetupInfo;

public abstract class ChabuXmitter {

	private static final int SETUP_PROTNAME_MAXLENGTH = 8;
	private static final int SETUP_APPNAME_MAXLENGTH = 56;
	private static final int ABORT_MSG_MAXLENGTH = 56;
	protected static final int   PT_MAGIC = 0x77770000;
	
	protected enum LoopCtrl {
		Break, Continue, None;
	}

	@FunctionalInterface
	protected interface LoopCtrlAction {
	    LoopCtrl run() throws IOException;
	}
	

	protected abstract ArrayList<LoopCtrlAction> getActions();
	private final Runnable xmitRequestListener;
	private final AbortMessage abortMessage;

	protected ByteBuffer xmitBuf = ByteBuffer.allocate( Constants.MAX_RECV_LIMIT_LOW );
	protected PacketType packetType = PacketType.NONE;
	protected ByteChannel loopByteChannel;
	protected XmitState xmitAbort = XmitState.IDLE;

	public ChabuXmitter(AbortMessage abortMessage, Runnable xmitRequestListener){
		this.abortMessage = abortMessage;
		this.xmitRequestListener = xmitRequestListener;
	}
	
	protected void runActionsUntilBreak() throws IOException {
		ArrayList<LoopCtrlAction> loopActions = getActions();
		Lwhile: while( true ) {
			for( LoopCtrlAction action : loopActions ) {
				LoopCtrl loopCtrl = action.run();
				if( loopCtrl == LoopCtrl.Break    ) break    Lwhile;
				if( loopCtrl == LoopCtrl.Continue ) continue Lwhile;
			}
		}
	}

	protected void processXmitNop(){
		checkXmitBufEmptyOrThrow("Cannot xmit NOP, buffer is not empty");
		xmitFillStart( PacketType.NOP );
		xmitFillComplete();
	}

	public void xmit(ByteChannel byteChannel) throws IOException {
		this.loopByteChannel = byteChannel;
		try{
			runActionsUntilBreak();
		}
		finally {
			this.loopByteChannel = null;
		}
	}
	
	protected LoopCtrl xmitAction_EvalAbort() throws IOException {
		if( abortMessage.isPending() ){
			prepareAbort();
			return LoopCtrl.Continue;
		}
		return LoopCtrl.None;
	}

	protected LoopCtrl xmitAction_RemainingXmitBuf() throws IOException {
		
		if( xmitBuf.hasRemaining() ){
			loopByteChannel.write(xmitBuf);
		}
		
		if( !xmitBuf.hasRemaining() && packetType != PacketType.SEQ ){
			handleNonSeqCompletion();
			packetType = PacketType.NONE;
		}
		if( xmitBuf.hasRemaining() ){
			callXmitRequestListener();
		}
		
		return xmitBuf.hasRemaining() ? LoopCtrl.Break : LoopCtrl.None;
	}



	protected abstract void handleNonSeqCompletion() ;

	protected void xmitFillSetupPacket(ChabuSetupInfo setupInfo) {
		xmitFillStart( PacketType.SETUP );
		xmitFillAddString( SETUP_PROTNAME_MAXLENGTH, Constants.PROTOCOL_NAME );
		xmitFillAddInt( Constants.PROTOCOL_VERSION );
		xmitFillAddInt( setupInfo.recvPacketSize );
		xmitFillAddInt( setupInfo.applicationVersion );
		xmitFillAddString( SETUP_APPNAME_MAXLENGTH, setupInfo.applicationProtocolName );
		xmitFillComplete();
	}

	protected void prepareXmitAccept(){
		checkXmitBufEmptyOrThrow("Cannot xmit ACCEPT, buffer is not empty");
		xmitFillStart( PacketType.ACCEPT );
		xmitFillComplete();
	}

	protected void xmitFillAddInt(int value) {
		xmitBuf.putInt( value );
	}
	
	protected void xmitFillAddString( int maxLength, String str) {
		byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
		if(bytes.length > maxLength){
			byte[] bytes2 = new byte[maxLength];
			System.arraycopy(bytes, 0, bytes2, 0, maxLength);
			bytes = bytes2;
		}
		xmitFillAddString(bytes);
	}
	
	protected void xmitFillAddString(byte[] bytes) {
		xmitBuf.putInt  ( bytes.length );
		xmitBuf.put     ( bytes );
		xmitFillAligned();
	}
	void prepareAbort(){
		checkXmitBufEmptyOrThrow("Cannot xmit ABORT, buffer is not empty");
		xmitFillStart( PacketType.ABORT );
		xmitFillAddInt( abortMessage.getCode() );
		xmitFillAddString( ABORT_MSG_MAXLENGTH, abortMessage.getMessage() );
		xmitFillComplete();
		xmitAbort = XmitState.PREPARED;
	}
	protected void xmitFillArmPacket(int channelId, int arm) {
		checkXmitBufEmptyOrThrow("Cannot xmit ACCEPT, buffer is not empty");
		xmitFillStart( PacketType.ARM );
		xmitBuf.putInt( channelId );
		xmitBuf.putInt( arm );
		xmitFillComplete();
	}
	
	protected void xmitFillStart( PacketType type ){
		packetType = type;
		xmitBuf.clear();
		xmitBuf.putInt( -1 );
		xmitBuf.putInt( PT_MAGIC | type.id );
	}

	protected void xmitFillComplete(){
		xmitFillAligned();
		xmitBuf.putInt( 0, xmitBuf.position() );
		xmitBuf.flip();
	}
	protected void xmitFillComplete( int packetSize ){
		xmitFillAligned();
		xmitBuf.putInt( 0, packetSize );
		xmitBuf.flip();
	}
	
	private void xmitFillAligned() {
		while( xmitBuf.position() % 4 != 0 ){
			xmitBuf.put( (byte)0 );
		}
	}
	protected void checkXmitBufEmptyOrThrow(String message) {
		if( xmitBuf.hasRemaining() ){
			throw new ChabuException(message);
		}
	}

	public void delayedAbort(int code, String message, Object ... args) {
		Utils.ensure( xmitAbort == XmitState.IDLE, 
				ChabuErrorCode.ASSERT, 
				"Abort is already pending while generating Abort from Validator");
		
		abortMessage.setPending( code, String.format(message, args) );
		xmitAbort = XmitState.PENDING;
		
		callXmitRequestListener();

	}

	protected void throwAbort() {
		int code = abortMessage.getCode();
		String msg = abortMessage.getMessage();
		
		abortMessage.setXmitted();
		
		throw new ChabuException( ChabuErrorCode.REMOTE_ABORT, code, 
				String.format("Remote Abort: Code:0x%08X (%d) %s", code, code, msg));
	}



	public void delayedAbort(ChabuErrorCode ec, String message, Object ... args) {
		delayedAbort(ec.getCode(), message, args);
	}

	void callXmitRequestListener() {
		if( xmitRequestListener != null ){
			xmitRequestListener.run();
		}
	}

	void channelXmitRequestArm(int channelId) {
		throw new RuntimeException("Xmit request for ARM before activation, channel: " + channelId );
	}

	void channelXmitRequestData(int channelId) {
		throw new RuntimeException("Xmit request for data before activation, channel: " + channelId );
	}

		


}
