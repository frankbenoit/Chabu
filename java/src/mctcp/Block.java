package mctcp;

import java.nio.ByteBuffer;

public class Block {
	 
	Block prevBlock;
	Block nextBlock;

	private ByteBuffer buffer;
	private ByteBuffer bufferPayload;

	public static final int OFFSET_PAYLOAD_SZ   =  0;
	public static final int OFFSET_CHANNEL_ID   =  2;
	public static final int OFFSET_BLOCK_SEQ_ID =  4;
	public static final int OFFSET_BLOCK_ARM_ID =  6;
	public static final int OFFSET_PAYLOAD_DATA =  8;

	public static final int BLOCK_SIZE =  1200;
	private Channel channel;

	public Block(Channel channel){
		this.channel = channel;
		this.buffer = ByteBuffer.allocate(BLOCK_SIZE);
		 this.bufferPayload = ByteBuffer.wrap( 
				 buffer.array(), 
				 buffer.arrayOffset() + OFFSET_PAYLOAD_DATA, 
				 buffer.capacity() - OFFSET_PAYLOAD_DATA );
	 }

	 public int getMaxBlockSize(){
		 return buffer.capacity();
	 }
	 
	 public boolean isCompleted(){
		 if( buffer.limit() < OFFSET_PAYLOAD_DATA ){
			 return false;
		 }
		 return ( buffer.position() == buffer.limit() );
	 }
	 public boolean isHeaderCompleted(){
		 if( buffer.limit() < 24 ){
			 return false;
		 }
		 return ( buffer.position() >= 24 );
	 }
	 private void ensureCompleted(){
		 if( !isCompleted() ){
			 throw new RuntimeException("Block not completed");
		 }
	 }
	 
	 public int getChannel(){
		 return buffer.getShort( OFFSET_CHANNEL_ID ) & 0xFF_FF;
	 }
	 
	 public int getBlockSeqId(){
		 return buffer.getShort( OFFSET_BLOCK_SEQ_ID ) & 0xFF_FF;
	 }
	 
	 public int getBlockArmId(){
		 return buffer.getShort( OFFSET_BLOCK_ARM_ID ) & 0xFF_FF;
	 }
	 
	 public int getPayloadSize(){
		 return buffer.getShort( OFFSET_PAYLOAD_SZ ) & 0xFF_FF;
	 }
	 
	 public ByteBuffer getPayload(){
		 ensureCompleted();
		 bufferPayload.position(0);
		 bufferPayload.limit( getPayloadSize() );
		 return bufferPayload.slice();
	 }
	 
	 /**
	  * Write data from the protocol into this Block.
	  * 
	  * @param bb data from the protocol. bb is set to be ready for reading. 
	  * 	This means the data between position+limit is the data copied into the Block.
	  */
	 public void protocolPutContent( ByteBuffer bb ){
		 buffer.put( bb );
	 }

	 /**
	  * Write data from this Block into a buffer from the protocol.
	  * 
	  * @param bb data from the protocol. bb is set to be ready for writing. 
	  * 	This means the data between position+limit is the data copied from the Block.
	  */
	 public void protocolGetContent( ByteBuffer bb ){
		 
	 }

	 public void resetForTx() {
		 buffer.position(OFFSET_PAYLOAD_DATA);
		 buffer.limit(buffer.capacity());
	 }

	 public void setSeq(int seq) {
		 buffer.putShort( OFFSET_BLOCK_SEQ_ID, (short)seq );
	 }

	 public void setArm(int arm) {
		 buffer.putShort( OFFSET_BLOCK_ARM_ID, (short)arm );
	 }

	 public void setChannel(int channel) {
		 buffer.putShort( OFFSET_CHANNEL_ID, (short)channel);
	 }

	public void composeComplete() {
		buffer.putShort( OFFSET_PAYLOAD_SZ, (short)(buffer.position() - OFFSET_PAYLOAD_DATA) );
		buffer.flip();
	}
}
