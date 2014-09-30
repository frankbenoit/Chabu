package mctcp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

public class Channel {

	/**
	 * Add first, removeLast
	 */
	private final LinkedList<Block> rxBlocks = new LinkedList<>();
	private final int   rxBlocksCount;
//	private int   rxBlocksSeq    = 1;
//	private int   rxBlocksArm    = 0;

	/**
	 * Add first, removeLast
	 */
	private final LinkedList<Block> txBlocks = new LinkedList<>();
	private Block txBlockCompose;
	private final int   txBlocksCount;
	private int   txBlocksSeq    = 1;
	private int   txBlocksArm    = 0;
	private boolean txReqPending = false;

	private INetworkConnector networkConnector;
	private final int id;
	private final ByteOrder byteOrder;
	
	public Channel( ByteOrder bo, int channelId, int rxBlocksCount, int txBlocksCount ){
		this.byteOrder     = bo;
		this.id            = channelId;
		this.rxBlocksCount = rxBlocksCount;
		this.txBlocksCount = txBlocksCount;
	}

	public int getId() {
		return id;
	}

	void setNetworkConnector(INetworkConnector networkConnector) {
		this.networkConnector = networkConnector;

		Block tx = new Block( byteOrder );
		tx.txReset();
		tx.setChannel( id            );
		tx.setSeq    ( txBlocksSeq++ );
		tx.setArm    ( rxBlocksCount );
		tx.txComposeComplete();
		txReqPending = true;
		this.networkConnector.channelReadyToSend(this);
	}
	
	// ------------------------------------------------------------------------------------
	// --- RX ----

	public ByteBuffer rxGetBuffer() {
		Block block = rxBlocks.getLast();
		if( block == null ) return null;
		return block.getPayload();
	}
	public void rxConsumedBuffer() {
		// remove buffer from list
		Block consumed = rxBlocks.removeLast();
		int txArm = consumed.getSeq() + rxBlocksCount;
		if( txArm > 0xFFFF ){
			txArm -= 0xFFFE; // jump over 0x0000
		}
		// send next ARM notice
		if( txBlocks.isEmpty() ){
			// nothing else to send, so create extra ARM notify block
			Block block = new Block(byteOrder);
			block.txReset();
			block.setChannel( id );
			block.setArm( txArm );
			block.txComposeComplete();
			txBlocks.addFirst(block);
		}
		else {
			// next block to be sent will have the latest arm value
			Block block = txBlocks.getLast();
			block.setArm(txArm);
		}
		if( !txReqPending ){
			txReqPending = true;
			networkConnector.channelReadyToSend(this);
		}
	}

	/**
	 * put a block received by the protocol
	 */
	void rxPutBlock(Block rxBlock) {
		Utils.ensure( rxBlock.rxCanConsume(), "Block not ready for rx consume");
		Utils.ensure( rxBlock.getChannel() == id, "Block not for this channel");
		Utils.ensure( rxBlocks.size() >= rxBlocksCount, "Too many RX Blocks");
		rxBlocks.addFirst( rxBlock );
		int arm = rxBlock.getArm();
		if( arm != 0 ){
			txBlocksArm = arm;
		}
		txCheckToSend();
	}

	private void txCheckToSend() {
		if( !txReqPending && !txBlocks.isEmpty() ){
			int diff = ( txBlocksArm - txBlocks.getLast().getSeq() ) & 0xFFFF;
			if( diff > 0x8000 ){
				diff -= 0xFFFF;
			}
			if( diff > 0 ){
				txReqPending = true;
				networkConnector.channelReadyToSend(this);
			}
		}
	}

	// ------------------------------------------------------------------------------------
	// --- TX ----
	
	public ByteBuffer txGetBuffer() {
		if( txBlockCompose == null ){
			txBlockCompose = new Block(byteOrder);
			txBlockCompose.txReset();
		}
		return txBlockCompose.getPayload();
	}
	public void txCommitBuffer() {
		Utils.ensure( txBlockCompose != null , "no active tx compose block" );
		// add buffer to list
		
		txBlockCompose.setSeq(txBlocksSeq++);
		txBlockCompose.setChannel(id);
		txBlockCompose.txComposeComplete();
		
		txBlocks.addFirst(txBlockCompose);
		
		txBlockCompose = null;
		txCheckToSend();
	}

	Block txPopBlock() {
		Block res = txBlocks.removeLast();

		txReqPending = false;			
		txCheckToSend();
		
		return res;
	}

}
