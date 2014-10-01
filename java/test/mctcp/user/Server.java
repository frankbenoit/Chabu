package mctcp.user;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.Iterator;
import java.util.Set;

import mctcp.Channel;
import mctcp.MctcpConnector;

public class Server implements Runnable {

	private boolean doShutDown;
	private ServerSocketChannel server;
	private AbstractSelector selector;
	private SocketChannel socketChannel;
	private Channel[] channels;

	private Server(ServerSocketChannel server, AbstractSelector selector, Channel[] channels) {
		this.server = server;
		this.selector = selector;
		this.channels = channels;
	}

	private Server(SocketChannel socketChannel, AbstractSelector selector, Channel[] channels) {
		this.socketChannel = socketChannel;
		this.selector = selector;
		this.channels = channels;
	}

	@Override
	public void run() {
		//ServerSocketChannel server = null;
		try {
			@SuppressWarnings("unused")
			int connectionOpenIndex = 0;

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
							MctcpConnector con = new MctcpConnector(clientSocket, channels );
							connectionOpenIndex++;
							clientSocket.register(selector, SelectionKey.OP_READ|SelectionKey.OP_CONNECT, con );
						} else if (key.isWritable() || key.isReadable() ) {
							//System.out.printf("Server selector %s\n", con.getConnectionName());
							MctcpConnector con = (MctcpConnector) key.attachment();
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

			try {
				if( server != null ){
					server.socket().close();
					server.close();
				}
				if( socketChannel != null ){
					socketChannel.close();
				}
				if( selector != null ) {
					selector.close();
				}
			} catch (Exception e) {
				//					startupException = e;
				e.printStackTrace();
			}
		}
	}


	public static Server startServer(int port, Channel[] channels ) throws IOException {
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		server.bind(new InetSocketAddress(port));
		AbstractSelector selector = server.provider().openSelector();
		server.register( selector, SelectionKey.OP_ACCEPT );
		Server runnable = new Server(server, selector, channels );
		Thread thread = new Thread(runnable);
		thread.start();
		return runnable;
	}
	public static Server startClient(String host, int port, Channel[] channels ) throws IOException{
		InetSocketAddress address = new InetSocketAddress(Inet4Address.getByName(host), port );
		SocketChannel socketChannel = SocketChannel.open(address);
		AbstractSelector selector = socketChannel.provider().openSelector();
		socketChannel.register( selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ );
		Server runnable = new Server(socketChannel, selector, channels );
		Thread thread = new Thread(runnable);
		thread.start();
		return runnable;
	}
}