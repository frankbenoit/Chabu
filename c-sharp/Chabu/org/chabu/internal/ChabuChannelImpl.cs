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
namespace org.chabu.intern {

using System;
using org.chabu;
using org.chabu.container;
using ByteBuffer = System.IO.MemoryStream;
using PrintWriter = System.IO.TextWriter;

/**
 *
 * @author Frank Benoit
 */
public sealed class ChabuChannelImpl : ChabuChannel {

	private int     channelId    = -1;
	
	private ChabuImpl                   chabu;
	private readonly ChabuChannelUser user;
	
	private int        xmitSeq = 0;
	private int        xmitArm = 0;
	
	private bool    recvArmShouldBeXmit  = false;
	
	private readonly ByteQueue recvBuffer;
	private int        recvSeq = 0;
	private int        recvArm = 0;
	
	private int        priority = 0;
	
	public ChabuChannelImpl(int recvBufferSize, int priority, ChabuChannelUser user ) {
		
		Utils.ensure( recvBufferSize > 0, ChabuErrorCode.CONFIGURATION_CH_RECVSZ, "recvBufferSize must be > 0, but is {0}", recvBufferSize );
		Utils.ensure( priority >= 0, ChabuErrorCode.CONFIGURATION_CH_PRIO, "priority must be >= 0, but is {0}", priority );
		Utils.ensure( user != null, ChabuErrorCode.CONFIGURATION_CH_USER, "IChabuChannelUser must be non null" );

		this.recvBuffer  = ByteQueueBuilder.create( "ChabuChannel", recvBufferSize);
		this.recvArm = recvBufferSize;
		this.recvArmShouldBeXmit = true;

		this.priority = priority;

		this.user = user;
        
        callUserToGiveXmit = new ConsumerByteBufferImpl(this);
		
	}
	
	public void activate(ChabuImpl chabu, int channelId ){

		this.chabu      = chabu;
		this.channelId  = channelId;

		user.setChannel(this);
		chabu.channelXmitRequestArm(channelId);
		
	}
	
	//@Override
	public void xmitRegisterRequest(){
		chabu.channelXmitRequestData(channelId);
	}

	//@Override
	public void recvRegisterRequest() {
		callUserToTakeRecvData();
	}

	private void callUserToTakeRecvData() {
		int consumed = 0;
		lock(this){
			ByteQueueInputPort inport = recvBuffer.getInport();
			int avail = inport.free();
			
//			// prepare trace
//			PrintWriter trc = chabu.getTraceWriter();
//			int trcStartPos = recvBuffer.position();
			
			user.recvEvent( recvBuffer.getOutport() );
			
			consumed = inport.free() - avail;
			recvBuffer.getOutport().ensureCommitted();
			
//			// write out trace info
//			if( trc != null && consumed > 0 ){
//				trc.printf( "CHANNEL_TO_APPL: { \"ID\" : %s }%n", channelId );
//				Utils.printTraceHexData(trc, recvBuffer, trcStartPos, recvBuffer.position());
//			}
			
			this.recvArm += consumed;
		}
		if( consumed > 0 ){
			this.recvArmShouldBeXmit = true;
			chabu.channelXmitRequestArm(channelId);
		}
	}

	public void handleRecvSeq(int seq, ByteBuffer buf, int pls ) {
		
			int allowedRecv = this.recvArm - this.recvSeq;
			
			
			Utils.ensure( this.recvSeq == seq, ChabuErrorCode.PROTOCOL_DATA_OVERFLOW, "Channel[%s] received more seq (%s) but expected (%s). Violation of the SEQ value.", channelId, this.recvSeq, seq );
			Utils.ensure( pls <= allowedRecv, ChabuErrorCode.PROTOCOL_DATA_OVERFLOW, "Channel[%s] received more data (%s) as it can take (%s). Violation of the ARM value.", channelId, buf.remaining(), allowedRecv );
			
			ByteQueueInputPort qin = recvBuffer.getInport();
			Utils.ensure( buf.remaining() <= qin.free(), ChabuErrorCode.PROTOCOL_CHANNEL_RECV_OVERFLOW, 
					"Channel[%s] received more data (%s) as it can take (%s). Violation of the ARM value. (this.recvArm=0x%X, this.recvSeq=0x%X, seq=0x%X)", 
					channelId, buf.remaining(), qin.free(),
					this.recvArm, this.recvSeq, seq );
			
			
			lock(this){
				int taken = qin.write( buf, pls );
				qin.commit();
				//int taken = Utils.transferUpTo( buf, recvBuffer, pls );
				
				Utils.ensure( taken == pls, ChabuErrorCode.PROTOCOL_CHANNEL_RECV_OVERFLOW, 
						"Channel[%s] received more data (%s) as it can take (%s). Violation of the ARM value. %s %s", 
						channelId, buf.remaining(), qin.free(), taken, pls );
				//recvBuffer.put( buf );
				this.recvSeq += pls;
			
			}
			
			int align = pls;
			while( (align&3) != 0 ){
				align++;
				buf.get();
			}
			
		callUserToTakeRecvData();
		
	}
	
	/**
	 * Receive the ARM from the partner. This may make the channel to prepare new data to send.
	 * @param arm the value to update this.xmitArm to.
	 */
	public void handleRecvArm(int arm) {
		if( this.xmitArm != arm ){
			// was blocked by receiver
			// now the arm is updated
			// --> try to send new data
			chabu.channelXmitRequestData(channelId);
		}
		this.xmitArm = arm;
	}

	public void handleXmitArm() {
		
		if( !recvArmShouldBeXmit ) {
            Console.Error.WriteLine("handleXmitArm()");
		}
		
		recvArmShouldBeXmit = false;
		chabu.processXmitArm(channelId, recvArm);
		chabu.channelXmitRequestData(channelId);

	}
	public void handleXmitData() {
		int armSize = xmitArm - xmitSeq;
		Utils.ensure( armSize >= 0, ChabuErrorCode.ASSERT, "arm size must not be negative: {0} {1} {2}", xmitArm, xmitSeq, armSize );
		if( armSize > 0 ){
			chabu.processXmitSeq( channelId, xmitSeq, armSize, callUserToGiveXmit );
		}
	}

	private readonly ConsumerByteBufferImpl callUserToGiveXmit;

    class ConsumerByteBufferImpl : ConsumerByteBuffer {
        ChabuChannelImpl o;
        public ConsumerByteBufferImpl(ChabuChannelImpl o) { this.o = o; }
		public void accept(ByteBuffer buf) {
			PrintWriter trc = o.chabu.getTraceWriter();
			int startPos = buf.position();

			o.user.xmitEvent(buf);

			int added = buf.position() - startPos;
			o.xmitSeq += added;

			// write out trace info
			if( trc != null && buf.position() != startPos ){
                trc.WriteLine("APPL_TO_CHANNEL: { \"ID\" : {0} }", o.channelId);
				Utils.printTraceHexData(trc, buf, startPos, buf.position());
			}
		}
	};

	public String toString(){
		return String.Format("Channel[{0} recvS:{1} recvA:{2} xmitS:{3} xmitA:{4}]", channelId, this.recvSeq, this.recvArm, this.xmitSeq, this.xmitArm );
	}

	//@Override
	public int getChannelId() {
		return channelId;
	}

	//@Override
	public int getPriority() {
		return priority;
	}

	//@Override
	public ChabuChannelUser getUser() {
		return user;
	}

}
}
