package chabu.tester.dut;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import chabu.Chabu;
import chabu.Channel;
import chabu.IChannelUser;
import chabu.INetwork;
import chabu.INetworkUser;
import chabu.Utils;
import chabu.tester.data.ACmdScheduled;
import chabu.tester.data.ACommand;
import chabu.tester.data.AResult;
import chabu.tester.data.CmdChannelCreateStat;
import chabu.tester.data.CmdConnectionAwait;
import chabu.tester.data.CmdConnectionConnect;
import chabu.tester.data.CmdSetupActivate;
import chabu.tester.data.CmdSetupChannelAdd;
import chabu.tester.data.CmdTimeBroadcast;
import chabu.tester.data.ResultChannelStat;
import chabu.tester.data.ResultVersion;

public class ChabuTestDutNw {

	private static final int DUT_VERSION = 0;
	private String name;
	private boolean doShutDown;
	private Selector selector;
	private Thread thread;
	
	private ArrayDeque<ACmdScheduled> commands = new ArrayDeque<>(100);
	private ArrayDeque<AResult>       results = new ArrayDeque<>(100);
	
	private long syncTimeRemote;
	private long syncTimeLocal;
	
	private CtrlNetwork ctrlNw = new CtrlNetwork();
	private TestNetwork testNw = new TestNetwork();

	class CtrlNetwork {
		SocketChannel socketChannel;
		ServerSocketChannel serverSocket;
		ByteBuffer rxBuffer = ByteBuffer.allocate(0x10000);
		ByteBuffer txBuffer = ByteBuffer.allocate(0x10000);
	}

	
	class TestChannelUser implements IChannelUser {

		Channel channel;
		
		@Override
		public void evRecv(ByteBuffer buffer, Object attachment) {
			
		}

		@Override
		public boolean evXmit(ByteBuffer buffer, Object attachment) {
			return false;
		}
		
	}
	class TestNetwork implements INetwork {
		
		ServerSocketChannel serverSocket;
		SocketChannel socketChannel;
		ByteBuffer rxBuffer = ByteBuffer.allocate(0x10000);
		ByteBuffer txBuffer = ByteBuffer.allocate(0x10000);
		
		Chabu user = new Chabu( ByteOrder.BIG_ENDIAN, 1000 );
		ArrayList<TestChannelUser> channelUsers = new ArrayList<>(256);
		boolean userRequestRecv = false;
		boolean userRequestXmit = false;
		boolean netwRequestRecv = true;
		boolean netwRequestXmit = true;
		public boolean activated = false;

		TestNetwork(){
//			rxBuffer.clear();
		}
		public void setNetworkUser(INetworkUser user) {
			Utils.fail("user internally set");
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

				long waitTime = 0;
				while( !commands.isEmpty() ){
					
					ACmdScheduled cmd = commands.element();
					
					long timeLocalMs = ( System.nanoTime() - syncTimeLocal  ) / 1000_000;
					long timeCmdMs   = ( cmd.schedTime     - syncTimeRemote ) / 1000_000;
					
					long diffMillis = timeLocalMs - timeCmdMs;

					if( diffMillis >= 0 ){
						executeSchedCommand( diffMillis, commands.remove() );
						continue;
					}

					waitTime = -diffMillis;
					break;
				}

				selector.select(waitTime);
				Set<SelectionKey> readyKeys = selector.selectedKeys();

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
//							System.out.printf("DUT %s: ctrl connected\n", name );

							enqueueResult( new ResultVersion( DUT_VERSION ) );

						}
						synchronized(this){
							notifyAll();
						}

						if (key.isReadable() ) {
//							System.out.printf("DUT %s: read\n", name );
							int readSz = ctrlNw.socketChannel.read( ctrlNw.rxBuffer );
//							System.out.printf("DUT %s: %s\n", name, ctrlNw.rxBuffer );
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
//								System.out.printf("DUT %s: closing\n", name );
								ctrlNw.socketChannel.close();
							}
//							System.out.printf("DUT %s: read complete\n", name );
						}
						if (key.isWritable() ) {
//							System.out.printf("DUT %s: write\n", name );
//							System.out.printf("DUT %s: %s\n", name, ctrlNw.txBuffer );
							while( ctrlNw.txBuffer.remaining() > 1000 && !results.isEmpty() ){
//								System.out.printf("Tester %s: loop buf %s\n", name, ctrlNw.txBuffer );
								AResult res = results.remove();
								AResult.encodeItem(ctrlNw.txBuffer, res);
							}
							ctrlNw.txBuffer.flip();
							ctrlNw.socketChannel.write( ctrlNw.txBuffer );
							if( !ctrlNw.txBuffer.hasRemaining() ){
								ctrlNw.socketChannel.register( selector, SelectionKey.OP_READ, ctrlNw );
							}
							ctrlNw.txBuffer.compact();
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

	private void executeSchedCommand( long delay, ACmdScheduled cmd ) throws IOException {
		
		System.out.printf("DUT %s: %sms %s\n", name, delay, cmd.commandId.name() );

		switch( cmd.commandId ){

		case TIME_BROADCAST  :  // handled directly in receive part
		case DUT_CONNECT     :  // used internally in Tester
		case DUT_DISCONNECT  :  // used internally in Tester
		{
			Utils.fail("Unexpected command at this stage");
		}
		break;

		case DUT_APPLICATION_CLOSE:
		{
			ctrlNw.socketChannel.close();
			shutDown();
		}
		return;

		case CONNECTION_AWAIT:
		{
			CmdConnectionAwait c = (CmdConnectionAwait)cmd;
			if( testNw.serverSocket == null ){
				testNw.serverSocket = ServerSocketChannel.open();
				testNw.serverSocket.configureBlocking(false);
			}
			testNw.serverSocket.bind(new InetSocketAddress(c.port));
			testNw.serverSocket.register( selector, SelectionKey.OP_ACCEPT, testNw );
		}
		break;
		case CONNECTION_CONNECT:
		{
			CmdConnectionConnect c = (CmdConnectionConnect)cmd;
			if( testNw.socketChannel == null ){
				testNw.socketChannel = SocketChannel.open();
				testNw.socketChannel.configureBlocking(false);
			}
			testNw.socketChannel.connect(new InetSocketAddress( c.address, c.port));
			testNw.socketChannel.register( selector, SelectionKey.OP_CONNECT|SelectionKey.OP_READ|SelectionKey.OP_WRITE, testNw );
		}
		break;
		case CONNECTION_CLOSE:
		{
			if( testNw.socketChannel != null ){
				testNw.socketChannel.register( selector, 0 );
				testNw.socketChannel.close();
				testNw.socketChannel  = null;
			}
		}
		break;
		case SETUP_CHANNEL_ADD:
		{
			Utils.ensure( !testNw.activated );
			CmdSetupChannelAdd c = (CmdSetupChannelAdd)cmd;
			Utils.ensure( testNw.channelUsers.size() == c.channelId, "Channel ID sequence is not correct, exp %d, but is %d", testNw.channelUsers.size(), c.channelId );
			TestChannelUser tcu = new TestChannelUser();
			tcu.channel = new Channel( c.rxCount, tcu );
			testNw.channelUsers.add( tcu );
		}
		break;
		case SETUP_ACTIVATE:
		{
			Utils.ensure( !testNw.activated );
			CmdSetupActivate c = (CmdSetupActivate)cmd;
			testNw.user = new Chabu( c.byteOrderBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN, c.maxPayloadSize );
			for( TestChannelUser cu : testNw.channelUsers ){
				testNw.user.addChannel(cu.channel);
			}
			testNw.activated  = true;
		}
		break;
		case CHANNEL_ACTION:
		{

		}
		break;
		case CHANNEL_CREATE_STAT:
		{
			CmdChannelCreateStat c = (CmdChannelCreateStat)cmd;
			ResultChannelStat res = new ResultChannelStat( createResultTimeStamp(), c.channelId, 1234, 5678 );
			enqueueResult( res );
		}
		break;
		}

		
	}
	private long createResultTimeStamp() {
		return System.nanoTime() - syncTimeLocal + syncTimeRemote;
	}
	private void enqueueResult(AResult res) throws ClosedChannelException {
		results.add( res );
		ctrlNw.socketChannel.register( selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE, ctrlNw );
	}
	private void consumeCmd(ACommand cmd) throws IOException {

		System.out.printf("DUT %s: -- %s --\n", name, cmd.getClass().getSimpleName());

		if( cmd instanceof CmdTimeBroadcast ){
			CmdTimeBroadcast c = (CmdTimeBroadcast)cmd;
			syncTimeLocal = System.nanoTime();
			syncTimeRemote = c.time;
		}
		else if( cmd instanceof ACmdScheduled ){
			commands.add( (ACmdScheduled)cmd );
		}
		else {
			Utils.fail("unexpected type %s", cmd );
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
