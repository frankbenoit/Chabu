package chabu;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;



public final class Channel implements IChannel {

	private int   channelId = -1;
	private Chabu chabu;
	private final IChannelUser user;
	private final Object       attachment;
	
	final int rxBlocks; // used in chabu
	
	private ByteBuffer xmitBuffer;
	private int        xmitSeq = 0;
	private int        xmitArm = 0xFFFF;
	private boolean    xmitDataValid;
	private boolean    xmitArmValid;
	
	private ArrayDeque<ByteBuffer> recvBuffers;
	private int                    recvSeq = 0xFFFF;
	private int                    recvArm = 0xFFFF;
	private String instanceName;
	
	public Channel(int rxBlocks, IChannelUser user ) {
		this( rxBlocks, user, null );
	}

	public Channel(int rxBlocks, IChannelUser user, Object attachment ) {
		this.rxBlocks    = rxBlocks;
		this.user        = user;
		this.attachment  = attachment;
		this.recvBuffers     = new ArrayDeque<>(rxBlocks);
		
		this.recvArm = ( this.recvArm + rxBlocks ) & 0xFFFF;
		xmitArmValid = true;
		instanceName = "ChabuChannel[<not yet active>]";
	}

	void activate(Chabu chabu, int channelId ){
		this.chabu      = chabu;
		this.channelId  = channelId;
		this.xmitBuffer = ByteBuffer.allocate(chabu.maxPayloadSize);
		this.xmitBuffer.order( chabu.byteOrder );
		instanceName = String.format("%s.ch%d", chabu.instanceName, channelId );
	}
	public void evUserReadRequest(){
		log("evUserReadRequest()");
		//Utils.ensure( false, "Not implemented" );
	}
	public void evUserWriteRequest(){
		log("evUserWriteRequest()");
		chabu.evUserWriteRequest(channelId);
	}

	void handleRecv( Block block ) {
		boolean xmitInterest = false;
		if( this.xmitArm != block.arm ){
			xmitInterest = true;
		}
		this.xmitArm = block.arm;
		if( block.payloadSize > 0 ){
			Utils.ensure( 1 == (short)( block.seq - this.recvSeq ), "Seq not inc 1: stored SEQ %d, recv SEQ %d", recvSeq, block.seq );
			Utils.ensure( (short)( this.recvArm - block.seq ) >= 0 );
			this.recvSeq = block.seq;
			recvBuffers.addLast( block.payload );
			block.payload = null;
		}
		
		// transfer data to user
		int oldRem = -1;
		ByteBuffer userBuf = recvBuffers.peek();
		while( userBuf != null && oldRem != userBuf.remaining() ){
			oldRem = userBuf.remaining();
			user.evRecv( userBuf, attachment );
			if( !userBuf.hasRemaining() ){
				
				// remove the buffer, now space for receive
				this.recvBuffers.remove();
				chabu.freeBlock( userBuf );
				this.recvArm = ( this.recvArm + 1 ) & 0xFFFF;
				this.xmitArmValid = true;

				// check for more recv data to transfer to user
				userBuf = this.recvBuffers.peek();
				oldRem = -1;
			}
		}
		if( xmitAllowed() || xmitInterest ){
			log("Channel.handleRecv() -> WriteRequest");
			chabu.evUserWriteRequest(channelId);
		}
		log( "handleRecv() %s", toString() );
	}
	private void log( String fmt, Object ... args ){
		ILogConsumer log = chabu.log;
		if( log != null ){
			log.log( ILogConsumer.Category.CHANNEL, instanceName, fmt, args );
		}
	}
	void handleXmit( Block block ) {
		if( !xmitDataValid ){
			checkUserXmitData();
		}

		if( xmitAllowed() ){
			xmitData(block);
		}
		
		if( !xmitDataValid ){
			checkUserXmitData();
			if( xmitAllowed() ){
				chabu.evUserWriteRequest(channelId);
			}
		}
		log( "handleXmit() %s", toString() );
	}

	private boolean xmitAllowed() {
		if( xmitArmValid ) return true;
		return xmitDataAllowed();
	}

	private boolean xmitDataAllowed() {
		if( !xmitDataValid ) return false;
		int diff = (short) ( xmitArm - xmitSeq );
		return diff >= 0;
	}

	private void checkUserXmitData() {
		boolean flush = user.evXmit( xmitBuffer, attachment);
		if( flush || xmitBuffer.position() > 0 ){
			xmitDataValid = true;
		}
	}

	private void xmitData(Block block) {
		
		block.channelId   = channelId;
		block.arm         = recvArm;
		block.valid       = true;
		
//	if( xmitDataValid && !xmitDataAllowed() ){
//		int sz = -1;
//		if( xmitBuffer != null ){
//			sz = xmitBuffer.position();
//		}
//		log("Channel.xmitData check %d %d %d\n", xmitArm, xmitSeq, sz);
//	}
		if( xmitDataAllowed() ){
			
			xmitBuffer.flip();
			
			block.payloadSize = xmitBuffer.remaining();
			block.seq         = xmitSeq;
			
			xmitSeq = ( xmitSeq + 1 ) & 0xFFFF;
			
			// swap the buffers
			ByteBuffer buf = block.payload;
			block.payload = xmitBuffer;
			xmitBuffer = buf;
			
			xmitBuffer.clear();
		}
		else {
			block.payloadSize = 0;
			block.seq         = ( xmitSeq -1 ) & 0xFFFF;
		}
		
		xmitArmValid  = false;
		xmitDataValid = false;
		
	}
	
	public String toString(){
		return String.format("Channel[%s %s %s %s %s]", channelId, this.recvSeq, this.recvArm, this.xmitSeq, this.xmitArm );
	}
	
}
