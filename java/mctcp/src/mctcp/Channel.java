package mctcp;

import java.nio.ByteBuffer;
import java.util.ArrayDeque;



public final class Channel implements IChannel {

	private int   channelId;
	private McTcp mcTcp;
	private final IChannelUser user;
	private final Object       attachment;
	
	final int rxBlocks; // used in mctcp
	
	private ByteBuffer xmitBuffer;
	private int        xmitSeq = 0;
	private int        xmitArm = 0xFFFF;
	private boolean    xmitDataValid;
	private boolean    xmitArmValid;
	
	private ArrayDeque<ByteBuffer> recvBuffers;
	private int                    recvSeq = 0xFFFF;
	private int                    recvArm = 0xFFFF;
	
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
		
	}

	void activate(McTcp mcTcp, int channelId ){
		this.mcTcp      = mcTcp;
		this.channelId  = channelId;
		this.xmitBuffer = ByteBuffer.allocate(mcTcp.maxPayloadSize);
		this.xmitBuffer.order( mcTcp.byteOrder );
	}
	public void evUserReadRequest(){
		Utils.ensure( false, "Not implemented" );
	}
	public void evUserWriteRequest(){
		mcTcp.evUserWriteRequest(channelId);
	}

	void handleRecv( Block block ) {
		System.out.println("Channel.handleRecv()");
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
				mcTcp.freeBlock( userBuf );
				this.recvArm = ( this.recvArm + 1 ) & 0xFFFF;
				this.xmitArmValid = true;

				// check for more recv data to transfer to user
				userBuf = this.recvBuffers.peek();
				oldRem = -1;
			}
		}
		if( xmitAllowed() || xmitInterest ){
			System.out.println("Channel.handleRecv() -> WriteRequest");
			mcTcp.evUserWriteRequest(channelId);
		}
	}
	void handleXmit( Block block ) {
		System.out.println("Channel.handleXmit()");
		if( !xmitDataValid ){
			checkUserXmitData();
		}

		if( xmitAllowed() ){
			xmitData(block);
		}
		
		if( !xmitDataValid ){
			checkUserXmitData();
			if( xmitAllowed() ){
				mcTcp.evUserWriteRequest(channelId);
			}
		}
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
//		System.out.printf("Channel.xmitData check %d %d %d\n", xmitArm, xmitSeq, sz);
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
}
