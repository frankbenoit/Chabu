package org.chabu.nwtest.server;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

abstract class AConnection  {
	SocketChannel channel;
	private SelectionKey key;

	public AConnection(SocketChannel channel, SelectionKey key ) {
		this.channel = channel;
		this.key = key;
	}
	
	abstract void accept(SelectionKey t);
	
	public void registerWrite() throws ClosedChannelException {
		key.interestOps(SelectionKey.OP_READ|SelectionKey.OP_WRITE);
	}
	public void unregisterWrite() throws ClosedChannelException {
		key.interestOps(SelectionKey.OP_READ);
	}
}