package chabu.tester;

import java.nio.channels.SocketChannel;

public class SelectorRegisterEntry {
	
	public final SocketChannel socketChannel;
	public final int interestOps;
	public final Object attachment;
	
	public SelectorRegisterEntry( SocketChannel socketChannel, int interestOps, Object attachment){
		this.socketChannel = socketChannel;
		this.interestOps   = interestOps;
		this.attachment    = attachment;
		
	}
}