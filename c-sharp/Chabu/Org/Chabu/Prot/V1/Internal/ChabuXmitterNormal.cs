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

namespace Org.Chabu.Prot.V1.Internal
{
    using global::System.Collections.Generic;
    using Util;
    using ByteBuffer = Org.Chabu.Prot.Util.ByteBuffer;
    using Runnable = global::System.Action;

    internal class ChabuXmitterNormal : ChabuXmitter {
	
	    /**
	     * The startup data is completely sent.
	     */
	    private XmitState xmitNop   = XmitState.IDLE;
	
	    private readonly Priorizer xmitChannelRequestData;
	    private readonly Priorizer xmitChannelRequestCtrl;
	
	    private readonly List<ChabuChannelImpl> channels;
	

	    private readonly int RemoteRecvPacketSize; 
	
	    private readonly ByteBuffer seqPadding = new ByteBuffer(3);
	    private ByteBuffer       seqPacketPayload;
	    private ChabuChannelImpl seqChannel;

        private readonly List<LoopCtrlAction> actionsNormalRun;


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
		
		    this.RemoteRecvPacketSize = remoteRecvPacketSize;
		
		    this.channels = channels;
		    xmitChannelRequestData = priorizerFactory(priorityCount, channels.size());
		    xmitChannelRequestCtrl = priorizerFactory(priorityCount, channels.size());
		
	    }
	
	    public override void channelXmitRequestData(int channelId){
            lock (this){
			    var priority = channels.get(channelId).Priority;
			    xmitChannelRequestData.request( priority, channelId );
		    }
		    callXmitRequestListener();
	    }

        public override void channelXmitRequestArm(int channelId){
		    lock(this){
			    var priority = channels.get(channelId).Priority;
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
	
	    private LoopCtrl xmitAction_RemainingSeq() {
		    var isCompleted = true;
		    if( packetType == PacketType.SEQ ){
			
			    if( seqPacketPayload.hasRemaining() ){
				    loopByteChannel.write(seqPacketPayload);
			    }
			    if( !seqPacketPayload.hasRemaining() && seqPadding.hasRemaining() ){
				    loopByteChannel.write(seqPadding);
			    }
			
			    var isDataComplete = !seqPacketPayload.hasRemaining();
			    var isPaddingComplete = !seqPadding.hasRemaining();
			    isCompleted = isDataComplete && isPaddingComplete;
			
			    if( isCompleted ){
				    HandleSeqCompletion();
			    }
			    else {
				    callXmitRequestListener();
			    }
		    }
		    return isCompleted ? LoopCtrl.None : LoopCtrl.Break;

	    }

	    private void HandleSeqCompletion() {
		    seqChannel.seqPacketCompleted();
		    if( seqChannel.XmitRemaining > 0 && seqChannel.getXmitRemainingByRemote() > 0 ){
			    xmitChannelRequestData.request( seqChannel.Priority, seqChannel.ChannelId);
			    callXmitRequestListener();
		    }

		    seqPacketPayload = null;
		    seqChannel       = null;
		    packetType       = PacketType.NONE;
	    }

        private LoopCtrl xmitAction_EvalChannelCtrl() {
		    var ch = PopNextPriorizedChannelRequest(xmitChannelRequestCtrl);
		    ch?.handleXmitCtrl( this, xmitBuf );
		    return xmitBuf.hasRemaining() ? LoopCtrl.Continue : LoopCtrl.None;
	    }

        private LoopCtrl xmitAction_EvalChannelData() {
		    ChabuChannelImpl ch = PopNextPriorizedChannelRequest(xmitChannelRequestData);
		    if( ch != null ){
			    seqPacketPayload = ch.handleXmitData( this, xmitBuf, RemoteRecvPacketSize - ChabuImpl.SEQ_MIN_SZ );
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

        private LoopCtrl xmitAction_EvalNop() {
		    if( xmitNop == XmitState.PENDING ){
			    processXmitNop();
			    xmitNop = XmitState.PREPARED;
		    }
		    return LoopCtrl.None;
	    }
        private LoopCtrl xmitAction_End() {
		    return LoopCtrl.Break;
	    }
	

	    private ChabuChannelImpl PopNextPriorizedChannelRequest(Priorizer priorizer) {
		    lock(this){
			    int reqChannel = priorizer.popNextRequest();
			    if( reqChannel < 0 ) return null;
			    return channels.get( reqChannel );
		    }
	    }

	    /** 
	     * Called by channel
	     */
	    internal void ProcessXmitArm( int channelId, int arm ){
		    xmitFillArmPacket(channelId, arm);
	    }

	    /** 
	     * Called by channel
	     */
	    internal void ProcessXmitSeq( int channelId, int seq, int payload ){
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