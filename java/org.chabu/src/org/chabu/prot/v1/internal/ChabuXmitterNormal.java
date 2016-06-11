package org.chabu.prot.v1.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.function.BiFunction;

import org.chabu.prot.v1.ChabuErrorCode;

public class ChabuXmitterNormal extends ChabuXmitter {
	
	/**
	 * The startup data is completely sent.
	 */
	private XmitState xmitNop   = XmitState.IDLE;
	
	private Priorizer xmitChannelRequestData;
	private Priorizer xmitChannelRequestCtrl;
	
	private ArrayList<ChabuChannelImpl> channels;
	

	private int remoteRecvPacketSize = Constants.MAX_RECV_LIMIT_LOW; 
	
	private ByteBuffer       seqPadding = ByteBuffer.allocate(3);
	private ByteBuffer       seqPacketPayload;
	private ChabuChannelImpl seqChannel;
	
	private final ArrayList<LoopCtrlAction> actionsNormalRun = new ArrayList<>();
	{
		actionsNormalRun.add( this::xmitAction_RemainingXmitBuf   );
		actionsNormalRun.add( this::xmitAction_RemainingSeq       );
		actionsNormalRun.add( this::xmitAction_EvalAbort          );
		actionsNormalRun.add( this::xmitAction_EvalChannelCtrl    );
		actionsNormalRun.add( this::xmitAction_EvalChannelData    );
		actionsNormalRun.add( this::xmitAction_EvalNop            );
		actionsNormalRun.add( this::xmitAction_End                );
	}

	protected ArrayList<LoopCtrlAction> getActions(){
		return actionsNormalRun;
	}
	
	public ChabuXmitterNormal(AbortMessage abortMessage, Runnable xmitRequestListener, int priorityCount, ArrayList<ChabuChannelImpl> channels, BiFunction< Integer, Integer, Priorizer> priorizerFactory, int remoteRecvPacketSize ){
		super(abortMessage, xmitRequestListener);
		xmitBuf.order(ByteOrder.BIG_ENDIAN);
		xmitBuf.clear().limit(0);
		
		this.remoteRecvPacketSize = remoteRecvPacketSize;
		
		this.channels = channels;
		xmitChannelRequestData = priorizerFactory.apply(priorityCount, channels.size());
		xmitChannelRequestCtrl = priorizerFactory.apply(priorityCount, channels.size());
		
	}
	
	@Override
	void channelXmitRequestData(int channelId){
		synchronized(this){
			int priority = channels.get(channelId).getPriority();
			xmitChannelRequestData.request( priority, channelId );
		}
		callXmitRequestListener();
	}
	
	@Override
	void channelXmitRequestArm(int channelId){
		synchronized(this){
			int priority = channels.get(channelId).getPriority();
			xmitChannelRequestCtrl.request( priority, channelId );
		}
		callXmitRequestListener();
	}

	@Override
	protected void handleNonSeqCompletion() {
		switch( packetType ){
		case NOP: 
			xmitNop = XmitState.XMITTED; 
			break;
			
		case ABORT:
			xmitAbort = XmitState.XMITTED;
			throwAbort();
			break;
			
		default: break;
		}
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
			
			boolean isDataComplete = !seqPacketPayload.hasRemaining();
			boolean isPaddingComplete = !seqPadding.hasRemaining();
			isCompleted = isDataComplete && isPaddingComplete;
			
			if( isCompleted ){
				handleSeqCompletion();
			}
			else {
				callXmitRequestListener();
			}
		}
		return isCompleted ? LoopCtrl.None : LoopCtrl.Break;

	}

	private void handleSeqCompletion() {
		seqChannel.seqPacketCompleted();
		if( seqChannel.getXmitRemaining() > 0 && seqChannel.getXmitRemainingByRemote() > 0 ){
			xmitChannelRequestData.request( seqChannel.getPriority(), seqChannel.getChannelId());
			callXmitRequestListener();
		}

		seqPacketPayload = null;
		seqChannel       = null;
		packetType       = PacketType.NONE;
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
			seqPacketPayload = ch.handleXmitData( this, xmitBuf, remoteRecvPacketSize - ChabuImpl.SEQ_MIN_SZ );
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
	

	private ChabuChannelImpl popNextPriorizedChannelRequest(Priorizer priorizer) {
		synchronized(this){
			int reqChannel = priorizer.popNextRequest();
			if( reqChannel < 0 ) return null;
			return channels.get( reqChannel );
		}
	}

	/** 
	 * Called by channel
	 */
	void processXmitArm( int channelId, int arm ){
		xmitFillArmPacket(channelId, arm);
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


	
	@Override
	public String toString() {
		return xmitBuf.toString();
	}

}