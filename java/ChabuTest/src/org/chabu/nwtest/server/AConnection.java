package org.chabu.nwtest.server;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

abstract class AConnection  {
	SocketChannel channel;
	private SelectionKey key;
	boolean writeReq = false;
	private AConnection parent;

	public AConnection(AConnection parent, SocketChannel channel, SelectionKey key ) {
		this.parent = parent;
		this.channel = channel;
		this.key = key;
	}
	
	abstract void accept(SelectionKey t);
	
	public void registerWriteReq() throws ClosedChannelException {
		if( parent != null ){
			parent.registerWriteReq();
			return;
		}
		writeReq = true;
		key.interestOps(SelectionKey.OP_READ|SelectionKey.OP_WRITE);
		key.selector().wakeup();
	}
	public boolean hasWriteReq() {
		if( parent != null ){
			return parent.hasWriteReq();
		}
		return writeReq;
	}
	public void resetWriteReq() throws ClosedChannelException {
		if( parent != null ){
			parent.resetWriteReq();
			return;
		}
		key.interestOps(SelectionKey.OP_READ);
		writeReq = false;
	}
}