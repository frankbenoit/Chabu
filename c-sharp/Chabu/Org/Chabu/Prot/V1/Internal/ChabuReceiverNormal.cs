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
    using global::System.Text;
    using ByteBuffer = global::System.IO.MemoryStream;

    internal class ChabuReceiverNormal : ChabuReceiver {


	    private static readonly int HEADER_RECV_SZ = ChabuImpl.SEQ_MIN_SZ;
	
	    private readonly ByteBuffer recvBufPadding = new ByteBuffer( 3 );
	    private readonly Setup setup;
	    private readonly List<ChabuChannelImpl> channels;
	
	    private int        seqPacketIndex     = 0;
	
	    public ChabuReceiverNormal(ChabuReceiver receiver, List<ChabuChannelImpl> channels, AbortMessage localAbortMessage, Setup setup)
            : base(receiver, localAbortMessage )
        {
		    this.channels = channels;
		    this.setup = setup;
	    }

	    protected override void processSeq(ByteChannel channel ) {
		    // is SEQ packet
		    recvBuf.position(HEADER_RECV_SZ);
		    int channelId = recvBuf.getInt(8);
		    int seq = recvBuf.getInt(12);
		    int pls = recvBuf.getInt(16);
		    int padding = packetSize - HEADER_RECV_SZ - pls;
		    ChabuChannelImpl chabuChannel = channels.get(channelId);
		    if( seqPacketIndex == 0 ){
			    // first processing
			    Utils.ensure( padding >= 0 && padding < 4, ChabuErrorCode.ASSERT, "padding inplausible packetSize:{0} pls:{1}", packetSize, pls );
			    chabuChannel.verifySeq( seq );
		    }

		    if( seqPacketIndex < pls ){
			    int toBeHandledLimit = pls-seqPacketIndex;
			    int handledBytes = chabuChannel.handleRecvSeq( channel, toBeHandledLimit);
			    seqPacketIndex += handledBytes;
		    }
		
		    if( seqPacketIndex >= pls && seqPacketIndex + ChabuImpl.SEQ_MIN_SZ < packetSize ){
			    int paddingRemaining = packetSize - seqPacketIndex - ChabuImpl.SEQ_MIN_SZ;
			    recvBufPadding.clear();
			    Utils.ensure( paddingRemaining <= 3 && paddingRemaining > 0, ChabuErrorCode.ASSERT, "paddingRemaining inplausible {0} ({1}, {2})", paddingRemaining, seqPacketIndex, packetSize );
			    recvBufPadding.limit(paddingRemaining);
			    seqPacketIndex += channel.read(recvBufPadding);
		    }
		
		    if( seqPacketIndex + ChabuImpl.SEQ_MIN_SZ >= packetSize ){
			    seqPacketIndex = 0;
			    recvBuf.clear();
			    recvBuf.limit(HEADER_RECV_SZ);
			    packetType = PacketType.NONE;
		    }
		    else {
			    Utils.ensure( recvBuf.position() == 20, ChabuErrorCode.ASSERT, "" );
			    Utils.ensure( recvBuf.limit() == 20, ChabuErrorCode.ASSERT, "" );
		    }
	    }

	    protected override void processRecvAbort() {
		
		    int code =  recvBuf.getInt();
		    string message = getRecvString(56);
		
		    throw new ChabuException( ChabuErrorCode.REMOTE_ABORT, code, string.Format("Recveived ABORT Code=0x{0:X8}: {1}", code, message ));
	    }

	    protected override void processRecvArm() {
		
		    Utils.ensure( setup.isRemoteAcceptReceived(), ChabuErrorCode.ASSERT, "" );
		
		    if( packetSize != 16 ){
			    throw new ChabuException(string.Format("Packet type ARM with unexpected len field: {0}", packetSize ));
		    }

		    int channelId = recvBuf.getInt();
		    int arm       = recvBuf.getInt();

		    ChabuChannelImpl channel = channels.get(channelId);
		    channel.handleRecvArm(arm);
	    }


	    private string getRecvString(int maxByteCount){
		
		    int len = recvBuf.getInt();
		    if( len > maxByteCount ){
			    throw new ChabuException(string.Format("Chabu string length ({0}) exceeds max allowed length ({1})",
					    len, maxByteCount ));
		    }
		    if( recvBuf.remaining() < len ){
			    throw new ChabuException(string.Format("Chabu string length exceeds packet length len:{0} data-remaining:{1}",
					    len, recvBuf.remaining() ));
		    }
			
		    byte[] bytes = new byte[len];
		    recvBuf.get( bytes );
		    while( (len & 3) != 0 ){
			    len++;
			    recvBuf.get();
		    }
		    return Encoding.UTF8.GetString(bytes);
	    }
	
	    public override string ToString() {
		    return recvBuf.ToString();
	    }
    }
}
