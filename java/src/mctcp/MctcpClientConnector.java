package mctcp;

import java.nio.channels.SocketChannel;

public class MctcpClientConnector {
	
	@SuppressWarnings("unused")
	private SocketChannel clientSocket;

	@SuppressWarnings("unused")
	private Channel[]     channels;

	public MctcpClientConnector( SocketChannel clientSocket, Channel ... channels ){
		this.clientSocket = clientSocket;
		this.channels = channels;
	}

}
