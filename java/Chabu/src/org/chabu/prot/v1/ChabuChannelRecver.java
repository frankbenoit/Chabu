package org.chabu.prot.v1;

public interface ChabuChannelRecver {

	/**
	 * Set the limit value as absolute byte position, until were the application can receive data. This value is relative to the recv position.
	 * An increase has the side effect that the remote host might be notified about the bigger amount of data that can be sent.
	 */
	void setRecvLimit( long recvLimit );
	
	/**
	 * Add a positive value onto the current recv limit.
	 * An increase has the side effect that the remote host might be notified about the bigger amount of data that can be sent.
	 */
	long addRecvLimit( int added );
	
	long getRecvLimit();

	/**
	 * Amount of bytes that the application already received.
	 */
	long getRecvPosition();
	
	/**
	 * Amount of bytes that the application is able to receive.
	 */
	long getRecvRemaining();
}
