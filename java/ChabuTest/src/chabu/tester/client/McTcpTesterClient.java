package chabu.tester.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.Set;

import chabu.INetwork;
import chabu.INetworkUser;
import chabu.Utils;

public class McTcpTesterClient {

	private String name;
	private boolean doShutDown;
	private Selector selector;
	private Thread thread;
	
	private CtrlNetwork ctrlNw = new CtrlNetwork();
	private TestNetwork testNw = new TestNetwork();

	class CtrlNetwork extends Network {
	
	}

	class TestNetwork extends Network {
		
	}
	class Network implements INetwork {
		
		ServerSocketChannel serverSocket;
		SocketChannel socketChannel;
		ByteBuffer rxBuffer = ByteBuffer.allocate(0x10000);
		ByteBuffer txBuffer = ByteBuffer.allocate(0x10000);
		
		INetworkUser user;
		boolean userRequestRecv = false;
		boolean userRequestXmit = false;
		boolean netwRequestRecv = true;
		boolean netwRequestXmit = true;

		Network(){
			rxBuffer.position(rxBuffer.limit());
		}
		public void setNetworkUser(INetworkUser user) {
			this.user = user;
		}
		public void evUserXmitRequest() {
			userRequestXmit = true;
			netwRequestRecv = true;
			selector.wakeup();
		}
		public void evUserRecvRequest() {
			userRequestRecv = true;
			netwRequestXmit = true;
			selector.wakeup();
		}
		public void handleRequests() {
			if( userRequestRecv ){
				userRequestRecv = false;
				user.evRecv(rxBuffer);
			}
			if( userRequestXmit ){
				userRequestXmit = false;
				user.evXmit(txBuffer);
			}
		}
		public void connectionClose() throws IOException {
			socketChannel.close();
		}
	};
	
	public void setCtrlNetworkUser(INetworkUser user) {
		this.ctrlNw.user = user;
	}
	public void setTestNetworkUser(INetworkUser user) {
		this.testNw.user = user;
	}
	
	public McTcpTesterClient(int ctrlPort, int testPort) throws IOException {

		ctrlNw.serverSocket = ServerSocketChannel.open();
		ctrlNw.serverSocket.configureBlocking(false);
		ctrlNw.serverSocket.bind(new InetSocketAddress(ctrlPort));
		
		testNw.serverSocket = ServerSocketChannel.open();
		testNw.serverSocket.configureBlocking(false);
		testNw.serverSocket.bind(new InetSocketAddress(testPort));
		
		selector = Selector.open();
		ctrlNw.serverSocket.register( selector, SelectionKey.OP_ACCEPT );
		testNw.serverSocket.register( selector, SelectionKey.OP_ACCEPT );
		
	}
	
	public void run() {
		try {
			@SuppressWarnings("unused")
			int connectionOpenIndex = 0;

			while (!doShutDown) {

				selector.select(500);

				Set<SelectionKey> readyKeys = selector.selectedKeys();

				ctrlNw.handleRequests();
				testNw.handleRequests();
				Iterator<SelectionKey> iterator = readyKeys.iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();
					if( key.isValid() ){
						if (key.isAcceptable()) {
							if( key.channel() == ctrlNw.serverSocket ){
								SocketChannel acceptedChannel = ctrlNw.serverSocket.accept();
								ctrlNw.socketChannel.close();
								ctrlNw.socketChannel = acceptedChannel;
								ctrlNw.socketChannel.configureBlocking(false);
								ctrlNw.serverSocket.register(selector, SelectionKey.OP_ACCEPT, ctrlNw);
							}
							else if( key.channel() == testNw.serverSocket ){
								SocketChannel acceptedChannel = testNw.serverSocket.accept();
								testNw.socketChannel.close();
								testNw.socketChannel = acceptedChannel;
								testNw.socketChannel.configureBlocking(false);
								testNw.serverSocket.register(selector, SelectionKey.OP_ACCEPT, testNw);
							}
							else {
								Utils.ensure(false , "invalid state" );
							}
							synchronized(this){
								notifyAll();
							}

						} else if (key.isWritable() || key.isReadable() ) {
							System.out.printf("Server selector %s %s %s\n", name, key.isReadable(), key.isWritable());
							Network nw = (Network)key.attachment();
							nw.netwRequestRecv = true;
							if( key.isReadable() ){								
								nw.rxBuffer.compact();
								int readSz = ((ReadableByteChannel)key.channel()).read(nw.rxBuffer);
								if( readSz < 0 ){
									key.cancel();
									nw.connectionClose();
								}
								nw.rxBuffer.flip();
								nw.user.evRecv(nw.rxBuffer);
							}
							
							if( key.isValid() && key.isWritable() ) {
								nw.netwRequestXmit = false;
								nw.user.evXmit(nw.txBuffer);
								nw.txBuffer.flip();
								System.out.printf("%s Xmit %d\n", name, nw.txBuffer.remaining() );
								((WritableByteChannel)key.channel()).write(nw.txBuffer);
								if( nw.txBuffer.hasRemaining() ){
									nw.netwRequestXmit = true;
								}
								nw.txBuffer.compact();
							}
							
							if( key.isValid()){
								int interestOps = 0;
								if( nw.netwRequestRecv ){
									interestOps |= SelectionKey.OP_READ;
								}
								if( nw.netwRequestXmit ){
									interestOps |= SelectionKey.OP_WRITE;
								}
								System.out.printf("%s %x\n", name, interestOps );
								key.channel().register( selector, interestOps, nw );
							}
						}
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			try {
				if( ctrlNw.socketChannel != null ){
					ctrlNw.socketChannel.socket().close();
					ctrlNw.socketChannel.close();
				}
				if( testNw.socketChannel != null ){
					testNw.socketChannel.socket().close();
					testNw.socketChannel.close();
				}
				if( ctrlNw.serverSocket != null ){
					ctrlNw.serverSocket.close();
				}
				if( testNw.serverSocket != null ){
					testNw.serverSocket.close();
				}
				if( selector != null ) {
					selector.close();
				}
				synchronized( this ){
					notifyAll();
				}
			} catch (Exception e) {
				//					startupException = e;
				e.printStackTrace();
			}
		}
	}

	public void start() {
		Utils.ensure( thread == null );
		thread = new Thread(this::run);
		thread.start();
	}


	public void shutDown() {
		doShutDown = true;
		selector.wakeup();
	}

	public synchronized void forceShutDown() {
		shutDown();
		while( selector.isOpen() ){
			Utils.waitOn(this);
		}
	}

	
	public static void main(String[] args) {
		if( args.length != 1 ){
			usageExit();
		}
		try{
			int ctrlPort = Integer.parseInt( args[0] );
			int testPort = ctrlPort+1;
			McTcpTesterClient client = new McTcpTesterClient( ctrlPort, testPort );
			client.run();
		}
		catch( Exception e ){
			e.printStackTrace();
			usageExit();
		}
	}

	private static void usageExit() {
		System.err.println("McTcpClientTester <control-port>");
		System.exit(1);
	}

}
