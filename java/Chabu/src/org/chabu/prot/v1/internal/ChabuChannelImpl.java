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
package org.chabu.prot.v1.internal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import org.chabu.prot.v1.ChabuChannel;
import org.chabu.prot.v1.ChabuErrorCode;
import org.chabu.prot.v1.RecvByteTarget;
import org.chabu.prot.v1.XmitByteSource;


/**
 *
 * @author Frank Benoit
 */
public class ChabuChannelImpl implements ChabuChannel {

	private int     channelId    = -1;
	
	private ChabuImpl                   chabu;
	private final RecvByteTarget recvTarget;
	private final XmitByteSource xmitSource;
	
	private int        xmitSeq = 0;
	private int        xmitArm = 0;
	
	private boolean    recvArmShouldBeXmit  = false;
	
	private int        recvSeq = 0;
	private int        recvArm = 0;
	
	private int        priority = 0;

	private ByteBuffer recvTargetBuffer;

	private long xmitLimit;
	private long xmitPosition;
	private long recvLimit;
	private long recvPosition;

	
	public ChabuChannelImpl(int recvBufferSize, int priority, RecvByteTarget recvTarget, XmitByteSource xmitSource ) {
		
		this.recvTarget = recvTarget;
		this.xmitSource = xmitSource;
		Utils.ensure( recvBufferSize > 0, ChabuErrorCode.CONFIGURATION_CH_RECVSZ, "recvBufferSize must be > 0, but is %s", recvBufferSize );
		Utils.ensure( priority >= 0, ChabuErrorCode.CONFIGURATION_CH_PRIO, "priority must be >= 0, but is %s", priority );
		Utils.ensure( recvTarget != null, ChabuErrorCode.CONFIGURATION_CH_USER, "IChabuChannelUser must be non null" );
		Utils.ensure( xmitSource != null, ChabuErrorCode.CONFIGURATION_CH_USER, "IChabuChannelUser must be non null" );

		this.recvArm = recvBufferSize;
		this.recvArmShouldBeXmit = true;

		this.priority = priority;
	}
	
	void activate(ChabuImpl chabu, int channelId ){

		this.chabu      = chabu;
		this.channelId  = channelId;

		chabu.channelXmitRequestArm(channelId);
		
	}
	
	void verifySeq(int packetSeq ) {
		Utils.ensure( this.recvSeq == packetSeq, ChabuErrorCode.PROTOCOL_DATA_OVERFLOW, "Channel[%s] received more seq (%s) but expected (%s). Violation of the SEQ value.", channelId, this.recvSeq, packetSeq );
	}
	
	int handleRecvSeq(ByteChannel byteChannel, int recvByteCount ) throws IOException {
		
		int allowedRecv = this.recvArm - this.recvSeq;
		int remainingBytes = recvByteCount;
		Utils.ensure( remainingBytes <= allowedRecv, ChabuErrorCode.PROTOCOL_DATA_OVERFLOW, 
				"Channel[%s] received more data (%s) as it can take (%s). Violation of the ARM value.", channelId, remainingBytes, allowedRecv );
			
		if( recvTargetBuffer == null ){
			
			recvTargetBuffer = recvTarget.getRecvBuffer(remainingBytes);
			
			Utils.ensure( recvTargetBuffer != null, ChabuErrorCode.ASSERT, 
					"Channel[%s] recvTargetBuffer is null.", channelId );
			
			Utils.ensure( recvTargetBuffer.remaining() <= remainingBytes, ChabuErrorCode.ASSERT, 
					"Channel[%s] recvTargetBuffer has more remaining (%d) as requested (%d).", 
					channelId, recvTargetBuffer.remaining(), remainingBytes );
			
			Utils.ensure( recvTargetBuffer.remaining() > 0, ChabuErrorCode.ASSERT, 
					"Channel[%s] recvTargetBuffer cannot take data.", 
					channelId );
		}
		
		int summedReadBytes = 0;
		while( remainingBytes > 0 ){
	
			int readBytes = byteChannel.read(recvTargetBuffer);
			summedReadBytes += readBytes;
			remainingBytes -= readBytes;
			
			if( !recvTargetBuffer.hasRemaining() ){
				recvTargetBuffer = null;
				recvTarget.recvCompleted();
				recvSeq += recvByteCount;
				recvPosition += recvByteCount;
			}
			
			if( readBytes == 0 ){
				// could not read => try next time
				break;
			}
		}	
		return summedReadBytes;
	}
	
	/**
	 * Receive the ARM from the partner. This may make the channel to prepare new data to send.
	 * @param arm the value to update this.xmitArm to.
	 */
	void handleRecvArm(int arm) {
		if( this.xmitArm != arm ){
			// was blocked by receiver
			// now the arm is updated
			// --> try to send new data
			chabu.channelXmitRequestData(channelId);
		}
		this.xmitArm = arm;
	}

	public void handleXmitCtrl(ChabuXmitter xmitter, ByteBuffer xmitBuf) {
		if( recvArmShouldBeXmit ) {
			recvArmShouldBeXmit = false;
			xmitter.processXmitArm(channelId, recvArm );
		}
	}

	public ByteBuffer handleXmitData(ChabuXmitter xmitter, ByteBuffer xmitBuf, int maxSize) {
		int davail = getXmitRemaining();
		if( davail == 0 ){
			System.out.println("ChabuChannelImpl.handleXmitData() : called by no data available");
			return null;
		}
		int pls = Math.min( davail, maxSize );
	
		ByteBuffer seqBuffer = xmitSource.getXmitBuffer(pls);
		if( seqBuffer.hasRemaining() ){
			int realPls = seqBuffer.remaining();
			
			Utils.ensure( realPls > 0 && realPls <= pls, ChabuErrorCode.ASSERT, "" );
			
			xmitter.processXmitSeq(channelId, xmitSeq, realPls );
			xmitSeq += realPls;
			xmitPosition += realPls;
		}
		return seqBuffer;
	}

	public String toString(){
		return String.format("Channel[%s recvS:%s recvA:%s xmitS:%s xmitA:%s]", channelId, this.recvSeq, this.recvArm, this.xmitSeq, this.xmitArm );
	}

	@Override
	public int getChannelId() {
		return channelId;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public void setXmitLimit(long xmitLimit) {
		int added = Utils.safePosInt( xmitLimit - this.xmitLimit );
		addXmitLimit(added);
	}

	@Override
	public long getXmitLimit() {
		return xmitLimit;
	}

	@Override
	public long addXmitLimit(int added) {
		this.xmitLimit += added;
		chabu.channelXmitRequestData(channelId);
		return xmitLimit;
	}

	@Override
	public int getXmitRemaining() {
		return Utils.safePosInt( xmitLimit - xmitPosition );
	}

	@Override
	public long getXmitPosition() {
		return xmitPosition;
	}

	/**
	 * Called from Chabu, when the SEQ packet was transmitted.
	 */
	public void seqPacketCompleted() {
		xmitSource.xmitCompleted();
	}

	@Override
	public void setRecvLimit(long recvLimit) {
		int added = Utils.safePosInt( recvLimit - this.recvLimit );
		addRecvLimit(added);
	}

	@Override
	public long addRecvLimit(int added) {
		recvLimit += added;
		recvArm += added;
		recvArmShouldBeXmit = true;
		chabu.recvArmShallBeXmitted(this);
		return recvLimit;
	}

	@Override
	public long getRecvLimit() {
		return recvLimit;
	}

	@Override
	public long getRecvPosition() {
		return recvPosition;
	}

	@Override
	public long getRecvRemaining() {
		return Utils.safePosInt(recvLimit - recvPosition);
	}


}
