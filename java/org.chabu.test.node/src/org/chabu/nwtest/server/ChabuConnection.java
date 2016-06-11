package org.chabu.nwtest.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import org.chabu.prot.v1.Chabu;

class ChabuConnection extends AConnection {
	
	private Chabu chabu;
	
	public ChabuConnection( TestServerPort testServer, SocketChannel channel, SelectionKey key ) {
		super( channel, key );
	}
	public void setChabu(Chabu chabu) {
		this.chabu = chabu;
		if( chabu == null ) return;
		try {
			chabu.handleChannel(channel);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void accept(SelectionKey t) {
		try{
			if( chabu == null ) return;
			if( t.isReadable() || t.isWritable() ){
				resetWriteReq();
				//System.out.println("-- handle chabu --");
				chabu.handleChannel(channel);
			}
		}
		catch( IOException e ){
			throw new RuntimeException(e);
		}
	}
}