package chabu.tester.dut;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
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
import chabu.ChabuConnectingInfo;
import chabu.Channel;
import chabu.ILogConsumer;
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
		ByteBuffer rxBuffer = ByteBuffer.allocate(1_000_000);
		ByteBuffer txBuffer = ByteBuffer.allocate(1_000_000);
	}

	
	class TestChannelUser implements INetworkUser {

		Channel channel;
		public int rxCountRemaining;
		public int txCountRemaining;
		public int rxCountDone;
		public int txCountDone;
		public int txDataIndex;
		public int rxDataIndex;
		
		@Override
		public void evRecv(ByteBuffer buffer) {
			if( buffer == null ){
				return;
			}
			int p = buffer.remaining();
			while( buffer.hasRemaining() && rxCountRemaining > 0 ){
				int value = buffer.get() & 0xff;
				int expt = testData.get( rxDataIndex ) & 0xFF;
				Utils.ensure( value == expt, "Data mismatch: read:%02X expt:%02X", value, expt );
				rxDataIndex++;
				rxCountDone++;
				rxCountRemaining--;
			}
			testLog("DutNw channel evRecv %d bytes", p - buffer.remaining() );
		}

		@Override
		public boolean evXmit(ByteBuffer buffer) {
			boolean flush = false;
			int p = buffer.remaining();
			while( buffer.hasRemaining() && txCountRemaining > 0 ){
				byte value = testData.get( txDataIndex );
				buffer.put( value );
				txDataIndex++;
				txCountDone++;
				txCountRemaining--;
				if( txCountRemaining == 0 ){
					flush = true;
				}
			}
			testLog("DutNw channel evXmit %d bytes %s %s remaining", p - buffer.remaining(), buffer, txCountRemaining );
			return flush;
		}
		
		@Override
		public void setNetwork(INetwork nw) {
		}
	}
	class TestNetwork implements INetwork {
		
		ServerSocketChannel serverSocket;
		SocketChannel socketChannel;
		ByteBuffer rxBuffer = ByteBuffer.allocate(0x10000);
		ByteBuffer txBuffer = ByteBuffer.allocate(0x10000);
		
		Chabu chabu;
		ArrayList<TestChannelUser> channelUsers = new ArrayList<>(256);
		boolean userRequestRun = false;
		boolean userRequestXmit = false;
		boolean netwRequestRecv = true;
		boolean netwRequestXmit = true;
		public boolean activated = false;

		TestNetwork(){
//			rxBuffer.clear();
			rxBuffer.limit(0);
		}
		public void setUser(Chabu chabu) {
			this.chabu = chabu;
			chabu.setLogConsumer( this::chabuLog );
		}
		private void chabuLog( ILogConsumer.Category cat, String inst, String fmt, Object ... args ){
			logger.printfln( "%s:%s %s", cat.name(), inst, String.format( fmt, args ));
		}
		public void setNetworkUser(INetworkUser user) {
			Utils.fail("user internally set");
		}
		public void evUserXmitRequest() {
			userRequestXmit = true;
			netwRequestXmit = true;
			selector.wakeup();
		}
		public void evUserRecvRequest() {
			userRequestRun = true;
			netwRequestRecv = true;
			selector.wakeup();
		}
		public void handleRequests() throws IOException {
			if( socketChannel == null ) return;
			if( !socketChannel.isConnected() ) return;
			if( userRequestRun ){
				userRequestRun = false;
				handleRecv();
			}
			if( userRequestXmit ){
				userRequestXmit = false;
				handleXmit();
			}
		}
		public void connectionClose() throws IOException {
			socketChannel.close();
		}
		public void handleRecv() throws IOException{
			rxBuffer.compact();
			int readSz = socketChannel.read(rxBuffer);
			rxBuffer.flip();
			testLog( "NwSocket Read %d bytes", readSz );
			if( readSz < 0 ){
				testLog( "SocketClosed");
			}
			chabu.evRecv(testNw.rxBuffer);
		}
		public void handleXmit() throws IOException {
			netwRequestXmit = false;
			chabu.evXmit(testNw.txBuffer);
			txBuffer.flip();
			if( txBuffer.hasRemaining() ){
				int writeSz = socketChannel.write(txBuffer);
				testLog("NwSocket Write %d bytes", writeSz );
				if( txBuffer.hasRemaining() ){
					netwRequestXmit = true;
				}
			}
			testNw.txBuffer.compact();
		}
	};
	
	public ChabuTestDutNw(String name, int ctrlPort ) throws IOException {

		this.logger = Logger.getLogger(name);
		this.name = name;
		ctrlNw.serverSocket = ServerSocketChannel.open();
		ctrlNw.serverSocket.configureBlocking(false);
		ctrlNw.serverSocket.bind(new InetSocketAddress(ctrlPort));
		
		selector = Selector.open();
		ctrlNw.serverSocket.register( selector, SelectionKey.OP_ACCEPT, ctrlNw );
		
	}
	
	private void run() {
		System.out.println("ChabuTestDutNw.run()");
		try {

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
					if( !key.isValid() ){
						continue;
					}

					synchronized(this){
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
								
								testLog("ctrl connected" );

								enqueueResult( new ResultVersion( DUT_VERSION ) );

							}
							notifyAll();

							if (key.isReadable() ) {
								//testLog("DUT %s: read", name );
								int readSz = ctrlNw.socketChannel.read( ctrlNw.rxBuffer );
								//testLog("reading %s %s", readSz, ctrlNw.rxBuffer );
								ctrlNw.rxBuffer.flip();
								while( true ){
									ACommand cmd = ACommand.decodeCommand( ctrlNw.rxBuffer );
									if( cmd == null ){
										break;
									}
									recvCmd( cmd );
								}
								ctrlNw.rxBuffer.compact();

								if( readSz < 0 ){
									//testLog("DUT %s: closing", name );
									ctrlNw.socketChannel.close();
								}
								//testLog("DUT %s: read complete", name );
							}
							if (key.isWritable() ) {
								//testLog("write" );
								//testLog("%s", ctrlNw.txBuffer );
								while( ctrlNw.txBuffer.remaining() > 1000 && !results.isEmpty() ){
									//								testLog("Tester %s: loop buf %s", name, ctrlNw.txBuffer );
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
								testNw.socketChannel.register( selector, SelectionKey.OP_READ, testNw );
								testNw.serverSocket.register( selector, 0, testNw );
								notifyAll();
							} else if ( key.isConnectable() ){
								if (testNw.socketChannel.isConnectionPending()){
									if( testNw.socketChannel.finishConnect() ){
										testNw.socketChannel.register( selector, SelectionKey.OP_READ|SelectionKey.OP_WRITE, testNw );
										testLog("%s: connect finished ok", name );
									}
									else {
										testLog("%s: connect finished FAIL", name );
									}
								}
							} else if (key.isWritable() || key.isReadable() ) {
								testLog("Server selector can rd:%s wr:%s", key.isReadable(), key.isWritable());
								testNw.netwRequestRecv = true;
								
								if( key.isReadable() ){
									testNw.handleRecv();
								}
								else {
									testLog( "NwSocket Read None" );
								}

								//if( key.isWritable() ) 
								{
									testNw.handleXmit();
								}
//								else {
//									testLog("NwSocket Write None" );
//								}
								int interestOps = 0;
								if( testNw.netwRequestRecv ){
									interestOps |= SelectionKey.OP_READ;
								}
								if( testNw.netwRequestXmit ){
									interestOps |= SelectionKey.OP_WRITE;
								}
								testLog("InterestOps rd:%s wr:%s", testNw.netwRequestRecv, testNw.netwRequestXmit );
								testNw.socketChannel.register( selector, interestOps, testNw );

							}
						}
						else {
							testLog(">>>>>>> ChabuTestDutNw.run() unknown attachment %s", key.attachment());
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
				e.printStackTrace();
			}
		}
	}
	private void testLog( String fmt, Object ... args ){
		//logger.printfln( "Test %s", String.format( fmt, args ));
	}

	private void executeSchedCommand( long delay, ACmdScheduled cmd ) throws IOException {
		
		testLog("%s %sms", cmd, delay );

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
			testNw.socketChannel.register( selector, 
					SelectionKey.OP_CONNECT|SelectionKey.OP_READ|SelectionKey.OP_WRITE, testNw );
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
			tcu.channel = new Channel( c.rxCount, c.txCount );
			tcu.channel.setNetworkUser(tcu);
			tcu.txDataIndex = c.txInitialOffset;
			tcu.rxDataIndex = c.rxInitialOffset;
			testNw.channelUsers.add( tcu );
		}
		break;
		case SETUP_ACTIVATE:
		{
			Utils.ensure( !testNw.activated );
			CmdSetupActivate c = (CmdSetupActivate)cmd;
			
			ChabuConnectingInfo info = new ChabuConnectingInfo();
			info.applicationName = "";
			info.applicationVersion = 0x01000123;
			info.receiveCannelCount = 1;
			info.maxReceivePayloadSize = c.maxPayloadSize;
			info.byteOrderBigEndian = c.byteOrderBigEndian;  
			
			testNw.setUser( new Chabu( info ));
			for( TestChannelUser cu : testNw.channelUsers ){
				testNw.chabu.addChannel(cu.channel);
			}
			testNw.chabu.setNetwork(testNw);
			testNw.chabu.activate();
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
				cu.channel.evUserRecvRequest();
				testNw.evUserRecvRequest();
			}
			if( c.txCount > 0 ){
				System.out.printf("tx [%d] %d %d\n", c.channelId, c.txCount, cu.txCountRemaining );
				cu.channel.evUserXmitRequest();
				testNw.evUserXmitRequest();
			}
			
		}
		break;
		case CHANNEL_CREATE_STAT:
		{
			Utils.ensure( testNw.activated );
			CmdChannelCreateStat c = (CmdChannelCreateStat)cmd;
			TestChannelUser cu = testNw.channelUsers.get( c.channelId );
			
			ResultChannelStat res = new ResultChannelStat( createResultTimeStamp(), c.channelId, cu.txCountDone, cu.rxCountDone );
			cu.rxCountDone = 0;
			cu.txCountDone = 0;
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
	private void recvCmd(ACommand cmd) throws IOException {

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
