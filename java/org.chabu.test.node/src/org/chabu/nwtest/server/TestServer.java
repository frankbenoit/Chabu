package org.chabu.nwtest.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.chabu.prot.v1.Chabu;

import com.sun.istack.internal.NotNull;


/**
 * The TestServer is started and waiting for the TestClient to connect.
 * The TestServer is ported to all supported platforms and is the item under test.
 * 
 * @author Frank Benoit
 *
 */
public class TestServer implements TestServerPort {

	Selector selector;
	ControlConnection ctrlConnection;
	ChabuConnection testConnection;
	Set<AConnection> xmitRequestsPending = new HashSet<>();
	ReentrantLock selectorLock = new ReentrantLock();
	boolean expectClose = false;
	private Chabu chabu;

	public static void main(String[] args) throws Exception {
		int port = 15000;
		if( args.length == 1 ){
			port = Integer.parseInt(args[0]);
		}
		TestServer server = new TestServer();
		server.run(port);
	}


	public void addXmitRequest(){
		synchronized (xmitRequestsPending) {
			//System.out.println("-- TX request --");
			xmitRequestsPending.add(testConnection);
		}
		selector.wakeup();
	}
	
	private void run(int port) throws Exception {
		
		selector = Selector.open();
		
		ServerSocketChannel sscCtrl = createServerSocket(port+0);
		ServerSocketChannel sscTest = createServerSocket(port+1);
		
		System.out.printf("Control Port %s%n", port );
		
		while( selector.isOpen() ){


			synchronized (xmitRequestsPending) {
				for( AConnection c : xmitRequestsPending ){
					c.registerWriteReq();
				}
				xmitRequestsPending.clear();
			}
			
			selectorLock.lock();
			try {
				selector.select(20000);
			} finally {
				selectorLock.unlock();
			}

			//System.out.println("nw server select");

			Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while(keyIterator.hasNext()) {

                //get the key itself
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if(!key.isValid() && !key.channel().isOpen() && key.attachment() == testConnection ) {
                	//ChabuConnection connection = (ChabuConnection)key.attachment();
                	if( !expectClose ){
                    	System.out.println("*** chabu connection closed by remote ***");
                	}
                	else {
                    	System.err.println("*** chabu connection closed by remote UNEXPECTED!!! ***");
                	}
                	testConnection = null;
                }
                if(!key.isValid()) {
                    continue;
                }
				
                if( key.isAcceptable() ){
                	if( key.channel() == sscCtrl ){
                		ctrlConnection = acceptConnection(ControlConnection::new, sscCtrl);
                		System.out.println("accepted control");
					}
	                if( key.channel() == sscTest ){
	                	testConnection = acceptConnection(ChabuConnection::new, sscTest);
	                	System.out.println("accepted testing chabu");
	                	if( chabu != null ){
	                		testConnection.setChabu(chabu);
	                	}
                	}
                }
				else {
					AConnection cons = (AConnection) key.attachment();
					if( key.isWritable()/* && !cons.hasWriteReq()*/ ){
						cons.resetWriteReq();
					}
					if( key.channel().isOpen() ){
						cons.accept( key );
					}
				}
			}
		}
		NwtUtil.closeLog();
	}


	private <T extends AConnection> T acceptConnection(ConnectionFactory<T> factory, ServerSocketChannel ssc) throws Exception {
		SocketChannel channel = ssc.accept();
		T connection = configureConnection(factory, channel);
		return connection;
	}


	private <T extends AConnection> T configureConnection(ConnectionFactory<T> factory, SocketChannel channel) throws IOException, ClosedChannelException, Exception {
		channel.configureBlocking(false);
		SelectionKey channelKey = channel.register( selector, SelectionKey.OP_READ );
		T connection = factory.create( this, channel, channelKey );
		channelKey.attach(connection);
		return connection;
	}

	private ServerSocketChannel createServerSocket(int port) throws IOException, ClosedChannelException {
		ServerSocketChannel sscCtrl = ServerSocketChannel.open();
		sscCtrl.configureBlocking(false);
		sscCtrl.bind(new InetSocketAddress(port));
		sscCtrl.register( selector, SelectionKey.OP_ACCEPT );
		return sscCtrl;
	}

	public void close() {
		try {
			selector.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	@Override
	public void setChabu(Chabu chabu) {
		this.chabu = chabu;
		if( chabu != null ){
			chabu.addXmitRequestListener( this::addXmitRequest );
		}
		if( testConnection != null ){
			testConnection.setChabu(chabu);
		}
	}


	@Override
	public void connectSync(@NotNull String hostName, int port) {
		System.out.println("connect: syncing with selector ...");
		selectorLock.lock();
		try {
			while( selectorLock.hasQueuedThreads() ){
				selector.wakeup();
				Thread.sleep(20);
			}
			System.out.printf("connect: starting to open connection %s:%s ...%n", hostName, port );
			SocketChannel channel = SocketChannel.open();
			channel.configureBlocking(false);
			channel.connect(new InetSocketAddress(hostName, port));
			int waitIndex = 0;
			while( !channel.finishConnect() ){
				Thread.sleep(20);
				waitIndex++;
				if( waitIndex % 10 == 0 ){
					System.out.printf(".");
				}
				if( waitIndex > 100 ){
					break;
				}
			}
			ChabuConnection connection = configureConnection(ChabuConnection::new, channel );
			testConnection = connection;
			testConnection.setChabu(chabu);

			System.out.println("connect: OK");
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			selectorLock.unlock();
		}
		
	}


	@Override
	public void expectClose() {
		expectClose = true;
	}


	@Override
	public void ensureClosed() {
		if( testConnection != null ){
			System.err.println("*** network connection should have been closed by remote party ***");
			try{
				SocketChannel channel = testConnection.getChannel();
				channel.close();
			}
			catch( IOException e ){
				throw new RuntimeException(e);
			}
			finally{
				testConnection = null;
			}
		}
	}
}
