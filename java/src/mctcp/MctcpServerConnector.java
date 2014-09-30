package mctcp;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.TreeMap;

public class MctcpServerConnector {
	
	@SuppressWarnings("unused")
	private SocketChannel  clientSocket;

	boolean loginDone = false;
	Block   loginBlock;

	private TreeMap<Integer, Channel> channels = new TreeMap<>();
	private LinkedList<Channel> nextToSend = new LinkedList<Channel>();
	
	private INetworkConnector networkConnector = new INetworkConnector() {
	};
	
	public MctcpServerConnector( SocketChannel clientSocket, Channel[] channels ){
		this.clientSocket = clientSocket;
		for (int i = 0; i < channels.length; i++) {
			this.channels.put( channels[i].getId(), channels[i] );
		}
		
		for( Integer k : this.channels.keySet() ){
			Channel ch = this.channels.get(k);
			ch.setNetworkConnector( networkConnector );
			nextToSend.addLast( ch );
		}
		
	}

	public void doIo(SelectionKey key) {
		if( !loginDone ){
			if( loginBlock == null ){
				loginBlock = new Block();
			}
			//clientSocket.
		}
		// TODO Auto-generated method stub
		key.interestOps( SelectionKey.OP_READ );
		key.interestOps( SelectionKey.OP_WRITE );
		key.interestOps( 0 );

	}



}
