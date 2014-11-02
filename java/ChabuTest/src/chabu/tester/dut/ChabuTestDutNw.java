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
import chabu.tester.Logger;
import chabu.tester.SelectorRegisterEntry;
import chabu.tester.data.ACmdScheduled;
import chabu.tester.data.ACommand;
import chabu.tester.data.AResult;
import chabu.tester.data.CmdChannelAction;
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
	private final ArrayDeque<SelectorRegisterEntry> selectorRegisterEntries = new ArrayDeque<>(20);
	private Thread thread;
	private final static TestData testData = new TestData();
	
	private ArrayDeque<ACmdScheduled> commands = new ArrayDeque<>(100);
	private ArrayDeque<AResult>       results = new ArrayDeque<>(100);
	
	private long syncTimeRemote;
	private long syncTimeLocal;
	
	private CtrlNetwork ctrlNw = new CtrlNetwork();
	private TestNetwork testNw = new TestNetwork();
	private Logger logger;

	class CtrlNetwork {
		SocketChannel socketChannel;
		ServerSocketChannel serverSocket;
		ByteBuffer rxBuffer = ByteBuffer.allocate(0x10000);
		ByteBuffer txBuffer = ByteBuffer.allocate(0x10000);
	}

	
	class TestChannelUser implements IChannelUser {

		Channel channel;
		public int rxCountRemaining;
		public int txCountRemaining;
		public int rxCountDone;
		public int txCountDone;
		public int txDataIndex;
		public int rxDataIndex;
		
		@Override
		public void evRecv(ByteBuffer buffer, Object attachment) {
			while( buffer.hasRemaining() && rxCountRemaining > 0 ){
				int value = buffer.get() & 0xff;
				Utils.ensure( value == testData.get( rxDataIndex ));
				rxDataIndex++;
				rxCountDone++;
				rxCountRemaining--;
			}
		}

		@Override
		public boolean evXmit(ByteBuffer buffer, Object attachment) {
			boolean flush = false;
			while( buffer.hasRemaining() && txCountRemaining > 0 ){
				buffer.put( testData.get( txDataIndex ) );
				txDataIndex++;
				txCountDone++;
				txCountRemaining--;
				if( txCountRemaining == 0 ){
					flush = true;
				}
			}
			return flush;
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
			rxBuffer.limit(0);
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

		this.logger = Logger.getLogger(name);
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

				synchronized(selectorRegisterEntries){
					while( !selectorRegisterEntries.isEmpty() ){
						SelectorRegisterEntry entry = selectorRegisterEntries.remove();
						entry.socketChannel.register( selector, entry.interestOps, entry.attachment );
					}
				}
				
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
//							logger.printfln("DUT %s: ctrl connected", name );

							enqueueResult( new ResultVersion( DUT_VERSION ) );

						}
						synchronized(this){
							notifyAll();
						}

						if (key.isReadable() ) {
//							logger.printfln("DUT %s: read", name );
							int readSz = ctrlNw.socketChannel.read( ctrlNw.rxBuffer );
//							logger.printfln("DUT %s: %s", name, ctrlNw.rxBuffer );
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
//								logger.printfln("DUT %s: closing", name );
								ctrlNw.socketChannel.close();
							}
//							logger.printfln("DUT %s: read complete", name );
						}
						if (key.isWritable() ) {
//							logger.printfln("DUT %s: write", name );
//							logger.printfln("DUT %s: %s", name, ctrlNw.txBuffer );
							while( ctrlNw.txBuffer.remaining() > 1000 && !results.isEmpty() ){
//								logger.printfln("Tester %s: loop buf %s", name, ctrlNw.txBuffer );
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
					else if( key.attachment() == testNw ){
						if (key.isAcceptable()) {
							Utils.ensure(testNw.socketChannel == null , "invalid state" );
							testNw.socketChannel = testNw.serverSocket.accept();
							testNw.socketChannel.configureBlocking(false);
							testNw.serverSocket.register( selector, 0 );
							synchronized(this){
								notifyAll();
							}
						} else if (key.isWritable() || key.isReadable() ) {
							logger.printfln("Server selector %s %s %s", name, key.isReadable(), key.isWritable());
							testNw.netwRequestRecv = true;
							if( key.isReadable() ){								
								testNw.rxBuffer.compact();
								testNw.socketChannel.read(testNw.rxBuffer);
								testNw.rxBuffer.flip();
								testNw.user.evRecv(testNw.rxBuffer);
							}
							
							//if( key.isWritable() )
							{
								testNw.netwRequestXmit = false;
								testNw.user.evXmit(testNw.txBuffer);
								testNw.txBuffer.flip();
								logger.printfln("%s Xmit %d", name, testNw.txBuffer.remaining() );
								testNw.socketChannel.write(testNw.txBuffer);
								if( testNw.txBuffer.hasRemaining() ){
									testNw.netwRequestXmit = true;
								}
								testNw.txBuffer.compact();
							}
							int interestOps = 0;
							if( testNw.netwRequestRecv ){
								interestOps |= SelectionKey.OP_READ;
							}
							if( testNw.netwRequestXmit ){
								interestOps |= SelectionKey.OP_WRITE;
							}
							logger.printfln("%s %x", name, interestOps );
							testNw.socketChannel.register( selector, interestOps );
							
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
		
		logger.printfln("DUT %s: %sms %s", name, delay, cmd.commandId.name() );

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
			tcu.txDataIndex = c.txInitialOffset;
			tcu.rxDataIndex = c.rxInitialOffset;
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
			testNw.user.activate();
			testNw.activated  = true;
		}
		break;
		case CHANNEL_ACTION:
		{
			Utils.ensure( testNw.activated );
			CmdChannelAction c = (CmdChannelAction)cmd;
			TestChannelUser cu = testNw.channelUsers.get( c.channelId );
			
			cu.rxCountRemaining += c.rxCount;
			cu.txCountRemaining += c.txCount;
			
			if( c.rxCount > 0 ){
				testNw.evUserRecvRequest();
			}
			if( c.txCount > 0 ){
				testNw.evUserXmitRequest();
			}
			
		}
		break;
		case CHANNEL_CREATE_STAT:
		{
			Utils.ensure( testNw.activated );
			CmdChannelCreateStat c = (CmdChannelCreateStat)cmd;
			TestChannelUser cu = testNw.channelUsers.get( c.channelId );
			
			ResultChannelStat res = new ResultChannelStat( createResultTimeStamp(), c.channelId, cu.rxCountDone, cu.txCountDone );
			enqueueResult( res );
			cu.rxCountDone = 0;
			cu.txCountDone = 0;
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

		logger.printfln("DUT %s: -- %s --", name, cmd.getClass().getSimpleName());

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
