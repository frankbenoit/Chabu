package org.chabu.nwtest.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

import org.chabu.prot.v1.Chabu;

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
	LinkedList<AConnection> xmitRequestsPending = new LinkedList<>();
	

	public static void main(String[] args) throws Exception {
		TestServer server = new TestServer();
		server.run(15000);
	}


	public void addXmitRequest(){
		synchronized (xmitRequestsPending) {
			xmitRequestsPending.add(testConnection);
		}
		selector.wakeup();
	}
	
	private void run(int port) throws Exception {
		
		selector = Selector.open();
		
		ServerSocketChannel sscCtrl = createServerSocket(port+0);
		ServerSocketChannel sscTest = createServerSocket(port+1);
		
		InetSocketAddress laddr = (InetSocketAddress)sscCtrl.getLocalAddress();
		System.out.println("LocalPort = " + laddr.getPort()+" and +1" );
		
		
		
		while( selector.isOpen() ){


			synchronized (xmitRequestsPending) {
				for( AConnection c : xmitRequestsPending ){
					c.registerWriteReq();
				}
				xmitRequestsPending.clear();
			}
			
			selector.select(2000);
			//System.out.println("nw server select");

			Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while(keyIterator.hasNext()) {

                //get the key itself
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if(!key.isValid()) {
                    continue;
                }
				
                if( key.isAcceptable() ){
                	if( key.channel() == sscCtrl ){
                		ctrlConnection = acceptConnection(ControlConnection::new, sscCtrl);
					}
	                if( key.channel() == sscTest ){
	                	testConnection = acceptConnection(ChabuConnection::new, sscTest);
                	}
                }
				else {
					AConnection cons = (AConnection) key.attachment();
					if( key.isWritable() && !cons.hasWriteReq() ){
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


	private <T extends AConnection> T acceptConnection(ConnectionFactory<T> factory, ServerSocketChannel ssc) throws IOException, ClosedChannelException {
		SocketChannel channel = ssc.accept();
		channel.configureBlocking(false);
		SelectionKey channelKey = channel.register( selector, SelectionKey.OP_READ );
		T connection = factory.create( this, channel, channelKey, this::addXmitRequest );
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
		testConnection.setChabu(chabu);
	}
}
