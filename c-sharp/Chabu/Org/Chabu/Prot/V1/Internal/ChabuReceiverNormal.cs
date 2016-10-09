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

    import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.chabu.prot.v1.ChabuErrorCode;
import org.chabu.prot.v1.ChabuException;

public class ChabuReceiverNormal extends ChabuReceiver {


	private static readonly int HEADER_RECV_SZ = ChabuImpl.SEQ_MIN_SZ;
	
	private readonly ByteBuffer recvBufPadding = ByteBuffer.allocate( 3 );
	private readonly Setup setup;
	private readonly ArrayList<ChabuChannelImpl> channels;
	
	private int        seqPacketIndex     = 0;
	
	public ChabuReceiverNormal(ChabuReceiver receiver, ArrayList<ChabuChannelImpl> channels, AbortMessage localAbortMessage, Setup setup) {
		super( receiver, localAbortMessage );
		this.channels = channels;
		this.setup = setup;
	}

	@Override
	protected void processSeq(ByteChannel channel ) throws IOException{
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
			int toBeHandledLimit = pls-seqPacketIndex;
			int handledBytes = chabuChannel.handleRecvSeq( channel, toBeHandledLimit);
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
		}
		else {
			Utils.ensure( recvBuf.position() == 20, ChabuErrorCode.ASSERT, "" );
			Utils.ensure( recvBuf.limit() == 20, ChabuErrorCode.ASSERT, "" );
		}
	}

	@Override
	protected void processRecvAbort() {
		
		int code =  recvBuf.getInt();
		String message = getRecvString(56);
		
		throw new ChabuException( ChabuErrorCode.REMOTE_ABORT, code, String.format("Recveived ABORT Code=0x%08X: %s", code, message ));
	}

	@Override
	protected void processRecvArm() {
		
		Utils.ensure( setup.isRemoteAcceptReceived(), ChabuErrorCode.ASSERT, "" );
		
		if( packetSize != 16 ){
			throw new ChabuException(String.format("Packet type ARM with unexpected len field: %s", packetSize ));
		}

		int channelId = recvBuf.getInt();
		int arm       = recvBuf.getInt();

		ChabuChannelImpl channel = channels.get(channelId);
		channel.handleRecvArm(arm);
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
	
	@Override
	public String toString() {
		return recvBuf.toString();
	}
}
