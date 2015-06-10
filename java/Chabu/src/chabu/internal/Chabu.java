package chabu.internal;

import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.function.Consumer;

import chabu.ChabuSetupInfo;
import chabu.ChabuConnectionAcceptInfo;
import chabu.ChabuErrorCode;
import chabu.ChabuException;
import chabu.IChabu;
import chabu.IChabuConnectingValidator;
import chabu.IChabuNetwork;


public class Chabu implements IChabu {

	public static final int PROTOCOL_VERSION = 1;

	private ArrayList<ChabuChannel> channels = new ArrayList<>(256);
	private int      priorityCount = 1;
	private BitSet[] xmitChannelRequest;
	private int      xmitLastChannelIdx = 0;
//	private BitSet[] recvChannelRequest;
//	private int      recvChannelIdx = 0;
	
	private ByteBuffer xmitHeader = ByteBuffer.allocate( 0x100 );
	private ByteBuffer recvHeader = ByteBuffer.allocate( 0x100 );

	private IChabuNetwork nw;
	
	/**
	 * The setup data is completely received.
	 */
	private RecvState recvSetupCompleted = RecvState.WAITING;
	/**
	 * The startup data is completely sent.
	 */
	private XmitState xmitStartupCompleted = XmitState.IDLE;

	
	private boolean activated = false;

	/**
	 * Have sent the ACCEPT packet
	 */
	private XmitState xmitAccepted = XmitState.IDLE;
	/**
	 * Have recv the ACCEPT packet
	 */
	private RecvState recvAccepted = RecvState.IDLE;

	int maxChannelId;
//	String instanceName = "Chabu";
	
	private ChabuSetupInfo infoLocal;
	private ChabuSetupInfo infoRemote;

	private XmitState    xmitAbortPending = XmitState.IDLE;
	private int          xmitAbortCode    = 0;
	private String       xmitAbortMessage = "";

	private PrintWriter traceWriter;

	private IChabuConnectingValidator val;
	
	public Chabu( ChabuSetupInfo info ){
		
		Utils.ensure( info.maxReceivePayloadSize >= 0x100 && info.maxReceivePayloadSize <= 0xFFFF, ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE, 
				"maxReceivePayloadSize must be in range 0x100 .. 0xFFFF, but is %s", info.maxReceivePayloadSize );
		
		Utils.ensure( info.applicationName != null, ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME, 
				"applicationName must not be null" );
		
		int nameBytes = info.applicationName.getBytes( StandardCharsets.UTF_8).length;
		Utils.ensure( nameBytes <= 200, ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME, 
				"applicationName must have length at maximum 200 UTF8 bytes, but has %s", nameBytes );
		
		this.infoLocal = info;
		
		recvHeader.order(ByteOrder.BIG_ENDIAN);
		recvHeader.clear();
		
		xmitHeader.order(ByteOrder.BIG_ENDIAN);
		xmitHeader.clear().limit(0);
		
	}
	
	/**
	 * The firmware does not allow to add channels during the operational time.
	 * The ModuleLink feature need to allow to add further channels during the runtime.
	 * The Cte must ensure itself that the channel is available at firmware side.
	 * @param channel
	 */
	public void addChannel( ChabuChannel channel ){
		channels.add(channel);
	}
	public void setPriorityCount( int priorityCount ){
		Utils.ensure( !activated, ChabuErrorCode.IS_ACTIVATED, "Priority count cannot be set when alread activated." );
		Utils.ensure( priorityCount >= 1 && priorityCount <= 20, ChabuErrorCode.CONFIGURATION_PRIOCOUNT, "Priority count must be in range 1..20, but is %s", priorityCount );
		this.priorityCount = priorityCount;
	}
	
	/**
	 * Priority that forces the processing to be done with highest possible priority.
	 * Lower priority values result in later processing.
	 */
	public int getHighestValidPriority(){
		return priorityCount-1;
	}
	
	/**
	 * When activate is called, chabu enters operation. No subsequent calls to {@link #addChannel(ChabuChannel)} or {@link #setPriorityCount(int)} are allowed.
	 */
	public void activate(){
		Utils.ensure( !activated, ChabuErrorCode.ASSERT, "activated called twice" );
		Utils.ensure( channels.size() > 0, ChabuErrorCode.CONFIGURATION_NO_CHANNELS, "No channels are set." );
		for( int i = 0; i < channels.size(); i++ ){
			ChabuChannel ch = channels.get(i);
			Utils.ensure( ch.getPriority() < priorityCount, ChabuErrorCode.CONFIGURATION_CH_PRIO, "Channel %s has higher priority (%s) as the max %s", i, ch.getPriority(), priorityCount );
			ch.activate(this, i );
		}
		
		xmitChannelRequest = new BitSet[ priorityCount ];
		for (int i = 0; i < xmitChannelRequest.length; i++) {
			xmitChannelRequest[i] = new BitSet(channels.size());
		}
		
//		recvChannelRequest = new BitSet[ priorityCount ];
//		for (int i = 0; i < recvChannelRequest.length; i++) {
//			recvChannelRequest[i] = new BitSet(channels.size());
//		}
		
		maxChannelId = channels.size() -1;
		activated = true;
		
		processXmitSetup();
	}
	
	/**
	 * Receive the data from the network and process it into the channels.
	 */
	public void evRecv(ByteBuffer buf) {
		// prepare trace
		PrintWriter trc = traceWriter;
		int trcStartPos = buf.position();
		
		// start real work
		
//		while( calcNextRecvChannel() ){
//			ChabuChannel ch = channels.get(recvChannelIdx);
//			ch.handleRecv(null);
//		}
		
		int oldRemaining = -1;
		outerloop: while( oldRemaining != buf.remaining() ){
			oldRemaining = buf.remaining();
			

			// ensure we have len+type
			while( recvHeader.position() < 2 ){
				if( !buf.hasRemaining() ){
					break outerloop;
				}
				recvHeader.put( buf.get() );
			}
			
			int len = recvHeader.getShort(0) & 0xFFFF;
			int pckSz = 2 + len;
			if( pckSz > recvHeader.capacity() ){
				delayedAbort(ChabuErrorCode.PROTOCOL_LENGTH, String.format("Packet with too much data: len %s", len ));
				// set all recv to be consumed.
				buf.position( buf.limit() );
			}

			recvHeader.limit(pckSz);
			Utils.transferRemaining(buf, recvHeader);


			if( !recvHeader.hasRemaining() ){
				
				// completed, now start processing
				recvHeader.flip();
				recvHeader.position(2);

				try{
					
					int packetTypeId = recvHeader.get() & 0xFF;
					PacketType packetType = PacketType.findPacketType(packetTypeId);
					if( packetType == null ){
						throw new ChabuException(String.format("Packet type cannot be found 0x%02X", packetTypeId ));
					}

					if( recvSetupCompleted != RecvState.RECVED && packetType != PacketType.SETUP ){
						throw new ChabuException(String.format("Recveived %s, but SETUP was expected", packetType ));
					}
					
					switch( packetType ){
					case SETUP : processRecvSetup();  break;
					case ACCEPT: processRecvAccept(); break; 
					case ABORT : processRecvAbort();  break; 
					case ARM   : processRecvArm();    break; 
					case SEQ   : processRecvSeq();    break; 
					default    : throw new ChabuException(String.format("Packet type 0x%02X unexpected: len %s", packetTypeId, len ));
					}
		
					if( recvHeader.hasRemaining() ){
						throw new ChabuException(String.format("Packet type 0x%02X left some bytes unconsumed: %s bytes", packetTypeId, recvHeader.remaining() ));
					}
				}
				finally {
					recvHeader.clear();
				}
			}
		}
		
		// write out trace info
		if( trc != null ){
			trc.printf( "WIRE_RX: {}%n");
			Utils.printTraceHexData(trc, buf, trcStartPos, buf.position());
		}
	}

	private void processRecvSetup() {
		
		/// when is startupRx set before?
		Utils.ensure( recvSetupCompleted != RecvState.RECVED, ChabuErrorCode.PROTOCOL_SETUP_TWICE, "Recveived SETUP twice" );
		Utils.ensure( activated, ChabuErrorCode.NOT_ACTIVATED, "While receiving the SETUP block, chabu was not activated." );

		int chabuProtocolVersion = recvHeader.get() & 0xFF;
		
		ChabuSetupInfo info = new ChabuSetupInfo();
		info.maxReceivePayloadSize = recvHeader.getShort() & 0xFFFF;
		info.applicationVersion    = recvHeader.getInt();
		info.applicationName       = getRecvString();
		
		this.infoRemote = info;

		recvSetupCompleted = RecvState.RECVED;	

		if( chabuProtocolVersion != PROTOCOL_VERSION) {
			delayedAbort( ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION.getCode(), String.format("Chabu protocol version mismatch. Expected %d, received %d", PROTOCOL_VERSION, chabuProtocolVersion ));
			return;
		}
				


		checkConnectingValidator();
	}

	private void processRecvAccept() {
		Utils.ensure( recvAccepted != RecvState.RECVED, ChabuErrorCode.PROTOCOL_ACCEPT_TWICE, "Recveived ACCEPT twice" );
		recvAccepted = RecvState.RECVED;
	}

	private void processRecvAbort() {
		
		int code =  recvHeader.getInt();
		String message = getRecvString();
		
		Utils.fail( ChabuErrorCode.REMOTE_ABORT, String.format("Recveived ABORT Code=0x%08X: %s", code, message ));
	}

	private void processRecvArm() {
		
		Utils.ensure( recvAccepted == RecvState.RECVED, ChabuErrorCode.ASSERT, "" );
		
		if( recvHeader.limit() != PacketType.ARM.headerSize+2 ){
			throw new ChabuException(String.format("Packet type ARM with unexpected len field: %s", recvHeader.limit()-2 ));
		}

		int channelId = recvHeader.getShort() & 0xFFFF;
		int arm       = recvHeader.getInt();

		ChabuChannel channel = channels.get(channelId);
		channel.handleRecvArm(arm);
	}

	private void processRecvSeq() {
		
		if( recvHeader.limit() < PacketType.SEQ.headerSize+2 ){
			throw new ChabuException(String.format("Packet type SEQ with unexpected len field (too small): %s", recvHeader.limit()-2 ));
		}

		int channelId = recvHeader.getShort() & 0xFFFF;
		int seq       = recvHeader.getInt();
		int pls       = recvHeader.getShort() & 0xFFFF;

		if( channelId >= channels.size() ){
			throw new ChabuException(String.format("Packet type SEQ with invalid channel ID %s, available channels %s", channelId, channels.size() ));
		}
		if( recvHeader.limit() != PacketType.SEQ.headerSize+2+pls ){
			throw new ChabuException(String.format("Packet type SEQ with unexpected len field: %s, PLS %s", recvHeader.limit()-2, pls ));
		}
		if( recvHeader.remaining() != pls ){
			throw new ChabuException(String.format("Packet type SEQ with unexpected len field (too small): %s", recvHeader.limit()-2 ));
		}

		ChabuChannel channel = channels.get(channelId);
		channel.handleRecvSeq( seq, recvHeader );
	}
	private void checkConnectingValidator() {
		if( xmitAccepted != XmitState.XMITTED && recvSetupCompleted == RecvState.RECVED && xmitStartupCompleted == XmitState.XMITTED ){
			
			if( infoRemote.maxReceivePayloadSize < 0x100 || infoRemote.maxReceivePayloadSize >= 0x10000 ){
			delayedAbort(ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE.getCode(), 
					String.format("MaxReceivePayloadSize must be on range 0x100 .. 0xFFFF bytes, but SETUP from remote contained 0x%02X", 
					infoRemote.maxReceivePayloadSize));
			}

			boolean isOk = true;
			if( val != null ){
				ChabuConnectionAcceptInfo acceptInfo = val.isAccepted(infoLocal, infoRemote);
				if( acceptInfo != null && acceptInfo.code != 0 ){
					isOk = false;

					delayedAbort(acceptInfo.code, acceptInfo.message );
					
				}
			}
			val = null;

			if( isOk && this.xmitHeader.capacity() != infoRemote.maxReceivePayloadSize ){
				this.xmitHeader = ByteBuffer.allocate( infoRemote.maxReceivePayloadSize );
				this.xmitHeader.order( ByteOrder.BIG_ENDIAN );
				this.xmitHeader.limit( 0 );
			}

			prepareXmitAccept();
			xmitAccepted = XmitState.PREPARED;
		}
	}

	private void delayedAbort(ChabuErrorCode ec, String message) {
		delayedAbort(ec.getCode(), message);
	}
	private void delayedAbort(int code, String message) {
		Utils.ensure( xmitAbortPending == XmitState.IDLE, ChabuErrorCode.ASSERT, "Abort is already pending while generating Abort from Validator");
		
		xmitAbortCode    = code;
		xmitAbortMessage = message;
		xmitAbortPending = XmitState.PENDING;
		
		nw.evUserXmitRequest();

	}

	
//	void evUserRecvRequest(int channelId){
//		synchronized(this){
//			int priority = channels.get(channelId).priority;
//			recvChannelRequest[priority].set( channelId );
//		}
//		nw.evUserRecvRequest();
//	}

	void evUserXmitRequest(int channelId){
		synchronized(this){
			int priority = channels.get(channelId).getPriority();
			Utils.ensure(priority < xmitChannelRequest.length, ChabuErrorCode.ASSERT, "priority:%s < xmitChannelRequest.length:%s", priority, xmitChannelRequest.length );
			xmitChannelRequest[priority].set( channelId );
		}
		nw.evUserXmitRequest();
	}


	/**
	 * Copy the User data from the Channel to the buffer.
	 * 
	 * @param buf
	 * @return 	true 	not implemented
	 * 			false 	no flush performed.
	 */
	public boolean evXmit(ByteBuffer buf) {
		// prepare trace
		PrintWriter trc = traceWriter;
		int trcStartPos = buf.position();

		// start real work
		while( buf.hasRemaining() ){
			
			Utils.transferRemaining( xmitHeader, buf );

			if( xmitHeader.hasRemaining() ){
				break;
			}
			if( xmitStartupCompleted != XmitState.IDLE ){
				if( xmitStartupCompleted == XmitState.PREPARED ){
					xmitStartupCompleted = XmitState.XMITTED;
					checkConnectingValidator();
					continue;
				}
			}
			if( recvSetupCompleted != RecvState.RECVED ){
				break;
			}
			if( xmitAbortPending == XmitState.PENDING ){
				
				int code = xmitAbortCode;
				String msg = xmitAbortMessage;
				xmitAbortCode = 0;
				xmitAbortMessage = "";
				xmitAbortPending = XmitState.XMITTED;
				Utils.fail( code, "%s", msg );
			}
			if( xmitAbortPending != XmitState.IDLE ){				
				if( xmitAbortPending == XmitState.PENDING ){
					processXmitAbort();
					continue;
				}
			}

			ChabuChannel nextXmitChannel = calcNextXmitChannel();
			if( nextXmitChannel != null ){
				nextXmitChannel.handleXmit();
			}
			else {
				// nothing more to do, go out
				break;
			}
		}
		// write out trace info
		if( trc != null ){
			trc.printf( "WIRE_TX: {}%n");
			Utils.printTraceHexData(trc, buf, trcStartPos, buf.position());
		}
		return false; // flushing not implemented
	}
	/**
	 * Put on the buffer the needed chabu protocol informations: chabu version, byte order, payloadsize, channel count
	 * 
	 * These values must be set previous to infoLocal
	 * 
	 * @param buf
	 */
	private void processXmitSetup(){

		if( xmitHeader.hasRemaining() ){
			throw new ChabuException("Cannot xmit SETUP, buffer is not empty");
		}
		
		byte[] anlBytes = infoLocal.applicationName.getBytes( StandardCharsets.UTF_8 );
		Utils.ensure( anlBytes.length <= 200, ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME, "SETUP the local application name must be less than 200 UTF8 bytes, but is %s bytes.", anlBytes.length );
		
		xmitHeader.clear();
		xmitHeader.putShort( (short)(PacketType.SETUP.headerSize + anlBytes.length) );
		xmitHeader.put     ( (byte ) PacketType.SETUP.id );
		xmitHeader.put     ( (byte ) PROTOCOL_VERSION );
		xmitHeader.putShort( (short) infoLocal.maxReceivePayloadSize       );
		xmitHeader.putInt  (         infoLocal.applicationVersion );
		xmitHeader.putShort( (short) anlBytes.length );
		xmitHeader.put     (         anlBytes );
		xmitHeader.flip();
		
		xmitStartupCompleted = XmitState.PREPARED;
		
	}

	private void prepareXmitAccept(){

		if( xmitHeader.hasRemaining() ){
			throw new ChabuException("Cannot xmit ACCEPT, buffer is not empty");
		}
		
		xmitHeader.clear();
		xmitHeader.putShort( (short)(PacketType.ACCEPT.headerSize) );
		xmitHeader.put     ( (byte ) PacketType.ACCEPT.id );
		xmitHeader.flip();
	}
	private void processXmitAbort(){
		
		
		byte[] msgBytes = xmitAbortMessage.getBytes( StandardCharsets.UTF_8 );
		Utils.ensure( msgBytes.length <= 200, ChabuErrorCode.PROTOCOL_ABORT_MSG_LENGTH, "Xmit Abort message, text must be less than 200 UTF8 bytes, but is %s", msgBytes.length );
		
		xmitHeader.clear();
		xmitHeader.putShort( (short)(PacketType.ABORT.headerSize + msgBytes.length) );
		xmitHeader.put     ( (byte ) PacketType.ABORT.id );
		xmitHeader.putInt  (         xmitAbortCode );
		xmitHeader.putShort( (short) msgBytes.length );
		xmitHeader.put     (         msgBytes );
		xmitHeader.flip();
		
		xmitAbortMessage = "";
		xmitAbortCode    = 0;
		xmitAbortPending = XmitState.PREPARED;
		
	}
	
	/** 
	 * Called by channel
	 */
	void processXmitArm( int channelId, int arm ){

		if( xmitHeader.hasRemaining() ){
			throw new ChabuException("Cannot xmit ACCEPT, buffer is not empty");
		}
		
		xmitHeader.clear();
		xmitHeader.putShort( (short)(PacketType.ARM.headerSize) );
		xmitHeader.put     ( (byte ) PacketType.ARM.id );
		xmitHeader.putShort( (short) channelId );
		xmitHeader.putInt  (         arm       );
		xmitHeader.flip();
		
	}
	
	/** 
	 * Called by channel
	 */
	int processXmitSeq( int channelId, int seq, Consumer<ByteBuffer> user ){
		
		if( xmitHeader.hasRemaining() ){
			throw new ChabuException("Cannot xmit SEQ, buffer is not empty");
		}
		
		xmitHeader.clear();
		int plPos = PacketType.SEQ.headerSize + 2;
		xmitHeader.position( plPos );
		
		user.accept( xmitHeader );
		
		int pls = xmitHeader.position() - plPos;
		
		if( pls > 0 ){
			xmitHeader.putShort( 0, (short)(PacketType.SEQ.headerSize+pls) );
			xmitHeader.put     ( 2, (byte ) PacketType.SEQ.id );
			xmitHeader.putShort( 3, (short) channelId );
			xmitHeader.putInt  ( 5,         seq       );
			xmitHeader.putShort( 7, (short) pls );
			xmitHeader.flip();
			return pls;
		}
		else {
			xmitHeader.clear().limit(0);
			return 0;
		}
	}


	private ChabuChannel calcNextXmitChannel() {
		synchronized(this){
			for( int prio = priorityCount-1; prio >= 0; prio-- ){
				int idxCandidate = -1;
				BitSet prioBitSet = xmitChannelRequest[prio];

				// search from last channel pos on
				if( xmitLastChannelIdx+1 < prioBitSet.size() ){
					idxCandidate = prioBitSet.nextSetBit(xmitLastChannelIdx+1);
				}

				// try from idx zero
				if( idxCandidate < 0 ){
					idxCandidate = prioBitSet.nextSetBit(0);
				}

				// if found, clear and use it
				if( idxCandidate >= 0 ){
					prioBitSet.clear(idxCandidate);
					xmitLastChannelIdx = idxCandidate;
					ChabuChannel channel = channels.get(idxCandidate);
					Utils.ensure( channel.getPriority() == prio, ChabuErrorCode.ASSERT, "Channels prio does not match the store prio." );
					return channel;
				}
			}
			return null;
		}
	}

//	private boolean calcNextRecvChannel() {
//		synchronized(this){
//			int idx = -1;
//			for( int i = priorityCount-1; i >= 0 && idx < 0; i-- ){
//				if( recvChannelIdx+1 < recvChannelRequest[i].size() ){
//					idx = recvChannelRequest[i].nextSetBit(recvChannelIdx);
//				}
//				if( idx < 0 ){
//					idx = recvChannelRequest[i].nextSetBit(0);
//				}
//				
//				if( idx >= 0 ){
//					recvChannelRequest[i].clear(idx);
//				}
//			}
//			if( idx >= 0 ){
//				recvChannelIdx = idx;
//				return true;
//			}
//			else {
//				return false;
//			}
//		}
//	}
	
	public void setNetwork(IChabuNetwork nw) {
		Utils.ensure( nw      != null, ChabuErrorCode.CONFIGURATION_NETWORK, "Network passed in is null" );
		Utils.ensure( this.nw == null, ChabuErrorCode.CONFIGURATION_NETWORK, "Network is already set" );
		this.nw = nw;
		nw.setChabu(this);
	}
	
	private String getRecvString(){
		
		int anl = recvHeader.getShort() & 0xFFFF;
		if( recvHeader.remaining() < anl ){
			throw new ChabuException("Chabu string length exceeds packet length" );
		}
			
		byte[] anlBytes = new byte[anl];
		recvHeader.get( anlBytes );
		return new String( anlBytes, StandardCharsets.UTF_8 );
	}
	@Override
	public void setTracePrinter(PrintWriter writer) {
		this.traceWriter = writer;
	}

	public PrintWriter getTraceWriter() {
		return traceWriter;
	}

	public void setConnectingValidator(IChabuConnectingValidator val) {
		Utils.ensure( val      != null, ChabuErrorCode.CONFIGURATION_VALIDATOR, "ConnectingValidator passed in is null" );
		Utils.ensure( this.val == null, ChabuErrorCode.CONFIGURATION_VALIDATOR, "ConnectingValidator is already set" );
		this.val = val;
	}
	
}
