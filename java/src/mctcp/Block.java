package mctcp;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

class Block {

	enum State {
		/** to be filled by the protocol */
		RX_ING,
		/** to be read out by application */
		RX_CONSUMING, 
		/** to be sent by the protocol */
		TX_ING, 
		/** to be filled by application */
		TX_FILLING
	}
	
	Block prevBlock;
	Block nextBlock;

	private State state = State.RX_ING;
	private ByteBuffer buffer;
	private ByteBuffer bufferPayload;

	public static final int OFFSET_PAYLOAD_SZ   =  0;
	public static final int OFFSET_CHANNEL_ID   =  2;
	public static final int OFFSET_BLOCK_SEQ_ID =  4;
	public static final int OFFSET_BLOCK_ARM_ID =  6;
	public static final int OFFSET_PAYLOAD_DATA =  8;

	public static final int BLOCK_SIZE =  1200;
	
	public Block(){
		this.buffer = ByteBuffer.allocate(BLOCK_SIZE);
		this.buffer.order(Constants.BYTE_ORDER);
		this.bufferPayload = ByteBuffer.wrap( 
				buffer.array(), 
				buffer.arrayOffset() + OFFSET_PAYLOAD_DATA, 
				buffer.capacity() - OFFSET_PAYLOAD_DATA );
	}

	// ------------------------------------------------------------------------------------
	// --- RX ----

	void rxReset() {
		state = State.RX_ING;
		buffer.position(0);
		// receive header info
		buffer.limit(OFFSET_PAYLOAD_DATA);
	}

	boolean rxCanConsume(){
		return state == State.RX_CONSUMING;
	}
	
	ByteBuffer getPayload(){

		Utils.ensure( state == State.RX_CONSUMING, "Receive consuming in wrong state %s", state);

		return bufferPayload;
	}

	/**
	 * Write data from the protocol into this Block.
	 */
	void receiveContent( ReadableByteChannel c ) throws IOException {
		
		Utils.ensure( state == State.RX_ING, "Receiving in wrong state %s", state);
		
		// header receive?
		if( buffer.limit() == OFFSET_PAYLOAD_DATA && buffer.hasRemaining() ){
			c.read( buffer );
			// header complete? -> prepare payload
			if( !buffer.hasRemaining() ){
				buffer.limit( getPayloadSize() + OFFSET_PAYLOAD_DATA );
			}
		}
		if( buffer.hasRemaining() ){
			c.read( buffer );
		}
		if( buffer.limit() >= OFFSET_PAYLOAD_DATA && !buffer.hasRemaining() ){

			bufferPayload.position(0);
			bufferPayload.limit( getPayloadSize() );
			
			state = State.RX_CONSUMING;
		}
	}

	// ------------------------------------------------------------------------------------
	// --- TX ----
	public void txReset() {
		state = State.TX_FILLING;
		buffer.position(OFFSET_PAYLOAD_DATA);
		buffer.putLong( 0, 0L );
		buffer.limit(buffer.capacity());
		bufferPayload.position(0);
		bufferPayload.limit(bufferPayload.capacity());
	}
	
	/**
	 * Write data from this Block into a buffer from the protocol.
	 * 
	 * @return true if the block is copied completely into the channel.
	 */
	public boolean transmitContent( WritableByteChannel c ) throws IOException{

		Utils.ensure( state == State.TX_ING, "Transmit in wrong state %s", state);
		
		c.write( buffer );
		
		return !buffer.hasRemaining();
		
	}

	public void txComposeComplete() {
		state = State.TX_ING;
		buffer.putShort( OFFSET_PAYLOAD_SZ, (short)bufferPayload.position() );
		buffer.limit( OFFSET_PAYLOAD_DATA + bufferPayload.position() );
		buffer.position(0);
	}

	// ------------------------------------------------------------------------------------
	// --- Getter/Setter ----

	public int getSeq(){
		return buffer.getShort( OFFSET_BLOCK_SEQ_ID ) & 0xFF_FF;
	}
	
	public void setSeq(int seq) {
		buffer.putShort( OFFSET_BLOCK_SEQ_ID, (short)seq );
	}

	public int getArm(){
		return buffer.getShort( OFFSET_BLOCK_ARM_ID ) & 0xFF_FF;
	}
	
	public void setArm(int arm) {
		buffer.putShort( OFFSET_BLOCK_ARM_ID, (short)arm );
	}

	public int getChannel(){
		return buffer.getShort( OFFSET_CHANNEL_ID ) & 0xFF_FF;
	}
	
	public void setChannel(int channel) {
		buffer.putShort( OFFSET_CHANNEL_ID, (short)channel);
	}

	public int getPayloadSize(){
		return buffer.getShort( OFFSET_PAYLOAD_SZ ) & 0xFF_FF;
	}

}
