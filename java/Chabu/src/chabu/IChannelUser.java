package chabu;

import java.nio.ByteBuffer;

public interface IChannelUser {
	
	void evRecv( ByteBuffer buffer, Object attachment );
	
	/**
	 * 
	 * @param buffer
	 * @param attachment
	 * @return true if flush
	 */
	boolean evXmit( ByteBuffer buffer, Object attachment );
	
}
