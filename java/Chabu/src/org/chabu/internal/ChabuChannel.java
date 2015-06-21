/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit.
 * 
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 * 
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
package org.chabu.internal;

import java.io.PrintWriter;
import java.nio.ByteBuffer;

import org.chabu.ChabuErrorCode;
import org.chabu.IChabuChannel;
import org.chabu.IChabuChannelUser;



public final class ChabuChannel implements IChabuChannel {

//	private static final int PACKETFLAG_ARM = 0x0001;
//	private static final int PACKETFLAG_SEQ = 0x0002;
//	private static final int Chabu_HEADER_SEQ_SIZE = 10;
//	private static final int Chabu_HEADER_SIZE_MAX = 10;
//	private static final int Chabu_HEADER_ARM_SIZE = 8;

	private int     channelId    = -1;
	
	private Chabu        chabu;
	private final IChabuChannelUser user;
	
//	private ByteBuffer xmitBuffer;
	private int        xmitSeq = 0;
	private int        xmitArm = 0;
	
//	private ByteBuffer xmitHeader = ByteBuffer.allocate(10);
//	private int        xmitLastIndex = 0;
//	private int        xmitLastLength = 0;
	
	private boolean    recvArmShouldBeXmit  = false;
	
	private final ByteBuffer recvBuffer;
	private int        recvSeq = 0;
	private int        recvArm = 0;
	
	private int        priority = 0;
	
	public ChabuChannel(int recvBufferSize, int priority, IChabuChannelUser user ) {
		
		Utils.ensure( recvBufferSize > 0, ChabuErrorCode.CONFIGURATION_CH_RECVSZ, "recvBufferSize must be > 0, but is %s", recvBufferSize );
		Utils.ensure( priority >= 0, ChabuErrorCode.CONFIGURATION_CH_PRIO, "priority must be >= 0, but is %s", priority );
		Utils.ensure( user != null, ChabuErrorCode.CONFIGURATION_CH_USER, "IChabuChannelUser must be non null" );

		this.recvBuffer  = ByteBuffer.allocate(recvBufferSize);
		this.recvArm = recvBufferSize;
		this.recvArmShouldBeXmit = true;

		this.priority = priority;

		this.user = user;
		
	}
	
	void activate(Chabu chabu, int channelId ){

//		this.xmitHeader.order( ByteOrder.BIG_ENDIAN );
		
		this.chabu      = chabu;
		this.channelId  = channelId;

		user.setChannel(this);
		chabu.channelXmitRequestArm(channelId);
		
	}
	
	public void evUserXmitRequest(){
		chabu.channelXmitRequestData(channelId);
	}

//	void handleRecv( ByteBuffer buf ) {
//
//		callUserToTakeRecvData();
//
//		if( buf == null ){
//			return;
//		}
//		
//		if( buf.remaining() < 8 ){
//			return;
//		}
//		int pkf = buf.getShort(buf.position()+2) & 0xFFFF;
//		if( pkf == PACKETFLAG_ARM ){
//			handleRecvArm( buf.getInt(buf.position()+4) );
//			buf.position( buf.position() + 8 );
//			return;
//		}
//		else if( pkf == PACKETFLAG_SEQ ){
//			
//			if( buf.remaining() < 10 ){
//				return;
//			}
//			int seq = buf.getInt  (buf.position()+4);
//			int pls = buf.getShort(buf.position()+8) & 0xFFFF;
//			
//			Utils.ensure( this.recvSeq == seq );
//			
//			if( buf.remaining() < 10+pls ){
//				return; // packet not yet complete
//			}
//			if( recvBuffer.remaining() < pls ){
//				return; // cannot take all payload, yet
//			}
//			
//			buf.position( buf.position() + 10 );
//			
//			if( pls > 0 ){
//				int oldLimit = buf.limit();
//				buf.limit( buf.position() + pls );
//				recvBuffer.put( buf );
//				buf.limit( oldLimit );
//				this.recvSeq += pls;
//				callUserToTakeRecvData();
//			}
//			return;
//		}
//		else{
//			throw Utils.fail("Unknown PKF %04X", pkf );
//		}
//	}

	@Override
	public void evUserRecvRequest() {
		callUserToTakeRecvData();
	}

	private void callUserToTakeRecvData() {
		synchronized(this){
			recvBuffer.flip();
			int avail = recvBuffer.remaining();
			
			// prepare trace
			PrintWriter trc = chabu.getTraceWriter();
			int trcStartPos = recvBuffer.position();
			
			user.evRecv( recvBuffer );
			
			int consumed = avail - recvBuffer.remaining();
			
			// write out trace info
			if( trc != null && consumed > 0 ){
				trc.printf( "CHANNEL_TO_APPL: { \"ID\" : %s }%n", channelId );
				Utils.printTraceHexData(trc, recvBuffer, trcStartPos, recvBuffer.position());
			}
			
			recvBuffer.compact();
			if( consumed > 0 ){
				this.recvArm += consumed;
				this.recvArmShouldBeXmit = true;
				chabu.channelXmitRequestArm(channelId);
			}
		}
	}

	void handleRecvSeq(int seq, ByteBuffer buf, int pls ) {
		
		Utils.ensure( buf.remaining() <= recvBuffer.remaining(), ChabuErrorCode.PROTOCOL_CHANNEL_RECV_OVERFLOW, "Channel[%s] received more data (%s) as it can take (%s). Violation of the ARM value.", channelId, buf.remaining(), recvBuffer.remaining() );
		
		int taken = Utils.transferUpTo( buf, recvBuffer, pls );
		
		Utils.ensure( taken == pls, ChabuErrorCode.PROTOCOL_CHANNEL_RECV_OVERFLOW, "Channel[%s] received more data (%s) as it can take (%s). Violation of the ARM value.", channelId, buf.remaining(), recvBuffer.remaining() );
		recvBuffer.put( buf );
		this.recvSeq += pls;
		
		int align = pls;
		while( (align&3) != 0 ){
			align++;
			buf.get();
		}
		
		callUserToTakeRecvData();
	}
	
	/**
	 * Receive the ARM from the partner.
	 * @param arm
	 */
	void handleRecvArm(int arm) {
		if( this.xmitArm != arm && this.xmitArm == this.xmitSeq ){
			// was blocked by receiver
			// now the arm is updated
			// --> try to send new data
			chabu.channelXmitRequestData(channelId);
		}
		this.xmitArm = arm;
	}

	void handleXmitArm() {
		
		if( !recvArmShouldBeXmit ) {
			System.err.println("handleXmitArm()");
		}
		
		recvArmShouldBeXmit = false;
		chabu.processXmitArm(channelId, recvArm);
		chabu.channelXmitRequestData(channelId);

	}
	void handleXmitData() {
		chabu.processXmitSeq( channelId, xmitSeq, this::callUserToGiveXmit );
	}
	void handleXmit() {
		

		
////		if( !buf.hasRemaining() ){
////			return ( xmitLastLength > 0 && xmitLastIndex < xmitLastLength );
////		}
//		
//
//
//		// get/fill xmit data from user
//		// xmitBuffer in filling mode
//		if( xmitBuffer.hasRemaining() ){
//			callUserToGiveXmit();
//		}
//
//		if( xmitLastIndex == 0 && xmitLastLength == 0 ){
//			// start new header
//			
//			if( recvArmShouldBeXmit ){
//				recvArmShouldBeXmit = false;
//				
//				xmitHeader.clear();
//				
//				xmitHeader.putShort( (short)channelId );      // CID
//				xmitHeader.putShort( (short)PACKETFLAG_ARM ); // PKF
//				xmitHeader.putInt( recvArm );                 // ARM
//				xmitHeader.flip();
//				xmitLastLength = Chabu_HEADER_ARM_SIZE;
//			}
//			else {
//				int pls = xmitBuffer.position();
//				Utils.ensure( pls >= 0 && pls <= 0xFFFF );
//				if( pls > org.chabu.getMaxXmitPayloadSize() ){
//					pls = org.chabu.getMaxXmitPayloadSize();
//				}
//				int remainArm = xmitArm - xmitSeq;
//				if( pls > remainArm ){
//					pls = remainArm;
//				}
//
//				Utils.ensure( pls >= 0 ); // negative shall not occur
//				if( pls > 0 ){
//
//					xmitHeader.clear();
//					
//					xmitHeader.putShort( (short)channelId );
//					xmitHeader.putShort( (short)PACKETFLAG_SEQ );
//					xmitHeader.putInt( xmitSeq );
//					xmitHeader.putShort( (short)pls );
//					xmitLastLength = pls + Chabu_HEADER_SEQ_SIZE;
//					xmitSeq += pls;
//					
//					xmitHeader.flip();
//				}
//
//			}
//			
//		}
//		
//
//		if( xmitLastLength != 0 ){
//
//			// block header
//			if(( xmitLastIndex < Chabu_HEADER_SIZE_MAX ) && ( xmitLastIndex < xmitLastLength ) && buf.hasRemaining()){
//
//				int copySz = xmitLastLength - xmitLastIndex;
//				if( copySz > Chabu_HEADER_SIZE_MAX ){
//					copySz = Chabu_HEADER_SIZE_MAX;
//				}
//				if( copySz > buf.remaining() ){
//					copySz = buf.remaining();
//				}
//
//				Utils.ensure( copySz >= 0 );
//
//				int hdrLimit = xmitHeader.limit();
//				xmitHeader.limit( xmitHeader.position() + copySz );
//
//				int oldPos = buf.position();
//				buf.put( xmitHeader );
//				Utils.ensure( oldPos + copySz == buf.position() );
//				xmitHeader.limit(hdrLimit);
//				xmitLastIndex += copySz;
//
//			}
//
//			// block payload
//			if(( xmitLastIndex >= Chabu_HEADER_SIZE_MAX ) && ( xmitLastIndex < xmitLastLength ) && buf.hasRemaining() ){
//
//				int copySz = xmitLastLength - xmitLastIndex;
//				if( copySz > buf.remaining() ){
//					copySz = buf.remaining();
//				}
//
//				// xmitBuffer to consume/xmit mode
//				xmitBuffer.flip();
//				
//				Utils.ensure(( copySz >= 0 ) && ( copySz <= buf.remaining() ) && ( copySz <= xmitBuffer.remaining() ), "copySz:%s, buf.remaining():%s, xmitBuffer.remaining():%s", copySz, buf.remaining(), xmitBuffer.remaining());
//				
//				int xmitLimit = xmitBuffer.limit();
//				xmitBuffer.limit( xmitBuffer.position() + copySz );
//				int oldPos = buf.position();
//				buf.put( xmitBuffer );
//				Utils.ensure( oldPos + copySz == buf.position(), "oldPos:%s copySz:%s oldPos+copySz:%s buf.position():%s", oldPos, copySz, oldPos + copySz, buf.position() );
//				xmitBuffer.limit(xmitLimit);
//
//				xmitLastIndex += copySz;
//
//				// xmitBuffer to back to filling mode
//				xmitBuffer.compact();
//				
//				Utils.ensure( xmitBuffer.position() >= (xmitLastLength - xmitLastIndex), "xmitBuffer.position():%s >= (xmitLastLength:%s - xmitLastIndex:%s)", xmitBuffer.position(), xmitLastLength, xmitLastIndex );
//			}
//			
//
//			// block completed
//			if( xmitLastIndex >= xmitLastLength ){
//				Utils.ensure( xmitLastIndex == xmitLastLength );
//				xmitLastIndex  = 0;
//				xmitLastLength = 0;
////				userCallback( userData, channel, Chabu_Channel_Event_Transmitted );
//			}
//		}
//
//		// get/fill xmit data from user
//		// xmitBuffer in filling mode
//		if( xmitBuffer.hasRemaining() ){
//			callUserToGiveXmit();
//		}
//		//Check if data is still available
//		if( xmitBuffer.position() > 0 ){
//			//register next xmit on same channel.
//			org.chabu.evUserXmitRequest(channelId);
//		}
//		
//		// true if there is outstanding data for the current block
//		return ( xmitLastLength > 0 ) && ( xmitLastIndex < xmitLastLength );
	}

	private void callUserToGiveXmit(ByteBuffer buf) {
		PrintWriter trc = chabu.getTraceWriter();
		int startPos = buf.position();
		
		user.evXmit(buf);
		
		int added = buf.position() - startPos;
		this.xmitSeq += added;
		
		// write out trace info
		if( trc != null && buf.position() != startPos ){
			trc.printf( "APPL_TO_CHANNEL: { \"ID\" : %s }%n", channelId );
			Utils.printTraceHexData(trc, buf, startPos, buf.position());
		}


	}
	
	public String toString(){
		return String.format("Channel[%s recvS:%s recvA:%s xmitS:%s xmitA:%s]", channelId, this.recvSeq, this.recvArm, this.xmitSeq, this.xmitArm );
	}

	int getId() {
		return channelId;
	}

	int getPriority() {
		return priority;
	}

	@Override
	public IChabuChannelUser getUser() {
		return user;
	}

}
