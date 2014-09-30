package mctcp.user;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.Iterator;
import java.util.Set;

import mctcp.MctcpServerConnector;

public class Server {
	int port = 2000;
	private AbstractSelector selector;
	private boolean doShutDown;
	
	
	Runnable selectorRunnable = new Runnable() {

		@Override
		public void run() {
			ServerSocketChannel server = null;
			try {
				@SuppressWarnings("unused")
				int connectionOpenIndex = 0;
				
				server = ServerSocketChannel.open();
				server.configureBlocking(false);
				ServerSocket ss = server.socket();
				//ss.setReuseAddress(true);
				ss.bind(new InetSocketAddress(port));
				selector = server.provider().openSelector();// Selector.open();
				server.register( selector, SelectionKey.OP_ACCEPT );
				//int cnt = 0;
				while (!doShutDown) {
					
					//System.out.println(">> select() " + cnt++ );
					selector.select( 500 );

					Set<SelectionKey> readyKeys = selector.selectedKeys();
					//System.out.printf("<< select() readyKeys.size()=%s\n", readyKeys.size());
					
					Iterator<SelectionKey> iterator = readyKeys.iterator();
					while (iterator.hasNext()) {
						SelectionKey key = iterator.next();
						iterator.remove();
						if( key.isValid() ){
							if (key.isAcceptable()) {
								SocketChannel clientSocket = server.accept();
								clientSocket.configureBlocking(false);
								MctcpServerConnector con = new MctcpServerConnector(clientSocket, null);
								connectionOpenIndex++;
								clientSocket.register(selector, SelectionKey.OP_READ|SelectionKey.OP_CONNECT, con );
							} else if (key.isWritable() || key.isReadable() ) {
								//System.out.printf("Server selector %s\n", con.getConnectionName());
								MctcpServerConnector con = (MctcpServerConnector) key.attachment();
								con.doIo( key );
							}
							else {
//								log.warn(String.format("select key? %s %s", key.isConnectable(), key.isValid()));
								
							}
						}
						else {
//							log.warn("select key no more valid." );
						}
					}
			    }
//			} catch( XMLStreamException e ){
//				ExceptionUtil.log(e);
//				startupException = e;
			} catch (Exception e) {
//				startupException = e;
				e.printStackTrace();
			} finally {
				synchronized( Server.this ){					
					Server.this.notifyAll();
				}

				try {
					
					server.socket().close();
					server.close();
					
					if( selector != null ) selector.close();
				} catch (Exception e) {
//					startupException = e;
					e.printStackTrace();
				}
			}
		}
	};

}