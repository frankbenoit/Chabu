package chabu.tester.dut;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import chabu.INetwork;
import chabu.INetworkUser;
import chabu.Utils;
import chabu.tester.data.ACommand;
import chabu.tester.data.CmdDutApplicationClose;

public class ChabuTestDutNw {

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
			rxBuffer.clear();
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
	
	public ChabuTestDutNw(String name, int ctrlPort ) throws IOException {

		this.name = name;
		ctrlNw.serverSocket = ServerSocketChannel.open();
		ctrlNw.serverSocket.configureBlocking(false);
		ctrlNw.serverSocket.bind(new InetSocketAddress(ctrlPort));
		
//		testNw.serverSocket = ServerSocketChannel.open();
//		testNw.serverSocket.configureBlocking(false);
//		testNw.serverSocket.bind(new InetSocketAddress(testPort));
		
		selector = Selector.open();
		ctrlNw.serverSocket.register( selector, SelectionKey.OP_ACCEPT, ctrlNw );
//		testNw.serverSocket.register( selector, SelectionKey.OP_ACCEPT );
		
	}
	
	private void run() {
		System.out.println("ChabuTestDutNw.run()");
		try {
			@SuppressWarnings("unused")
			int connectionOpenIndex = 0;

			while (!doShutDown) {

				selector.select(0);

				Set<SelectionKey> readyKeys = selector.selectedKeys();

				ctrlNw.handleRequests();
				testNw.handleRequests();
				Iterator<SelectionKey> iterator = readyKeys.iterator();
				while (iterator.hasNext()) {
					
					SelectionKey key = iterator.next();
					iterator.remove();
					Utils.ensure( key.isValid() );
					
					if( key.attachment() == ctrlNw ){
						if (key.isAcceptable()) {
							SocketChannel acceptedChannel = ctrlNw.serverSocket.accept();
							if( ctrlNw.socketChannel != null ){
								ctrlNw.socketChannel.close();
							}
							ctrlNw.socketChannel = acceptedChannel;
							ctrlNw.socketChannel.configureBlocking(false);
							ctrlNw.serverSocket.register(selector, SelectionKey.OP_ACCEPT, ctrlNw);
							ctrlNw.socketChannel.register(selector, SelectionKey.OP_READ, ctrlNw);
							System.out.printf("DUT %s: ctrl connected\n", name );
						}
						synchronized(this){
							notifyAll();
						}

						if (key.isReadable() ) {
							System.out.printf("DUT %s: read 1\n", name );
							if( key.attachment() instanceof CtrlNetwork ){
								int readSz = ctrlNw.socketChannel.read( ctrlNw.rxBuffer );
								System.out.printf("DUT %s: %s\n", name, ctrlNw.rxBuffer );
								ctrlNw.rxBuffer.flip();
								while( true ){
									ACommand cmd = ACommand.decodeCommand( ctrlNw.rxBuffer );
									if( cmd == null ){
										break;
									}
									consumeCmd( cmd );
								}
								ctrlNw.rxBuffer.compact();

								if( readSz < 0 ){
									System.out.printf("DUT %s: closing\n", name );
									ctrlNw.socketChannel.close();
								}
								System.out.printf("DUT %s: read complete\n", name );
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

	private void consumeCmd(ACommand cmd) throws IOException {
		System.out.printf("DUT %s: -- %s --\n", name, cmd.getClass().getSimpleName());
		if( cmd instanceof CmdDutApplicationClose ){
			ctrlNw.socketChannel.close();
			shutDown();
		}
	}
	public void start() {
		Utils.ensure( thread == null );
		thread = new Thread(this::run, name );
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
			ChabuTestDutNw client = mainInternalCreateThread( "DUT", ctrlPort );
			client.start();
			client.join();
		}
		catch( Exception e ){
			e.printStackTrace();
			usageExit();
		}
	}

	public void join() throws InterruptedException {
		thread.join();
	}
	private static void usageExit() {
		System.err.println("ChabuClientTester <control-port>");
		System.exit(1);
	}
	public static ChabuTestDutNw mainInternalCreateThread(String name, int firstListenPort) throws IOException {
		ChabuTestDutNw client = new ChabuTestDutNw( name, firstListenPort );
		client.start();
		return client;
	}

}
