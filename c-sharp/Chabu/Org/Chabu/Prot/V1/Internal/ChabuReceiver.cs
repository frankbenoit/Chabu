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
    using ByteBuffer = global::System.IO.MemoryStream;
    internal abstract class ChabuReceiver {

	    private static readonly int HEADER_RECV_SZ = ChabuImpl.SEQ_MIN_SZ;
	
	    protected readonly AbortMessage localAbortMessage;
	    protected readonly ByteBuffer recvBuf;
	
	    protected PacketType packetType = PacketType.NONE;
	    protected int        packetSize = 0;
	    protected bool cancelCurrentReceive = false;

	
	    public ChabuReceiver(ChabuReceiver receiver, AbortMessage localAbortMessage) {
		    this.localAbortMessage = localAbortMessage;
		    if( receiver == null ){
			    recvBuf = new ByteBuffer( Constants.MAX_RECV_LIMIT_LOW );
			    recvBuf.order(ByteOrder.BIG_ENDIAN );
			    recvBuf.clear();
			    recvBuf.limit(HEADER_RECV_SZ);
		    }
		    else {
			    recvBuf = receiver.recvBuf;
			    packetSize = receiver.packetSize;
		    }
	    }
	    public void recv(ByteChannel channel) {
		    cancelCurrentReceive = false;
		    while( !cancelCurrentReceive ){
			    if( packetType == PacketType.NONE ){
				    Utils.ensure(recvBuf.limit() >= HEADER_RECV_SZ, ChabuErrorCode.UNKNOWN, "unknown header size: {0}", recvBuf);
				    channel.read(recvBuf);
				    if( recvBuf.position() < 8 ){
					    break;
				    }

				    packetSize = recvBuf.getInt(0);
				    int packetTypeId = recvBuf.getInt(4) & 0xFF;
				    packetType = PacketTypeExtension.findPacketType(packetTypeId);
				    if( packetType == PacketType.NONE ){
					    throw new ChabuException(string.Format("Packet type 0x%02X unexpected: packetSize {0}", packetTypeId, packetSize ));
				    }
			    }
			
			    if( packetType != PacketType.SEQ ){
				    Utils.ensure( packetSize <= Constants.MAX_RECV_LIMIT_LOW, ChabuErrorCode.UNKNOWN, "unknown header size");
				    if( packetSize > recvBuf.position() ){
					    recvBuf.limit(packetSize);
					    channel.read(recvBuf);
					    if( recvBuf.hasRemaining() ){
						    // not fully read, try next time
						    break;
					    }
				    }
			    }
			    else {
				    if( HEADER_RECV_SZ > recvBuf.position() ){
					    recvBuf.limit(HEADER_RECV_SZ);
					    channel.read(recvBuf);
					    if( recvBuf.hasRemaining() ){
						    // not fully read, try next time
						    break;
					    }
				    }
				
			    }
			
			    recvBuf.flip();
			    recvBuf.position(8);
			
			    if( packetType != PacketType.SEQ ){
				    Utils.ensure( packetSize <= Constants.MAX_RECV_LIMIT_LOW, ChabuErrorCode.UNKNOWN, "unknown header size");
				    switch( packetType ){
				    case PacketType.SETUP   : processRecvSetup();    break;
				    case PacketType.ACCEPT  : processRecvAccept();   break; 
				    case PacketType.ABORT   : processRecvAbort();    break; 
				    case PacketType.ARM     : processRecvArm();      break; 
				    case PacketType.DAVAIL  : processRecvDavail();   break; 
				    case PacketType.NOP     : processRecvNop();      break; 
				    case PacketType.RST_REQ : processRecvResetReq(); break; 
				    case PacketType.RST_ACK : processRecvResetAck(); break; 
				    default      : break;
				    }
				    Utils.ensure( recvBuf.remaining() < HEADER_RECV_SZ, ChabuErrorCode.ASSERT, "After normal command, the remaining bytes must be below the HEADER_RECV_SZ limit, but is {0}", recvBuf.limit());
				    Utils.ensure( recvBuf.position() == packetSize, ChabuErrorCode.ASSERT, "After normal command, the remaining bytes must be below the HEADER_RECV_SZ limit, but is {0}", recvBuf.limit());
				    recvBuf.compact();
				    recvBuf.limit(HEADER_RECV_SZ);
				    packetType = PacketType.NONE;
				    continue;
			    }
			    else {
				    processSeq(channel);
				
				    bool isContinuingSeq = packetType == PacketType.SEQ; 
				    if( isContinuingSeq ){
					    break;
				    }
				
				    continue;
			    }
		    }
	    }
        protected virtual void processSeq(ByteChannel channel) {
		    throw new global::System.SystemException("unexpected packet SEQ");
	    }
        protected virtual void processRecvNop(){
		    throw new global::System.SystemException("unexpected packet NOP");
	    }
        protected virtual void processRecvResetAck(){
		    throw new global::System.SystemException("unexpected packet Reset-Ack");
	    }
        protected virtual void processRecvResetReq(){
		    throw new global::System.SystemException("unexpected packet Reset-Req");
	    }
        protected virtual void processRecvDavail(){
		    throw new global::System.SystemException("unexpected packet DAVail");
	    }
        protected virtual void processRecvArm(){
		    throw new global::System.SystemException("unexpected packet ARM");
	    }
        protected virtual void processRecvAbort(){
		    throw new global::System.SystemException("unexpected packet Abort");
	    }
        protected virtual void processRecvAccept(){
		    throw new global::System.SystemException("unexpected packet Accept");
	    }
	    protected virtual void processRecvSetup(){
		    throw new global::System.SystemException("unexpected packet Setup");
	    }


    }
}
