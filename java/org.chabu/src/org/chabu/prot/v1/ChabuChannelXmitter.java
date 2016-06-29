package org.chabu.prot.v1;

public interface ChabuChannelXmitter extends ChabuChannelBase {

	/**
	 * Set the index until which position chabu shall transmit data. 
	 * The value is only allowed to be increased. 
	 * If a value smaller as zero or smaller as a previously given value is given, a IndexOutofBoundsExcepiton will be thrown. 
	 * @param seqIndex
	 */
	void setXmitLimit( long xmitLimit );
	long getXmitLimit();
	long addXmitLimit( int added );
	
	long getXmitPosition();
	int getXmitRemaining();
	
}
