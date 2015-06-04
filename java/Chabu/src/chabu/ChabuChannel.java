package chabu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Example sequence to use:
 * 
 * 1. create new ChabuChannel(name)
 * 2. initByteBuffer()
 * 3. setUser()
 * 4. clearBuffer()
 * 
 */
public class ChabuChannel{
	public ByteBuffer rsBuffer;
	public ByteBuffer rqBuffer;
	public Channel channel;
	public String name;
	private INetworkUser user;
	
	public ChabuChannel(String name){
		//default length
		this(10000, 10000, name);
	}
	public ChabuChannel(int recvBufferSize, int xmitBufferSize, String name){				
		channel = new Channel( recvBufferSize, xmitBufferSize );
		this.name = name;
	}
	/**
	 * 
	 * @param prior 0 is the lowest prior
	 * @param chabu TODO
	 */
	public void setUser(INetworkUser user, int prior, Chabu chabu){
		this.user = user;
		channel.setNetworkUser( this.user );
		this.user.setNetwork( channel );
		channel.setPriority(prior);
		chabu.addChannel( channel );
	}
	/**
	 * 
	 */
	public void clearBuffer() {
		rqBuffer.clear();
		rsBuffer.clear().limit(0);
	}
	
	public void initByteBuffer(int capacity_rq, int capacity_rs){
		
		rsBuffer = ByteBuffer.allocate(capacity_rs);
		rsBuffer.order(ByteOrder.LITTLE_ENDIAN);

		rqBuffer = ByteBuffer.allocate(capacity_rq);
		rqBuffer.order(ByteOrder.LITTLE_ENDIAN);
	}
}