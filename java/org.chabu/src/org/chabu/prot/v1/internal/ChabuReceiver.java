package org.chabu.prot.v1.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.chabu.prot.v1.ChabuErrorCode;
import org.chabu.prot.v1.ChabuException;
import org.chabu.prot.v1.ChabuSetupInfo;

public class ChabuReceiver {


	private static final int HEADER_RECV_SZ = ChabuImpl.SEQ_MIN_SZ;
	
	private ByteBuffer recvBuf        = ByteBuffer.allocate( Constants.MAX_RECV_LIMIT_LOW );
	private ByteBuffer recvBufPadding = ByteBuffer.allocate( 3 );
	private Aborter aborter;
	private Setup setup;
	private ArrayList<ChabuChannelImpl> channels;
	private boolean activated = false;
	@SuppressWarnings("unused")
	private int maxReceiveSize = Constants.MAX_RECV_LIMIT_LOW;
	
	private PacketType packetType = PacketType.NONE;
	private int        packetSize;
	private int        seqPacketIndex     = 0;

	public ChabuReceiver() {
		recvBuf.order(ByteOrder.BIG_ENDIAN );
		recvBuf.clear();
		recvBuf.limit(HEADER_RECV_SZ);
	}

	public void activate(ArrayList<ChabuChannelImpl> channels, Aborter aborter, Setup setup) {
		this.channels = channels;
		this.aborter = aborter;
		this.setup = setup;
		this.activated = true;
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
				// is SEQ packet
				recvBuf.position(HEADER_RECV_SZ);
				int channelId = recvBuf.getInt(8);
				int seq = recvBuf.getInt(12);
				int pls = recvBuf.getInt(16);
				int padding = packetSize - HEADER_RECV_SZ - pls;
				ChabuChannelImpl chabuChannel = channels.get(channelId);
				if( seqPacketIndex == 0 ){
					// first processing
					Utils.ensure( padding >= 0 && padding < 4, ChabuErrorCode.ASSERT, "padding inplausible packetSize:%s pls:%d", packetSize, pls );
					chabuChannel.verifySeq( seq );
				}
				
				if( seqPacketIndex < pls ){
					int handledBytes = chabuChannel.handleRecvSeq( channel, pls-seqPacketIndex);
					seqPacketIndex += handledBytes;
				}
				
				if( seqPacketIndex >= pls && seqPacketIndex + ChabuImpl.SEQ_MIN_SZ < packetSize ){
					int paddingRemaining = packetSize - seqPacketIndex - ChabuImpl.SEQ_MIN_SZ;
					recvBufPadding.clear();
					Utils.ensure( paddingRemaining <= 3 && paddingRemaining > 0, ChabuErrorCode.ASSERT, "paddingRemaining inplausible %d (%s, %s)", paddingRemaining, seqPacketIndex, packetSize );
					recvBufPadding.limit(paddingRemaining);
					seqPacketIndex += channel.read(recvBufPadding);
				}
				
				if( seqPacketIndex + ChabuImpl.SEQ_MIN_SZ >= packetSize ){
					seqPacketIndex = 0;
					recvBuf.clear();
					recvBuf.limit(HEADER_RECV_SZ);
					packetType = PacketType.NONE;
					continue;
				}
				else {
					Utils.ensure( recvBuf.position() == 20, ChabuErrorCode.ASSERT, "" );
					Utils.ensure( recvBuf.limit() == 20, ChabuErrorCode.ASSERT, "" );
					break;
				}
			}
		}
	}

	private void processRecvSetup() {
		
		/// when is startupRx set before?
		Utils.ensure( !setup.isRemoteSetupReceived(), ChabuErrorCode.PROTOCOL_SETUP_TWICE, "Recveived SETUP twice" );
		Utils.ensure( activated, ChabuErrorCode.NOT_ACTIVATED, "While receiving the SETUP block, org.chabu was not activated." );

		String pn = getRecvString(8);
		if( !Constants.PROTOCOL_NAME.equals(pn) ) {
			aborter.delayedAbort( ChabuErrorCode.SETUP_REMOTE_CHABU_NAME.getCode(), 
					String.format("Chabu protocol name mismatch. Expected %s, received %d", Constants.PROTOCOL_NAME, pn ));
			return;
		}
		
		int pv = recvBuf.getInt();
		
		int rs = recvBuf.getInt();
		int av = recvBuf.getInt();
		String an = getRecvString(56);

		ChabuSetupInfo info = new ChabuSetupInfo( rs, av, an );

		setup.setRemote(info);

		if(( pv >>> 16 ) != (Constants.PROTOCOL_VERSION >>> 16 )) {
			aborter.delayedAbort( ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION.getCode(), String.format("Chabu Protocol Version: expt 0x%08X recv 0x%08X", 
					Constants.PROTOCOL_VERSION, pv ));
			return;
		}
				
		setup.checkConnectingValidator();
	}
	private void processRecvAccept() {
		setup.setRemoteAcceptReceived();
	}

	private void processRecvAbort() {
		
		int code =  recvBuf.getInt();
		String message = getRecvString(56);
		
		throw new ChabuException( ChabuErrorCode.REMOTE_ABORT, code, String.format("Recveived ABORT Code=0x%08X: %s", code, message ));
	}

	private void processRecvArm() {
		
		Utils.ensure( setup.isRemoteAcceptReceived(), ChabuErrorCode.ASSERT, "" );
		
		if( packetSize != 16 ){
			throw new ChabuException(String.format("Packet type ARM with unexpected len field: %s", packetSize ));
		}

		int channelId = recvBuf.getInt();
		int arm       = recvBuf.getInt();

		ChabuChannelImpl channel = channels.get(channelId);
		channel.handleRecvArm(arm);
	}

	private void processRecvResetAck() {
		throw Utils.implMissing();
	}

	private void processRecvResetReq() {
		throw Utils.implMissing();
	}

	private void processRecvNop() {
		throw Utils.implMissing();
	}

	private void processRecvDavail() {
		throw Utils.implMissing();
	}

	private String getRecvString(int maxByteCount){
		
		int len = recvBuf.getInt();
		if( len > maxByteCount ){
			throw new ChabuException(String.format("Chabu string length (%d) exceeds max allowed length (%d)",
					len, maxByteCount ));
		}
		if( recvBuf.remaining() < len ){
			throw new ChabuException(String.format("Chabu string length exceeds packet length len:%d data-remaining:%d",
					len, recvBuf.remaining() ));
		}
			
		byte[] bytes = new byte[len];
		recvBuf.get( bytes );
		while( (len & 3) != 0 ){
			len++;
			recvBuf.get();
		}
		return new String( bytes, StandardCharsets.UTF_8 );
	}
	
	public void initRecvBuf(int maxReceiveSize) {
		Utils.ensure(maxReceiveSize >= Constants.MAX_RECV_LIMIT_LOW, ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_TOO_LOW, "");
		Utils.ensure(maxReceiveSize <= Constants.MAX_RECV_LIMIT_HIGH, ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_TOO_HIGH, "");
		this.maxReceiveSize = maxReceiveSize;
	}

	@Override
	public String toString() {
		return recvBuf.toString();
	}
}
