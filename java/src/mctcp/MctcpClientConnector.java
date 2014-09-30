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

	// login make suggestion of channels info
	
	// [ 4b packet size ]
	// [ str sz ] CLIENT_NAME
	// [ 4b protocol version ]
	// [ str sz ] APPLICATION_NAME
	// [ 4b application version ]
	// [ 4b Channel Count ]
	// --- for each channel ----
	//   [ 4b block size  ]
	//   [ 4b block count ]

	// response
	// [ 4b packet size ]
	// [ str sz ] SERVER_NAME
	// [ 4b protocol version ]
	// [ str sz ] APPLICATION_NAME
	// [ 4b application version ]
	// [ 4b status ]
	// [ str sz ] message

}
