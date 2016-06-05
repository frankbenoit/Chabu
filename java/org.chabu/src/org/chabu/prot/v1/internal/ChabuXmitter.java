package org.chabu.prot.v1.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.function.BiFunction;

import org.chabu.prot.v1.ChabuErrorCode;
import org.chabu.prot.v1.ChabuException;
import org.chabu.prot.v1.ChabuSetupInfo;

public class ChabuXmitter implements Aborter, ConnectionAccepter {
	
	private enum LoopCtrl {
		Break, Continue, None;
	}

	@FunctionalInterface
	private interface LoopCtrlAction {
	    LoopCtrl run() throws IOException;
	}
	
	private static final int   PT_MAGIC = 0x77770000;

	private static final int SETUP_PROTNAME_MAXLENGTH = 8;
	private static final int SETUP_APPNAME_MAXLENGTH = 56;
	private static final int ABORT_MSG_MAXLENGTH = 56;

	/**
	 * The startup data is completely sent.
	 */
	private XmitState xmitStartupCompleted = XmitState.PENDING;
	private XmitState xmitNop   = XmitState.IDLE;
	private XmitState xmitAbort = XmitState.IDLE;
	
	private Priorizer xmitChannelRequestData;
	private Priorizer xmitChannelRequestCtrl;
	
	private ByteBuffer xmitBuf = ByteBuffer.allocate( Constants.MAX_RECV_LIMIT_LOW );
	private PacketType packetType = PacketType.NONE;

	private final LinkedList<Runnable> xmitRequestListeners = new LinkedList<>();
	
	private ArrayList<ChabuChannelImpl> channels;
	
	/**
	 * Have sent the ACCEPT packet
	 */
	private volatile XmitState xmitAccepted = XmitState.IDLE;
	private AbortMessage abortMessage = new AbortMessage();

	private Setup setup;
	private int maxXmitSize = Constants.MAX_RECV_LIMIT_LOW; 
	
	@SuppressWarnings("unused")
	private boolean activated;
	private ByteBuffer       seqPadding = ByteBuffer.allocate(3);
	private ByteBuffer       seqPacketPayload;
	private ChabuChannelImpl seqChannel;
	
	private ByteChannel loopByteChannel;

	private final ArrayList<LoopCtrlAction> actionsSetupRun = new ArrayList<>();
	private final ArrayList<LoopCtrlAction> actionsNormalRun = new ArrayList<>();
	private ArrayList<LoopCtrlAction> actions = actionsSetupRun;

	public ChabuXmitter(){
		
		xmitBuf.order(ByteOrder.BIG_ENDIAN);
		xmitBuf.clear().limit(0);

		{
			actionsSetupRun.add( this::xmitAction_RemainingXmitBuf   );
			actionsSetupRun.add( this::xmitAction_RemainingSeq       );
			actionsSetupRun.add( this::xmitAction_EvalStartup        );
			actionsSetupRun.add( this::xmitAction_EvalAbort          );
			actionsSetupRun.add( this::xmitAction_EvalAccept         );
			actionsSetupRun.add( this::xmitAction_End                );
		}
		{
			actionsNormalRun.add( this::xmitAction_RemainingXmitBuf   );
			actionsNormalRun.add( this::xmitAction_RemainingSeq       );
			actionsNormalRun.add( this::xmitAction_EvalAbort          );
			actionsNormalRun.add( this::xmitAction_EvalChannelCtrl    );
			actionsNormalRun.add( this::xmitAction_EvalChannelData    );
			actionsNormalRun.add( this::xmitAction_EvalNop            );
			actionsNormalRun.add( this::xmitAction_End                );
		}
	}
	
	void activate(int priorityCount, ArrayList<ChabuChannelImpl> channels, Setup setup, 
			BiFunction< Integer, Integer, Priorizer> priorizerFactory ) {
		
		Utils.ensure( priorityCount >= 1 && priorityCount <= 20, 
				ChabuErrorCode.CONFIGURATION_PRIOCOUNT, 
				"Priority count must be in range 1..20, but is %s", priorityCount );
		this.channels = channels;
		this.setup = setup;
		xmitChannelRequestData = priorizerFactory.apply(priorityCount, channels.size());
		xmitChannelRequestCtrl = priorizerFactory.apply(priorityCount, channels.size());
		this.activated = true;
		
		for( ChabuChannelImpl ch : channels ){
			Utils.ensure( ch.getPriority() < priorityCount, ChabuErrorCode.CONFIGURATION_CH_PRIO, 
					"Channel %s has higher priority (%s) as the max %s", 
					ch.getChannelId(), ch.getPriority(), priorityCount );
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
			xmitChannelRequestCtrl.reqest( priority, channelId );
		}
		callXmitRequestListener();
	}

	void callXmitRequestListener() {
		LinkedList<Runnable> listeners = xmitRequestListeners;
		for( Runnable r : listeners ){
			r.run();
		}
	}
		
	LoopCtrl xmitAction_RemainingXmitBuf() throws IOException {
		
		if( xmitBuf.hasRemaining() ){
			loopByteChannel.write(xmitBuf);
		}
		
		if( !xmitBuf.hasRemaining() && packetType != PacketType.SEQ ){
			handleNonSeqCompletion();
		}
		
		return xmitBuf.hasRemaining() ? LoopCtrl.Break : LoopCtrl.None;
	}

	private void handleNonSeqCompletion() {
		switch( packetType ){
		case SETUP : 
			xmitStartupCompleted = XmitState.XMITTED; 
			break;
			
		case ACCEPT: 
			xmitAccepted = XmitState.XMITTED;
			break;
			
		case NOP: 
			xmitNop = XmitState.XMITTED; 
			break;
			
		case ABORT:
			xmitAbort = XmitState.XMITTED;
			throwAbort();
			break;
			
		default: break;
		}
		packetType = PacketType.NONE;
	}
	
	LoopCtrl xmitAction_RemainingSeq() throws IOException {
		boolean isCompleted = true;
		if( packetType == PacketType.SEQ ){
			
			if( seqPacketPayload.hasRemaining() ){
				loopByteChannel.write(seqPacketPayload);
			}
			if( !seqPacketPayload.hasRemaining() && seqPadding.hasRemaining() ){
				loopByteChannel.write(seqPadding);
			}
			
			isCompleted = !seqPacketPayload.hasRemaining() && !seqPadding.hasRemaining();
			
			if( isCompleted ){
				handleSeqCompletion();
			}
		}
		return isCompleted ? LoopCtrl.None : LoopCtrl.Break;

	}

	private void handleSeqCompletion() {
		seqChannel.seqPacketCompleted();
		if( seqChannel.getXmitRemaining() > 0 && seqChannel.getXmitRemainingByRemote() > 0 ){
			xmitChannelRequestData.reqest( seqChannel.getPriority(), seqChannel.getChannelId());
		}

		seqPacketPayload = null;
		seqChannel       = null;
		packetType       = PacketType.NONE;
	}
	
	LoopCtrl xmitAction_EvalAbort() throws IOException {
		if( xmitAbort == XmitState.PENDING ){
			prepareAbort();
			return LoopCtrl.Continue;
		}
		return LoopCtrl.None;
	}
	
	LoopCtrl xmitAction_EvalStartup() throws IOException {
		if( xmitStartupCompleted == XmitState.PENDING ){
			processXmitSetup();
			return LoopCtrl.Continue;
		}
		return LoopCtrl.None;
	}
	
	LoopCtrl xmitAction_EvalAccept() throws IOException {
		if( xmitAccepted == XmitState.IDLE ){
			return LoopCtrl.Break;
		}
		else if( xmitAccepted == XmitState.PENDING ){
			prepareXmitAccept();
			return LoopCtrl.Continue;
		}	
		else if( xmitAccepted == XmitState.XMITTED ){
			actions = actionsNormalRun;
			return LoopCtrl.Continue;
		}
		else {
			Utils.fail(ChabuErrorCode.ASSERT, "shall not be here");
			return LoopCtrl.None;
		}

	}
	
	LoopCtrl xmitAction_EvalChannelCtrl() throws IOException {
		ChabuChannelImpl ch = popNextPriorizedChannelRequest(xmitChannelRequestCtrl);
		if( ch != null ){
			ch.handleXmitCtrl( this, xmitBuf );
		}
		return xmitBuf.hasRemaining() ? LoopCtrl.Continue : LoopCtrl.None;
	}

	LoopCtrl xmitAction_EvalChannelData() throws IOException {
		ChabuChannelImpl ch = popNextPriorizedChannelRequest(xmitChannelRequestData);
		if( ch != null ){
			seqPacketPayload = ch.handleXmitData( this, xmitBuf, maxXmitSize - ChabuImpl.SEQ_MIN_SZ );
			if( seqPacketPayload == null ){
				return LoopCtrl.None;
			}
			seqChannel = ch;
			int paddingCount = (4 - (seqPacketPayload.remaining() & 3)) % 4;
			seqPadding.clear().limit( paddingCount );
			
			Utils.ensure( xmitBuf.getInt(0) == xmitBuf.remaining() + seqPacketPayload.remaining() + seqPadding.remaining(),
					ChabuErrorCode.ASSERT, "%d = %d + %d + %d", xmitBuf.getInt(0), xmitBuf.remaining(), seqPacketPayload.remaining(), seqPadding.remaining());
		}
		return xmitBuf.hasRemaining() ? LoopCtrl.Continue : LoopCtrl.None;
	}
	
	LoopCtrl xmitAction_EvalNop() throws IOException {
		if( xmitNop == XmitState.PENDING ){
			processXmitNop();
			xmitNop = XmitState.PREPARED;
		}
		return LoopCtrl.None;
	}
	LoopCtrl xmitAction_End() throws IOException {
		return LoopCtrl.Break;
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

	private void runActionsUntilBreak() throws IOException {
		Lwhile: while( true ) {
			ArrayList<LoopCtrlAction> loopActions = actions;
			for( LoopCtrlAction action : loopActions ) {
				LoopCtrl loopCtrl = action.run();
				if( loopCtrl == LoopCtrl.Break    ) break    Lwhile;
				if( loopCtrl == LoopCtrl.Continue ) continue Lwhile;
			}
		}
	}


	private void throwAbort() {
		int code = abortMessage.getCode();
		String msg = abortMessage.getMessage();
		
		abortMessage.setXmitted();
		
		throw new ChabuException( ChabuErrorCode.REMOTE_ABORT, code, 
				String.format("Remote Abort: Code:0x%08X (%d) %s", code, code, msg));
	}


	private ChabuChannelImpl popNextPriorizedChannelRequest(Priorizer priorizer) {
		synchronized(this){
			int reqChannel = priorizer.popNextRequest();
			if( reqChannel < 0 ) return null;
			return channels.get( reqChannel );
		}
	}

	void prepareAbort(){
		checkXmitBufEmptyOrThrow("Cannot xmit ABORT, buffer is not empty");
		xmitFillStart( PacketType.ABORT );
		xmitFillAddInt( abortMessage.getCode() );
		xmitFillAddString( ABORT_MSG_MAXLENGTH, abortMessage.getMessage() );
		xmitFillComplete();
		xmitAbort = XmitState.PREPARED;
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
		packetType = type;
		xmitBuf.clear();
		xmitBuf.putInt( -1 );
		xmitBuf.putInt( PT_MAGIC | type.id );
	}

	private void xmitFillComplete(){
		xmitFillAligned();
		xmitBuf.putInt( 0, xmitBuf.position() );
		xmitBuf.flip();
	}
	private void xmitFillComplete( int packetSize ){
		xmitFillAligned();
		xmitBuf.putInt( 0, packetSize );
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
	void processXmitSeq( int channelId, int seq, int payload ){
		
		checkXmitBufEmptyOrThrow("Cannot xmit SEQ, buffer is not empty");
		
		xmitFillStart( PacketType.SEQ );
		xmitFillAddInt( channelId );
		xmitFillAddInt( seq );
		xmitFillAddInt( payload ); // PLS
		int packetSize = ChabuImpl.SEQ_MIN_SZ + payload;
		int packetSizeAligned = Utils.alignUpTo4(packetSize);
		xmitFillComplete( packetSizeAligned );
	}

	public void addXmitRequestListener( Runnable r) {
		Utils.ensure( r != null, ChabuErrorCode.ASSERT, "Listener passed in is null" );
		//Utils.ensure( !this.activated, ChabuErrorCode.ASSERT, "Chabu already activated" );
		this.xmitRequestListeners.add(r);
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

	void processXmitNop(){
		checkXmitBufEmptyOrThrow("Cannot xmit NOP, buffer is not empty");
		xmitFillStart( PacketType.NOP );
		xmitFillComplete();
	}
	
	private void xmitFillSetupPacket() {
		
		ChabuSetupInfo infoLocal = setup.getInfoLocal();
		
		xmitFillStart( PacketType.SETUP );
		xmitFillAddString( SETUP_PROTNAME_MAXLENGTH, Constants.PROTOCOL_NAME );
		xmitFillAddInt( Constants.PROTOCOL_VERSION );
		xmitFillAddInt( infoLocal.recvPacketSize       );
		xmitFillAddInt( infoLocal.applicationVersion );
		xmitFillAddString( SETUP_APPNAME_MAXLENGTH, infoLocal.applicationProtocolName );
		xmitFillComplete();
	}
	private void checkXmitBufEmptyOrThrow(String message) {
		if( xmitBuf.hasRemaining() ){
			throw new ChabuException(message);
		}
	}

	public void acceptConnection(int maxXmitSize){
		this.maxXmitSize = maxXmitSize;
		xmitAccepted = XmitState.PENDING;		
	}
	
	private void prepareXmitAccept(){
		checkXmitBufEmptyOrThrow("Cannot xmit ACCEPT, buffer is not empty");
		xmitFillStart( PacketType.ACCEPT );
		xmitFillComplete();
		xmitAccepted = XmitState.PREPARED;
	}

	private void xmitFillAddInt(int value) {
		xmitBuf.putInt( value );
	}
	
	private void xmitFillAddString( int maxLength, String str) {
		byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
		if(bytes.length > maxLength){
			byte[] bytes2 = new byte[maxLength];
			System.arraycopy(bytes, 0, bytes2, 0, maxLength);
			bytes = bytes2;
		}
		xmitFillAddString(bytes);
	}
	
	private void xmitFillAddString(byte[] bytes) {
		xmitBuf.putInt  ( bytes.length );
		xmitBuf.put     ( bytes );
		xmitFillAligned();
	}

	public void delayedAbort(int code, String message, Object ... args) {
		Utils.ensure( xmitAbort == XmitState.IDLE, 
				ChabuErrorCode.ASSERT, 
				"Abort is already pending while generating Abort from Validator");
		
		abortMessage.setPending( code, String.format(message, args) );
		xmitAbort = XmitState.PENDING;
		
		callXmitRequestListener();

	}

	public void delayedAbort(ChabuErrorCode ec, String message, Object ... args) {
		delayedAbort(ec.getCode(), message, args);
	}
	
	private void checkLocalAppNameLength() {
		byte[] anlBytes = setup.getInfoLocal().applicationProtocolName.getBytes( StandardCharsets.UTF_8 );
		Utils.ensure( anlBytes.length <= Constants.APV_MAX_LENGTH, 
				ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME_TOO_LONG, 
				"SETUP the local application name must be less than 200 UTF8 bytes, but is %s bytes.",
				anlBytes.length );
	}

	@Override
	public String toString() {
		return xmitBuf.toString();
	}

	public void recvArmShallBeXmitted( ChabuChannelImpl channel ) {
		xmitChannelRequestCtrl.reqest( channel.getPriority(), channel.getChannelId() );
	}
}