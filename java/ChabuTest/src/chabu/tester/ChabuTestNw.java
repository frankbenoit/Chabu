package chabu.tester;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import chabu.INetwork;
import chabu.INetworkUser;
import chabu.Utils;
import chabu.tester.data.ACommand;
import chabu.tester.data.AResult;
import chabu.tester.data.AXferItem;
import chabu.tester.data.CmdDutConnect;
import chabu.tester.data.CmdDutDisconnect;

public class ChabuTestNw {
	private String name;
	private boolean doShutDown;
	private Selector selector;
	private final ArrayDeque<SelectorRegisterEntry> selectorRegisterEntries = new ArrayDeque<>(20);
	private CtrlNetwork ctrlNw = new CtrlNetwork();
	private Thread thread;

	private TreeMap<DutId, DutState> dutStates = new TreeMap<>();
	
	class DutState {
		
		final ByteBuffer txBuffer;
		final ArrayDeque<ACommand> commands;
		public SocketChannel socketChannel;
		int registeredInterrestOps = 0;
		public ByteBuffer rxBuffer;
		
		DutState(){
			commands = new ArrayDeque<>(100);
			txBuffer = ByteBuffer.allocate(2000);
			rxBuffer = ByteBuffer.allocate(2000);
			txBuffer.clear();
			rxBuffer.clear();
		}
		
		public void addCommand(ACommand cmd) throws ClosedChannelException {
			commands.add(cmd);
			if( (registeredInterrestOps & SelectionKey.OP_WRITE) == 0 ){
				registeredInterrestOps |= SelectionKey.OP_WRITE;
				synchronized(selectorRegisterEntries){
					SelectorRegisterEntry entry = new SelectorRegisterEntry( socketChannel, registeredInterrestOps, this );
					selectorRegisterEntries.add( entry );
				}
				selector.wakeup();
			}
		}
		
	}
	
	class CtrlNetwork extends Network {
	
	}

	class Network implements INetwork {
		
//		ServerSocketChannel serverSocket;
//		SocketChannel socketChannel;
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
//		public void connectionClose() throws IOException {
//			socketChannel.close();
//		}
	};
	
	public void setCtrlNetworkUser(INetworkUser user) {
		this.ctrlNw.user = user;
	}
	
	public ChabuTestNw(String name) throws IOException, InterruptedException {

		this.name = name;
		
		selector = Selector.open();

//		ctrlNw.serverSocket = ServerSocketChannel.open();
//		ctrlNw.serverSocket.configureBlocking(false);
//		ctrlNw.serverSocket.register( selector, 0 );
		
		thread = new Thread( this::run, name );
		thread.start();
	}
	
	private void run() {
		System.out.println("thread started");
		synchronized(selector){
			try {

				while (!doShutDown) {

					System.out.printf("%s: selector sleep\n", name );
					selector.select();
					System.out.printf("%s: selector wakeup\n", name );
					
					synchronized(selectorRegisterEntries){
						while( !selectorRegisterEntries.isEmpty() ){
							SelectorRegisterEntry entry = selectorRegisterEntries.remove();
							entry.socketChannel.register( selector, entry.interestOps, entry.attachment );
						}
					}
					
					Set<SelectionKey> readyKeys = selector.selectedKeys();

					ctrlNw.handleRequests();
					Iterator<SelectionKey> iterator = readyKeys.iterator();
					while (iterator.hasNext()) {
						SelectionKey key = iterator.next();
						iterator.remove();
						//					System.out.printf("%s selector key %s\n", name, key);
						if( key.isValid() ){
							if (key.isConnectable()) {
								//							System.out.printf("%s: connecting\n", name );
								synchronized( this ){
									DutState ds = (DutState)key.attachment();
									if (ds.socketChannel.isConnectionPending()){
										if( ds.socketChannel.finishConnect() ){
											ds.registeredInterrestOps &= ~SelectionKey.OP_CONNECT;
											ds.registeredInterrestOps |= SelectionKey.OP_READ;
											ds.socketChannel.register( selector, ds.registeredInterrestOps, ds );
											System.out.printf("%s: connecting ok\n", name );
										}
									}
								}
							}
							if (key.isWritable() ) {
								synchronized( this ){
									DutState ds = (DutState)key.attachment();
									//								System.out.printf("Tester %s: write p1\n", name );
									while( ds.txBuffer.remaining() > 1000 && !ds.commands.isEmpty() ){
										//System.out.printf("Tester %s: loop buf %s\n", name, ds.txBuffer );
										ACommand cmd = ds.commands.remove();
										AXferItem.encodeItem(ds.txBuffer, cmd);
									}
									//								System.out.printf("Tester %s: buf %s\n", name, ds.txBuffer );
									ds.txBuffer.flip();
									//								System.out.printf("Tester %s: write %s bytes\n", name, ds.txBuffer.remaining() );
									ds.socketChannel.write(ds.txBuffer);
									if( ds.commands.isEmpty() && !ds.txBuffer.hasRemaining() ){
										this.notifyAll();
									}
									if( !ds.txBuffer.hasRemaining() ){
										ds.registeredInterrestOps &= ~SelectionKey.OP_WRITE;
										ds.socketChannel.register( selector, ds.registeredInterrestOps, ds );
									}
									ds.txBuffer.compact();
									this.notifyAll();
								}
							}
							if( key.isReadable() ){
								synchronized( this ){
									DutState ds = (DutState)key.attachment();
									int readSz = ds.socketChannel.read( ds.rxBuffer );
									//								System.out.printf("%s read %d\n", name, readSz );
									ds.rxBuffer.flip();
									while( ds.rxBuffer.hasRemaining() ){
										AResult res = AResult.decodeResult(ds.rxBuffer);
										if( res == null ) {
											break;
										}
										consumeResult( res );
									}
									ds.rxBuffer.compact();
									if( readSz < 0 ){
										ds.socketChannel.close();
										System.out.printf("%s closed\n", name );
									}
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {

				try {
					synchronized(this){
						for( DutState ds : dutStates.values() ){
							if( ds.socketChannel != null ){
								ds.socketChannel.close();
								ds.socketChannel = null;
							}
						}
						dutStates.clear();
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
	}

	private void consumeResult(AResult res) {
		System.out.printf("%s: recv %s\n", name, res );
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

	public synchronized void addCommand(DutId dut, ACommand cmd) throws IOException {
		if( dut == DutId.ALL ){
			Utils.ensure( !(cmd instanceof CmdDutConnect) );
			Utils.ensure( !(cmd instanceof CmdDutDisconnect) );
			for( DutState ds : dutStates.values() ){
				ds.addCommand( cmd );
			}
		}
		else {
			if( !dutStates.containsKey(dut)){
				dutStates.put( dut, new DutState());
			}
			DutState ds = dutStates.get(dut);
			
			if( cmd instanceof CmdDutConnect ){
				Utils.ensure( ds.socketChannel == null );
				CmdDutConnect cmdDutConnect = (CmdDutConnect)cmd;
				SocketChannel socketChannel = SocketChannel.open();
				socketChannel.configureBlocking(false);
				socketChannel.connect( new InetSocketAddress( cmdDutConnect.address, cmdDutConnect.port ));
				ds.socketChannel = socketChannel;
				ds.registeredInterrestOps = SelectionKey.OP_CONNECT;
				synchronized(selectorRegisterEntries){
					SelectorRegisterEntry entry = new SelectorRegisterEntry( socketChannel, ds.registeredInterrestOps, ds);
					selectorRegisterEntries.add( entry );
				}
				selector.wakeup();
				System.out.println("selector wakeup!");
			}
			else if( cmd instanceof CmdDutDisconnect ){
				if( ds.socketChannel != null ) {
					ds.socketChannel.close();				
				}
			}
			else {
				ds.addCommand( cmd );
			}
		}
		notifyAll();
	}

	public synchronized void flush( DutId dut ) throws InterruptedException {
		while(true){
			boolean isEmpty = true;
			if( dut == DutId.ALL ){
				for( DutState ds : dutStates.values() ){
					if( !ds.commands.isEmpty() ){
						isEmpty = false;
					}
				}
			}
			else {
				DutState ds = dutStates.get(dut);
				if( ds != null && !ds.commands.isEmpty() ){
					isEmpty = false;
				}
			}

			if( isEmpty ){
				break;
			}
			wait();
		}
	}

}
