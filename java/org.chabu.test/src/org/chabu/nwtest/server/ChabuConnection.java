package org.chabu.nwtest.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.chabu.prot.v1.Chabu;

class ChabuConnection extends AConnection {
	
	private Chabu chabu;
	
	public ChabuConnection( TestServerPort testServer, SocketChannel channel, SelectionKey key, Runnable xmitRequestListener ) {
		super( channel, key );
	}
	public void setChabu(Chabu chabu) {
		this.chabu = chabu;
	}

	public void accept(SelectionKey t) {
		try{
			if( t.isReadable() || t.isWritable() ){
				resetWriteReq();
				chabu.handleChannel(channel);
			}
		}
		catch( IOException e ){
			throw new RuntimeException(e);
		}
	}
	public void setXmitRequest(Runnable xmitRequest) {
	}
}