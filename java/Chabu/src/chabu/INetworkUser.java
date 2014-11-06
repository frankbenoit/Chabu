package chabu;

import java.nio.ByteBuffer;

public interface INetworkUser {

	public void setNetwork( INetwork nw );
	
	/**
	 * Event to receive data from the network.
	 * @param bufferToConsume
	 */
	public void evRecv( ByteBuffer bufferToConsume );
	
	/**
	 * Event to pass data to the network.
	 * @param bufferToFill
	 */
	public boolean evXmit( ByteBuffer bufferToFill );
	
}
