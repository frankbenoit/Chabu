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

namespace Org.Chabu.Prot.V1.Internal
{
    using global::System.Collections.Generic;
    using ByteBuffer = global::System.IO.MemoryStream;
    using Runnable = global::System.Action;

    internal class ChabuXmitterNormal : ChabuXmitter {
	
	    /**
	     * The startup data is completely sent.
	     */
	    private XmitState xmitNop   = XmitState.IDLE;
	
	    private Priorizer xmitChannelRequestData;
	    private Priorizer xmitChannelRequestCtrl;
	
	    private List<ChabuChannelImpl> channels;
	

	    private int remoteRecvPacketSize = Constants.MAX_RECV_LIMIT_LOW; 
	
	    private ByteBuffer       seqPadding = new ByteBuffer(3);
	    private ByteBuffer       seqPacketPayload;
	    private ChabuChannelImpl seqChannel;

        private List<LoopCtrlAction> actionsNormalRun;


        internal override List<LoopCtrlAction> getActions(){
		    return actionsNormalRun;
	    }
	
	    public ChabuXmitterNormal(AbortMessage abortMessage, Runnable xmitRequestListener, int priorityCount, List<ChabuChannelImpl> channels, ChabuFactory.PriorizerFactory priorizerFactory, int remoteRecvPacketSize )
            : base(abortMessage, xmitRequestListener)
        {
            actionsNormalRun = new List<LoopCtrlAction>
            {
                xmitAction_RemainingXmitBuf,
                xmitAction_RemainingSeq    ,
                xmitAction_EvalAbort       ,
                xmitAction_EvalChannelCtrl ,
                xmitAction_EvalChannelData ,
                xmitAction_EvalNop         ,
                xmitAction_End             ,
            };

            xmitBuf.order(ByteOrder.BIG_ENDIAN);
		    xmitBuf.clear().limit(0);
		
		    this.remoteRecvPacketSize = remoteRecvPacketSize;
		
		    this.channels = channels;
		    xmitChannelRequestData = priorizerFactory(priorityCount, channels.size());
		    xmitChannelRequestCtrl = priorizerFactory(priorityCount, channels.size());
		
	    }
	
	    public override void channelXmitRequestData(int channelId){
            lock (this){
			    int priority = channels.get(channelId).getPriority();
			    xmitChannelRequestData.request( priority, channelId );
		    }
		    callXmitRequestListener();
	    }

        public override void channelXmitRequestArm(int channelId){
		    lock(this){
			    int priority = channels.get(channelId).getPriority();
			    xmitChannelRequestCtrl.request( priority, channelId );
		    }
		    callXmitRequestListener();
	    }

	    protected override void handleNonSeqCompletion() {
		    switch( packetType ){
		    case PacketType.NOP: 
			    xmitNop = XmitState.XMITTED; 
			    break;
			
		    case PacketType.ABORT:
			    xmitAbort = XmitState.XMITTED;
			    throwAbort();
			    break;
			
		    default: break;
		    }
	    }
	
	    LoopCtrl xmitAction_RemainingSeq() {
		    bool isCompleted = true;
		    if( packetType == PacketType.SEQ ){
			
			    if( seqPacketPayload.hasRemaining() ){
				    loopByteChannel.write(seqPacketPayload);
			    }
			    if( !seqPacketPayload.hasRemaining() && seqPadding.hasRemaining() ){
				    loopByteChannel.write(seqPadding);
			    }
			
			    bool isDataComplete = !seqPacketPayload.hasRemaining();
			    bool isPaddingComplete = !seqPadding.hasRemaining();
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
	
	    LoopCtrl xmitAction_EvalChannelCtrl() {
		    ChabuChannelImpl ch = popNextPriorizedChannelRequest(xmitChannelRequestCtrl);
		    if( ch != null ){
			    ch.handleXmitCtrl( this, xmitBuf );
		    }
		    return xmitBuf.hasRemaining() ? LoopCtrl.Continue : LoopCtrl.None;
	    }

	    LoopCtrl xmitAction_EvalChannelData() {
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
					    ChabuErrorCode.ASSERT, "{0} = {1} + {2} + {3}", xmitBuf.getInt(0), xmitBuf.remaining(), seqPacketPayload.remaining(), seqPadding.remaining());
		    }
		    return xmitBuf.hasRemaining() ? LoopCtrl.Continue : LoopCtrl.None;
	    }
	
	    LoopCtrl xmitAction_EvalNop() {
		    if( xmitNop == XmitState.PENDING ){
			    processXmitNop();
			    xmitNop = XmitState.PREPARED;
		    }
		    return LoopCtrl.None;
	    }
	    LoopCtrl xmitAction_End() {
		    return LoopCtrl.Break;
	    }
	

	    private ChabuChannelImpl popNextPriorizedChannelRequest(Priorizer priorizer) {
		    lock(this){
			    int reqChannel = priorizer.popNextRequest();
			    if( reqChannel < 0 ) return null;
			    return channels.get( reqChannel );
		    }
	    }

	    /** 
	     * Called by channel
	     */
	    internal void processXmitArm( int channelId, int arm ){
		    xmitFillArmPacket(channelId, arm);
	    }

	    /** 
	     * Called by channel
	     */
	    internal void processXmitSeq( int channelId, int seq, int payload ){
		    checkXmitBufEmptyOrThrow("Cannot xmit SEQ, buffer is not empty");
		
		    xmitFillStart( PacketType.SEQ );
		    xmitFillAddInt( channelId );
		    xmitFillAddInt( seq );
		    xmitFillAddInt( payload ); // PLS
		    int packetSize = ChabuImpl.SEQ_MIN_SZ + payload;
		    int packetSizeAligned = Utils.alignUpTo4(packetSize);
		    xmitFillComplete( packetSizeAligned );
	    }


	
	    public override string ToString() {
		    return xmitBuf.ToString();
	    }

    }
}