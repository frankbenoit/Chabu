package chabu;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.Iterator;
import java.util.Set;

public class Chabu_SingleServer implements INetwork {

	private String name;
	private boolean doShutDown;
	private ServerSocketChannel serverSocket;
	private AbstractSelector selector;
	private SocketChannel socketChannel;
	private Thread thread;
	private ByteBuffer rxBuffer = ByteBuffer.allocate(0x10000);
	{
		rxBuffer.position(rxBuffer.limit());
	}
	private ByteBuffer txBuffer = ByteBuffer.allocate(0x10000);
	
	private boolean userRequestRecv = false;
	private boolean userRequestXmit = false;
	
	private boolean netwRequestRecv = true;
	private boolean netwRequestXmit = true;
	private INetworkUser user;
	
	public void evUserRecvRequest() {
		userRequestRecv = true;
		netwRequestRecv = true;
		selector.wakeup();
	}
	public void evUserXmitRequest() {
		System.out.println("Chabu_SingleServer.evUserXmitRequest()");
		userRequestXmit = true;
		netwRequestXmit = true;
		selector.wakeup();
	}
	public void setNetworkUser(INetworkUser user) {
		this.user = user;
	}
	
	private Chabu_SingleServer(ServerSocketChannel server, AbstractSelector selector) {
		this.serverSocket = server;
		this.selector = selector;
	}

	private Chabu_SingleServer(SocketChannel socketChannel, AbstractSelector selector) {
		this.socketChannel = socketChannel;
		this.selector = selector;
	}

	public synchronized SocketChannel getClientSocket() {
		while( socketChannel == null ){
			Utils.waitOn(this);
		}
		return socketChannel;
	}
	
	
	public void run() {
		try {
			@SuppressWarnings("unused")
			int connectionOpenIndex = 0;

			while (!doShutDown) {

				selector.select(500);

				Set<SelectionKey> readyKeys = selector.selectedKeys();

				if( userRequestRecv ){
					userRequestRecv = false;
					user.evRecv(rxBuffer);
				}
				if( userRequestXmit ){
					userRequestXmit = false;
					user.evXmit(txBuffer);
				}
				Iterator<SelectionKey> iterator = readyKeys.iterator();
				while (iterator.hasNext()) {
					SelectionKey key = iterator.next();
					iterator.remove();
					if( key.isValid() ){
						if (key.isAcceptable()) {
							Utils.ensure(socketChannel == null , "invalid state" );
							socketChannel = serverSocket.accept();
							socketChannel.configureBlocking(false);
							serverSocket.register( selector, 0 );
							synchronized(this){
								notifyAll();
							}
						} else if (key.isWritable() || key.isReadable() ) {
							System.out.printf("Server selector %s %s %s\n", name, key.isReadable(), key.isWritable());
							netwRequestRecv = true;
							if( key.isReadable() ){								
								rxBuffer.compact();
								socketChannel.read(rxBuffer);
								rxBuffer.flip();
								user.evRecv(rxBuffer);
							}
							
							//if( key.isWritable() )
							{
								netwRequestXmit = false;
								user.evXmit(txBuffer);
								txBuffer.flip();
								System.out.printf("%s Xmit %d\n", name, txBuffer.remaining() );
								socketChannel.write(txBuffer);
								if( txBuffer.hasRemaining() ){
									netwRequestXmit = true;
								}
								txBuffer.compact();
							}
							
						}
					}
				}
				int interestOps = 0;
				if( netwRequestRecv ){
					interestOps |= SelectionKey.OP_READ;
				}
				if( netwRequestXmit ){
					interestOps |= SelectionKey.OP_WRITE;
				}
				System.out.printf("%s %x\n", name, interestOps );
				socketChannel.register( selector, interestOps );
			}
			//			} catch( XMLStreamException e ){
			//				ExceptionUtil.log(e);
			//				startupException = e;
		} catch (Exception e) {
			//				startupException = e;
			e.printStackTrace();
		} finally {

			try {
				if( serverSocket != null ){
					serverSocket.socket().close();
					serverSocket.close();
				}
				if( socketChannel != null ){
					socketChannel.close();
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


	public void start(){
		thread = new Thread(this::run);
		thread.start();
	}
	
	public static Chabu_SingleServer startServer(INetworkUser nwUser, int port ) throws IOException {
		ServerSocketChannel server = ServerSocketChannel.open();
		server.configureBlocking(false);
		server.bind(new InetSocketAddress(port));
		AbstractSelector selector = server.provider().openSelector();
		server.register( selector, SelectionKey.OP_ACCEPT );
		Chabu_SingleServer nw = new Chabu_SingleServer( server, selector );
		nw.name = "S";
		nwUser.setNetwork(nw);
		nw.setNetworkUser(nwUser);
		return nw;
	}
	public static Chabu_SingleServer startClient(INetworkUser nwUser, String host, int port ) throws IOException{
		InetSocketAddress address = new InetSocketAddress(Inet4Address.getByName(host), port );
		SocketChannel socketChannel = SocketChannel.open(address);
		socketChannel.configureBlocking(false);
		AbstractSelector selector = socketChannel.provider().openSelector();
		socketChannel.register( selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ );
		Chabu_SingleServer nw = new Chabu_SingleServer( socketChannel, selector );
		nw.name = "C";
		nwUser.setNetwork(nw);
		nw.setNetworkUser(nwUser);
		return nw;
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
	
	static class ChannelUser implements IChannelUser {
		Channel channel = new Channel( 4, this );
		private String name;
		ChannelUser(String name){
			this.name = name;
		}
		public void evRecv(ByteBuffer buffer, Object attachment) {
			System.out.printf("ChannelUser %s-RX %d\n", name, buffer.remaining() );
			buffer.position( buffer.limit() );
		}
		int txRem = 12000;
		public boolean evXmit(ByteBuffer buffer, Object attachment) {
			int copy = txRem;
			if( copy > buffer.remaining() ){
				copy = buffer.remaining();
			}
			txRem -= copy;
			buffer.position( buffer.position() + copy );
			System.out.printf("ChannelUser %s-TX %d\n", name, copy );
			return false;
		}
		
	};
	
	public static void main(String[] args) throws IOException, InterruptedException {
		ChannelUser cus;
		ChannelUser cuc;
		Chabu_SingleServer server;
		Chabu_SingleServer client;
		{
			Chabu chabuServer = new Chabu(ByteOrder.BIG_ENDIAN, 1000 );
			chabuServer.instanceName = "S";
			cus = new ChannelUser("ChSrv");
			chabuServer.addChannel( cus.channel );
			chabuServer.activate();
			server = startServer(chabuServer, 20000);
			server.start();
		}
		{
			Chabu chabuClient = new Chabu(ByteOrder.BIG_ENDIAN, 1000 );
			chabuClient.instanceName = "C";
			cuc = new ChannelUser("ChCli");
			chabuClient.addChannel( cuc.channel );
			chabuClient.activate();
			client = startClient(chabuClient, "localhost", 20000);
			client.start();
		}

		//cus.channel.evUserWriteRequest();
		synchronized (Chabu_SingleServer.class) {
			Chabu_SingleServer.class.wait(3000);	
		}
		client.forceShutDown();
		server.forceShutDown();
	}
}