package org.chabu.prot.v1.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;

import org.chabu.prot.v1.ChabuErrorCode;
import org.chabu.prot.v1.ChabuException;

public abstract class ChabuReceiver {

	private static final int HEADER_RECV_SZ = ChabuImpl.SEQ_MIN_SZ;
	
	protected final AbortMessage localAbortMessage;
	protected final ByteBuffer recvBuf;
	
	protected PacketType packetType = PacketType.NONE;
	protected int        packetSize = 0;

	
	public ChabuReceiver(ChabuReceiver receiver, AbortMessage localAbortMessage) {
		this.localAbortMessage = localAbortMessage;
		if( receiver == null ){
			recvBuf = ByteBuffer.allocate( Constants.MAX_RECV_LIMIT_LOW );
			recvBuf.order(ByteOrder.BIG_ENDIAN );
			recvBuf.clear();
			recvBuf.limit(HEADER_RECV_SZ);
		}
		else {
			recvBuf = receiver.recvBuf;
			packetSize = receiver.packetSize;
		}
	}
	public void recv(ByteChannel channel) throws IOException {
		while( true ){
			if( packetType == PacketType.NONE ){
				Utils.ensure(recvBuf.limit() >= HEADER_RECV_SZ, ChabuErrorCode.UNKNOWN, "unknown header size: %s", recvBuf);
				channel.read(recvBuf);
				if( recvBuf.position() < 8 ){
					break;
				}

				packetSize = recvBuf.getInt(0);
				final int packetTypeId = recvBuf.getInt(4) & 0xFF;
				packetType = PacketType.findPacketType(packetTypeId);
				if( packetType == PacketType.NONE ){
					throw new ChabuException("Packet type 0x%02X unexpected: packetSize %s", packetTypeId, packetSize );
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
				case SETUP   : processRecvSetup();    break;
				case ACCEPT  : processRecvAccept();   break; 
				case ABORT   : processRecvAbort();    break; 
				case ARM     : processRecvArm();      break; 
				case DAVAIL  : processRecvDavail();   break; 
				case NOP     : processRecvNop();      break; 
				case RST_REQ : processRecvResetReq(); break; 
				case RST_ACK : processRecvResetAck(); break; 
				default      : break;
				}
				Utils.ensure( recvBuf.remaining() < HEADER_RECV_SZ, ChabuErrorCode.ASSERT, "After normal command, the remaining bytes must be below the HEADER_RECV_SZ limit, but is %d", recvBuf.limit());
				Utils.ensure( recvBuf.position() == packetSize, ChabuErrorCode.ASSERT, "After normal command, the remaining bytes must be below the HEADER_RECV_SZ limit, but is %d", recvBuf.limit());
				recvBuf.compact();
				recvBuf.limit(HEADER_RECV_SZ);
				packetType = PacketType.NONE;
				continue;
			}
			else {
				processSeq(channel);
				
				boolean isContinuingSeq = packetType == PacketType.SEQ; 
				if( isContinuingSeq ){
					break;
				}
				
				continue;
			}
		}
	}
	protected void processSeq(ByteChannel channel) throws IOException {
		throw new RuntimeException("unexpected packet SEQ");
	}
	protected void processRecvNop(){
		throw new RuntimeException("unexpected packet NOP");
	}
	protected void processRecvResetAck(){
		throw new RuntimeException("unexpected packet Reset-Ack");
	}
	protected void processRecvResetReq(){
		throw new RuntimeException("unexpected packet Reset-Req");
	}
	protected void processRecvDavail(){
		throw new RuntimeException("unexpected packet DAVail");
	}
	protected void processRecvArm(){
		throw new RuntimeException("unexpected packet ARM");
	}
	protected void processRecvAbort(){
		throw new RuntimeException("unexpected packet Abort");
	}
	protected void processRecvAccept(){
		throw new RuntimeException("unexpected packet Accept");
	}
	protected void processRecvSetup(){
		throw new RuntimeException("unexpected packet Setup");
	}


}
