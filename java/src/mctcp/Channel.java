package mctcp;

public class Channel {

	private Block rxBlocksHead;
	private Block rxBlocksTail;
	private Block rxBlocksFree;
	private Block rxBlocksProg;
	private int   rxBlocksCount  = 0;
	private int   rxBlocksSeq    = 1;
	private int   rxBlocksArm    = 0;

	private Block txBlocksHead;
	private Block txBlocksTail;
	private Block txBlocksFree;
	private Block txBlocksProg;
	private int   txBlocksCount  = 0;
	private int   txBlocksSeq    = 1;
	private int   txBlocksArm    = 0;

	private final int id;
	private INetworkConnector networkConnector;
	
	public Channel( int channelId, int rxBlocks, int blockCountTx ){
		this.id = channelId;
		for (int i = 0; i < rxBlocks; i++) {
			Block prev = rxBlocksFree;
			rxBlocksFree = new Block(this);
			rxBlocksFree.nextBlock = prev;
		}
		for (int i = 0; i < blockCountTx; i++) {
			Block prev = txBlocksFree;
			txBlocksFree = new Block(this);
			txBlocksFree.nextBlock = prev;
		}
		Block tx = txBlocksFree;
		txBlocksFree = tx.nextBlock;
		tx.resetForTx();
		tx.setChannel( channelId     );
		tx.setSeq    ( txBlocksSeq++ );
		tx.setArm    ( rxBlocks      );
		tx.composeComplete();
		txBlocksProg = tx;
	}

	public int getId() {
		return id;
	}

	void setNetworkConnector(INetworkConnector networkConnector) {
		this.networkConnector = networkConnector;
	}
	
}
