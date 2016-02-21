package org.chabu.internal;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.chabu.ChabuErrorCode;
import org.chabu.ChabuException;
import org.chabu.ChabuSetupInfo;

public class ChabuReceiver {

	private static final int SEQ_MIN_SZ = 24;
	
	private ByteBuffer recvBuf;
	private ChabuXmitter xmitter;
	private Setup setup;
	private PrintWriter traceWriter;
	private ArrayList<ChabuChannelImpl> channels;
	private boolean activated = false;
	
	public void recv(ByteBuffer buf) {
		// prepare trace
		PrintWriter trc = traceWriter;
		int trcStartPos = buf.position();
		
		// start real work
		
		int oldRemaining = -1;
		while( oldRemaining != buf.remaining() ){
			oldRemaining = buf.remaining();

			// ensure we have len+type
			Utils.transferUpTo( buf, recvBuf, 8 );
			if( recvBuf.position() < 8 ){
				break;
			}
			
			int ps = recvBuf.getInt(0);
			if( ps > recvBuf.capacity() ){
				xmitter.delayedAbort(ChabuErrorCode.PROTOCOL_LENGTH, 
						String.format("Packet with too much data: len %s", ps ));
				// set all recv to be consumed.
				buf.position( buf.limit() );
			}

			recvBuf.limit(ps);
			Utils.transferRemaining(buf, recvBuf);


			if( !recvBuf.hasRemaining() ){
				
				// completed, now start processing
				recvBuf.flip();
				recvBuf.position(4);

				try{
					
					int packetTypeId = recvBuf.getInt() & 0xFF;
					PacketType packetType = PacketType.findPacketType(packetTypeId);
					if( packetType == null ){
						xmitter.delayedAbort( ChabuErrorCode.PROTOCOL_PCK_TYPE, 
								String.format("Packet type cannot be found 0x%02X", packetTypeId ));
						return;
					}

					if( !setup.isRemoteSetupReceived() && packetType != PacketType.SETUP ){
						xmitter.delayedAbort( ChabuErrorCode.PROTOCOL_EXPECTED_SETUP, 
								String.format("Recveived %s, but SETUP was expected", packetType ));
						return;
					}
					
					switch( packetType ){
					case SETUP : processRecvSetup();  break;
					case ACCEPT: processRecvAccept(); break; 
					case ABORT : processRecvAbort();  break; 
					case ARM   : processRecvArm();    break; 
					case SEQ   : processRecvSeq();    break; 
					default    : throw new ChabuException(String.format(
							"Packet type 0x%02X unexpected: ps %s", packetTypeId, ps ));
					}
		
					if( recvBuf.hasRemaining() ){
						throw new ChabuException(String.format(
								"Packet type 0x%02X left some bytes unconsumed: %s bytes", 
								packetTypeId, recvBuf.remaining() ));
					}
				}
				finally {
					recvBuf.clear();
				}
			}
		}
		
		// write out trace info
		if( trc != null ){
			trc.printf( "WIRE_RX: {}%n");
			Utils.printTraceHexData(trc, buf, trcStartPos, buf.position());
		}
	}
	private String protocolVersionToString( int version ){
		return String.format("%d.%d", version >>> 16, version & 0xFFFF );
	}
	private void processRecvSetup() {
		
		/// when is startupRx set before?
		Utils.ensure( !setup.isRemoteSetupReceived(), ChabuErrorCode.PROTOCOL_SETUP_TWICE, "Recveived SETUP twice" );
		Utils.ensure( activated, ChabuErrorCode.NOT_ACTIVATED, "While receiving the SETUP block, org.chabu was not activated." );

		String pn = getRecvString();
		if( !ChabuImpl.PROTOCOL_NAME.equals(pn) ) {
			xmitter.delayedAbort( ChabuErrorCode.SETUP_REMOTE_CHABU_NAME.getCode(), 
					String.format("Chabu protocol name mismatch. Expected %s, received %d", ChabuImpl.PROTOCOL_NAME, pn ));
			return;
		}
		
		int pv = recvBuf.getInt();
		
		int rs = recvBuf.getInt();
		int av = recvBuf.getInt();
		String an = getRecvString();

		ChabuSetupInfo info = new ChabuSetupInfo( rs, av, an );

		setup.setRemote(info);

		setup.setRecvSetupCompleted( RecvState.RECVED );	

		if(( pv >>> 16 ) != (ChabuImpl.PROTOCOL_VERSION >>> 16 )) {
			xmitter.delayedAbort( ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION.getCode(), String.format("Chabu protocol version mismatch. Expected %s, received %s", 
					protocolVersionToString(ChabuImpl.PROTOCOL_VERSION), protocolVersionToString(pv) ));
			return;
		}
				
		setup.checkConnectingValidator(xmitter);
	}
	private void processRecvAccept() {
		setup.setRemoteAcceptReceived();
	}

	private void processRecvAbort() {
		
		int code =  recvBuf.getInt();
		String message = getRecvString();
		
		throw new ChabuException( ChabuErrorCode.REMOTE_ABORT, code, String.format("Recveived ABORT Code=0x%08X: %s", code, message ));
	}

	private void processRecvArm() {
		
		Utils.ensure( setup.isRemoteAcceptReceived(), ChabuErrorCode.ASSERT, "" );
		
		if( recvBuf.limit() != 16 ){
			throw new ChabuException(String.format("Packet type ARM with unexpected len field: %s", 16 ));
		}

		int channelId = recvBuf.getInt();
		int arm       = recvBuf.getInt();

		ChabuChannelImpl channel = channels.get(channelId);
		channel.handleRecvArm(arm);
	}

	private void processRecvSeq() {
		checkRecvSeqMinLength();

		int channelId = recvBuf.getInt();
		int seq       = recvBuf.getInt();
		@SuppressWarnings("unused")
		int dav       = recvBuf.getInt();
		int pls       = recvBuf.getInt();

		checkRecvSeqChannelValid(channelId);
		checkRecvSeqPlsValid(pls);
		processRecvSeqToChannel(channelId, seq, pls);
	}

	private void processRecvSeqToChannel(int channelId, int seq, int pls) {
		ChabuChannelImpl channel = channels.get(channelId);
		channel.handleRecvSeq( seq, recvBuf, pls );
	}

	private void checkRecvSeqPlsValid(int pls) {
		if( recvBuf.limit() != Utils.alignUpTo4( SEQ_MIN_SZ+pls )){
			throw new ChabuException("Packet type SEQ with unexpected len field: %s, PLS %s", 
					recvBuf.limit(), pls );
		}
	}

	private void checkRecvSeqChannelValid(int channelId) {
		if( channelId >= channels.size() ){
			throw new ChabuException("Packet type SEQ with invalid channel ID %s, available channels %s", 
					channelId, channels.size() );
		}
	}

	private void checkRecvSeqMinLength() {
		if( recvBuf.limit() < SEQ_MIN_SZ ){
			throw new ChabuException("Packet type SEQ with unexpected len field (too small): %s", 
					recvBuf.limit() );
		}
	}
	private String getRecvString(){
		
		int len = recvBuf.getInt();
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
		recvBuf = ByteBuffer.allocate( maxReceiveSize );
		recvBuf.order(ByteOrder.BIG_ENDIAN );
		recvBuf.clear();
	}

	@Override
	public String toString() {
		return recvBuf.toString();
	}
	public void activate(ArrayList<ChabuChannelImpl> channels, ChabuXmitter xmitter, Setup setup) {
		this.channels = channels;
		this.xmitter = xmitter;
		this.setup = setup;
		this.activated = true;
	}
	public void setTracePrinter(PrintWriter writer) {
		this.traceWriter = writer;
	}
}
