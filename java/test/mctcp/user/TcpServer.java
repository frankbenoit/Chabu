package mctcp.user;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

import mctcp.Constants;
import mctcp.IChannel;
import mctcp.Utils;

public class TcpServer implements Runnable {

	private boolean doShutDown;
	private ServerSocketChannel server;
	private AbstractSelector selector;
	private SocketChannel client;
	private Thread thread;

	private TcpServer(ServerSocketChannel server, AbstractSelector selector) {
		this.server = server;
		this.selector = selector;
	}

	private TcpServer(SocketChannel socketChannel, AbstractSelector selector) {
		this.client = socketChannel;
		this.selector = selector;
	}

	public synchronized SocketChannel getClientSocket() {
		while( client == null ){
			Utils.waitOn(this);
		}
		return client;
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
				selector.select();

				Set<SelectionKey> readyKeys = selector.selectedKeys();
				//System.out.printf("<< select() readyKeys.size()=%s\n", readyKeys.size());

				Iterator<SelectionKey> iterator = readyKeys.iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();
					if( key.isValid() ){
						if (key.isAcceptable()) {
							Utils.ensure(client == null , "invalid state" );
							client = server.accept();
							client.configureBlocking(false);
							server.register( selector, 0 );
							client.register(selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE, null );
							synchronized(this){
								notifyAll();
							}
						} else if (key.isWritable() || key.isReadable() ) {
							//System.out.printf("Server selector %s\n", key);

							int interestOps = SelectionKey.OP_READ;
							if( key.isReadable() ){								
								channel.rxBuffer.compact();
								client.read(channel.rxBuffer);
								channel.rxBuffer.flip();
							}
							
							channel.handler.accept(channel);
							
							if( key.isWritable() ){								
								channel.txBuffer.flip();
								client.write(channel.txBuffer);
								if( channel.txBuffer.hasRemaining() ){
									interestOps |= SelectionKey.OP_WRITE;
								}
								channel.txBuffer.compact();
							}
							
							interestOps |= channel.interestedOps;
							channel.interestedOps = 0;
							client.register( selector, interestOps );
							//System.out.printf("reg key %02X\n", interestOps );
							if( channel.doClose  ){
								client.close();
							}
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
				if( client != null ){
					client.close();
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

	private MyChannel channel = new MyChannel();
	private class MyChannel implements IChannel {
		private ByteBuffer txBuffer = ByteBuffer.allocate(100000);
		private ByteBuffer rxBuffer = ByteBuffer.allocate(100000);
		boolean doClose = false;
		Consumer<IChannel> handler;
		private int interestedOps;
		
		{
			txBuffer.order(Constants.BYTE_ORDER);
			rxBuffer.order(Constants.BYTE_ORDER);
			rxBuffer.flip();
		}
		
		public boolean txPossible() {
			return client.keyFor(selector).isWritable();
		}
		public ByteBuffer txGetBuffer() {
			return txBuffer;
		}
		public boolean rxPossible() {
			return client.keyFor(selector).isReadable();
		}
		public ByteBuffer rxGetBuffer() {
			return rxBuffer;
		}
		public void setHandler(Consumer<IChannel> h) {
			this.handler = h;
		}
		public void registerWaitForRead() {
			interestedOps |= SelectionKey.OP_READ;
		}
		public void registerWaitForWrite() {
			interestedOps |= SelectionKey.OP_WRITE;
		}
		public void close() {
			doClose = true;
		}
	};

	public void start(){
		thread = new Thread(this);
		thread.start();
	}
	public static TcpServer startServer(int port ) throws IOException {
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		server.bind(new InetSocketAddress(port));
		AbstractSelector selector = server.provider().openSelector();
		server.register( selector, SelectionKey.OP_ACCEPT );
		TcpServer runnable = new TcpServer(server, selector );
		return runnable;
	}
	public static TcpServer startClient(String host, int port ) throws IOException{
		InetSocketAddress address = new InetSocketAddress(Inet4Address.getByName(host), port );
		SocketChannel socketChannel = SocketChannel.open(address);
		socketChannel.configureBlocking(false);
		AbstractSelector selector = socketChannel.provider().openSelector();
		socketChannel.register( selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ );
		TcpServer runnable = new TcpServer(socketChannel, selector );
		return runnable;
	}

	public void shutDown() {
		doShutDown = true;
		selector.wakeup();
	}

	public IChannel getChannel() {
		return channel;
	}
}