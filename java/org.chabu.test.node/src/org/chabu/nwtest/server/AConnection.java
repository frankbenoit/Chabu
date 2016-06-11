package org.chabu.nwtest.server;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

abstract class AConnection  {
	SocketChannel channel;
	private SelectionKey key;
	boolean writeReq = false;

	public AConnection(SocketChannel channel, SelectionKey key ) {
		this.channel = channel;
		this.key = key;
	}
	
	abstract void accept(SelectionKey t) throws Exception;
	
	public void registerWriteReq() throws ClosedChannelException {
		writeReq = true;
		key.interestOps(SelectionKey.OP_READ|SelectionKey.OP_WRITE);
		key.selector().wakeup();
	}
	public boolean hasWriteReq() {
		return writeReq;
	}
	public void resetWriteReq() throws ClosedChannelException {
		key.interestOps(SelectionKey.OP_READ);
		writeReq = false;
	}
	public SocketChannel getChannel() {
		return channel;
	}

}