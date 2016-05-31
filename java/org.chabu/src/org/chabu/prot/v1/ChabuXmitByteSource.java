package org.chabu.prot.v1;

import java.nio.ByteBuffer;

public interface ChabuXmitByteSource {
	
	/**
	 * Ask for a ByteBuffer with at least the given amount of bytes remaining.
	 * Depending on how the interface is used, the buffer might be used for receiving or for transmitting.
	 * The caller must call {@link #xmitCompleted()} when done with the buffer. 
	 * @param size
	 * @return
	 */
	ByteBuffer getXmitBuffer( int size );
	
	void xmitCompleted();
	
	void xmitReset();

	void setChannel( ChabuChannel channel );
	
}