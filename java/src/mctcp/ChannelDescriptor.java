package mctcp;

public class ChannelDescriptor {
	
	private int countRx;
	private int countTx;

	public ChannelDescriptor( int countRx, int countTx ){
		this.countRx = countRx;
		this.countTx = countTx;
	}

	public int getCountRx() {
		return countRx;
	}
	public int getCountTx() {
		return countTx;
	}
}
