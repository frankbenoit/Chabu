package org.chabu.nwtest.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.chabu.ChabuBuilder;
import org.chabu.IChabu;
import org.chabu.TestUtils;
import org.chabu.nwtest.Const;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The TestServer is started and waiting for the TestClient to connect.
 * The TestServer is ported to all supported platforms and is the item under test.
 * 
 * @author Frank Benoit
 *
 */
public class TestServer {

	Selector selector;
	ControlConnection ctrlConnection;
	ChabuConnection   testConnection;
	LinkedList<AConnection> xmitRequestsPending = new LinkedList<>();
	
	IChabu chabu;
	private ArrayList<ChabuChannelUser> chabuChannelUsers = new ArrayList<>( 20 );

	class ControlConnection extends AConnection {
	
		ByteBuffer recvBuffer = ByteBuffer.allocate(1000);
		ByteBuffer xmitBuffer = ByteBuffer.allocate(1000);
		ChabuBuilder builder;
		String firstError = null;
		
		public ControlConnection(SocketChannel channel, SelectionKey key ) {
			super( channel, key );
		}
		
		public void accept(SelectionKey t) {
			try{
				if( !t.isValid() ){
					
				}
				if( t.isReadable() ){
					recvBuffer.compact();
					channel.read(recvBuffer);
					recvBuffer.flip();

				}
				
				try{
					String reqStr = StandardCharsets.UTF_8.decode(recvBuffer).toString().trim();
					if( !reqStr.isEmpty() ){
						JSONObject req = new JSONObject( reqStr );
						//System.out.printf( "ctrl req : %s\n", req );
						
						JSONObject resp = process( req );
						if( resp == null ){
							// closing, so go out without writing
							return;
						}
						//System.out.printf( "ctrl resp: %s\n", resp );
						xmitBuffer.put(StandardCharsets.UTF_8.encode(CharBuffer.wrap(resp.toString())));
					}
				}
				catch( JSONException e ){
					recvBuffer.position(0);
				}
					
				xmitBuffer.flip();
//				int sz = 
				channel.write(xmitBuffer);
				xmitBuffer.compact();
//				if( sz > 0 ){
//					System.out.printf("ctrl written %d\n", sz );
//				}
				if( xmitBuffer.position() > 0 ){
					t.interestOps( t.interestOps() | SelectionKey.OP_WRITE );
				}
				else {
					t.interestOps( t.interestOps() & ~SelectionKey.OP_WRITE );
				}
				
			}
			catch( IOException e ){
				throw new RuntimeException(e);
			}
		}

		private JSONObject process(JSONObject req) {
			String err = firstError;
			if( err != null ){
				firstError = null;
				return new JSONObject().put("IsError", true).put("Message", err );
			}
			switch(req.getString("Command")){
			
			case "ChabuBuilder.start":
				return builderStart( 
						req.getInt   ("ApplicationVersion"), 
						req.getString("ApplicationName"   ), 
						req.getInt   ("RecvBuffer"        ), 
						req.getInt   ("PriorityCount"     ));
				
			case "ChabuBuilder.addChannel":
				return builderAddChannel( 
						req.getInt("Channel"   ), 
						req.getInt("Priority"  ), 
						req.getInt("RecvBuffer"), 
						req.getInt("XmitBuffer"));
				
			case "ChabuBuilder.build":
				return builderBuild();
				
			case "Chabu.close":
				return chabuClose();
				
			case "Channel.recv":
				return channelRecv( 
						req.getInt("Channel"), 
						req.getInt("Amount" ));
				
			case "Channel.xmit":
				return channelXmit( 
						req.getInt("Channel"), 
						req.getInt("Amount" ));
				
			case "Channel.ensureCompleted":
				return channelEnsureCompleted( 
						req.getInt("Channel"));
				
			case "Channel.state":
				return channelState( 
						req.getInt("Channel"));
				
			case "Close":
				close();
				return null;
				
			default:
				return new JSONObject().put("IsError", true).put("Message", "Unknown Command");
			}
		}

		private JSONObject builderStart( int applicationVersion, String applicationName, int recvBufferSz, int priorityCount) {
			System.out.printf("builderStart( %s, %s, %s, %s)\n",  applicationVersion, applicationName, recvBufferSz, priorityCount );
			builder = ChabuBuilder.start( applicationVersion, applicationName, recvBufferSz, priorityCount);
			return new JSONObject();
		}

		private JSONObject builderAddChannel(int channel, int priority, int recvBufferSz, int xmitBufferSz) {
			System.out.printf("builderAddChannel( %s, %s, %s, %s)\n",  channel, priority, recvBufferSz, xmitBufferSz );
			chabuChannelUsers.ensureCapacity(channel+1);
			while( chabuChannelUsers.size() < channel+1 ){
				chabuChannelUsers.add(null);
			}
			chabuChannelUsers.set( channel, new ChabuChannelUser( channel, xmitBufferSz, this::errorReceiver ) );
			builder.addChannel( channel, recvBufferSz, priority, chabuChannelUsers.get(channel));
			return new JSONObject();
		}

		private JSONObject builderBuild() {
			System.out.printf("builderBuild()\n");
			chabu = builder.build();
			chabu.addXmitRequestListener( testConnection::xmitRequest );
			builder = null;
			return new JSONObject();
		}

		private JSONObject chabuClose() {
			System.out.printf("builderClose()\n");
			chabu = null;
			chabuChannelUsers.clear();
			return new JSONObject();
		}

		private JSONObject channelRecv(int channelId, int amount) {
			System.out.printf("channelRecv( %s, %s)\n",  channelId, amount );
			ChabuChannelUser user = chabuChannelUsers.get(channelId);
			user.addRecvAmount(amount);
			return new JSONObject();
		}

		private JSONObject channelXmit(int channelId, int amount) {
			if(Const.LOG_TIMING) System.out.printf("channelXmit( %s, %s)\n", channelId, amount );
			ChabuChannelUser user = chabuChannelUsers.get(channelId);
			user.addXmitAmount(amount);
			return new JSONObject();
		}
		private JSONObject channelEnsureCompleted(int channelId) {
			System.out.printf("channelEnsureCompleted( %s )\n", channelId );
			ChabuChannelUser user = chabuChannelUsers.get(channelId);
			user.ensureCompleted();
			return new JSONObject();
		}
		private JSONObject channelState(int channelId) {
			ChabuChannelUser user = chabuChannelUsers.get(channelId);
			return user.getState();
		}
		private void close() {
			
			try {
				selector.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		private void errorReceiver( String msg ){
			if( firstError == null ){
				System.out.println("Error: "+msg);
				firstError = msg;
			}
		}
	}
	class ChabuConnection extends AConnection {
		ByteBuffer recvBuffer = ByteBuffer.allocate(3000);
		ByteBuffer xmitBuffer = ByteBuffer.allocate(3000);
		
		public ChabuConnection(SocketChannel channel, SelectionKey key ) {
			super( channel, key );
			recvBuffer.clear().limit(0);
			xmitBuffer.clear();
		}

		private void xmitRequest(){
			addXmitRequest(this);
		}

		public void accept(SelectionKey t) {
			try{
				
				if( t.isReadable() ){
					recvBuffer.compact();
					channel.read(recvBuffer);
					recvBuffer.flip();
					if( chabu != null && recvBuffer.hasRemaining() ){
						chabu.evRecv( recvBuffer );
					}
				}
				
				if( chabu != null ){
					chabu.evXmit( xmitBuffer );
					if( xmitBuffer.position() > 0 ){
						xmitBuffer.flip();
						int sz = channel.write(xmitBuffer);
						
						if( Const.LOG_TIMING ) System.out.printf("channel.write %7s time=%5s\n", sz, System.currentTimeMillis()% 10_000 );
						
						xmitBuffer.compact();
					}
				}
			}
			catch( IOException e ){
				throw new RuntimeException(e);
			}
		}
	}

	class BasicConnection extends AConnection {
		private ByteBuffer recvBuffer = ByteBuffer.allocate(1);
		private AConnection client;

		public BasicConnection(SocketChannel channel, SelectionKey key ) {
			super( channel, key );
			

			recvBuffer.order(ByteOrder.BIG_ENDIAN);
			recvBuffer.clear().limit(1);
		}

		
		@Override
		public void accept(SelectionKey key) {
			try{
				
				if( client != null ){
					client.accept(key);
					return;
				}
				
				if( key == null ){
					return;
				}

				if( key.isReadable() ){
					channel.read(recvBuffer);
					if( recvBuffer.hasRemaining() ){
						channel.register( selector, SelectionKey.OP_READ, this );
					}
					else {
						recvBuffer.flip();
						if( recvBuffer.get() == 0 ){
							TestUtils.ensure( ctrlConnection == null );
							client = ctrlConnection = new ControlConnection( channel, channel.keyFor(selector) );
						}
						else {
							TestUtils.ensure( testConnection == null );
							client = testConnection = new ChabuConnection( channel, channel.keyFor(selector) );
						}
						client.accept(key);
					}
				}
			}
			catch( IOException e ){
				throw new RuntimeException(e);
			}
		}

		@Override
		public String toString() {
			if( client != null ) return client.toString();
			return super.toString();
		}
	}
	
	public static void main(String[] args) throws Exception {
		TestServer server = new TestServer();
		server.run(args);
	}

	void addXmitRequest( AConnection c ){
		synchronized (xmitRequestsPending) {
			xmitRequestsPending.add(c);
		}
		selector.wakeup();
	}
	
	private void run(String[] args) throws Exception {
		
		int port = -1;
		{
			int argIdx = -1;
			while( ++argIdx < args.length ){
				if( "-listen".equalsIgnoreCase( args[ argIdx ] )){
					argIdx++;
					if( argIdx >= args.length ) throw new RuntimeException("-listen without port");
					try{
						port = Integer.parseInt( args[ argIdx ] );
					}
					catch(NumberFormatException e ){
						throw new RuntimeException("-listen with illformed port number");
					}
					if( port < 100 || port > 0xFFFF ){
						throw new RuntimeException("-listen without legal port number: "+port);
					}
				}
			}
		}
		
		
		ServerSocketChannel ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ssc.bind(new InetSocketAddress(port));
		InetSocketAddress laddr = (InetSocketAddress)ssc.getLocalAddress();
		System.out.println("LocalPort = " + laddr.getPort() );
		
		selector = Selector.open();
		ssc.register( selector, SelectionKey.OP_ACCEPT );
		
		
		while( selector.isOpen() ){


			synchronized (xmitRequestsPending) {
				for( AConnection c : xmitRequestsPending ){
					c.registerWrite();
				}
				xmitRequestsPending.clear();
			}
			
			selector.select(2000);

			Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
            while(keyIterator.hasNext()) {

                //get the key itself
                SelectionKey key = keyIterator.next();
                keyIterator.remove();

                if(!key.isValid()) {
                    continue;
                }
				
//                System.out.printf("Server %s%s%s%s%s %s%s%s%s %s\n",
//                		key.isValid()       ? "V" : " ",
//                		key.isAcceptable()  ? "A" : " ",
//                		key.isConnectable() ? "C" : " ",
//                		key.isWritable()    ? "W" : " ",
//                		key.isReadable()    ? "R" : " ",
//    					(key.interestOps() & SelectionKey.OP_ACCEPT  ) != 0 ? "a" : " ",
//    					(key.interestOps() & SelectionKey.OP_CONNECT ) != 0 ? "c" : " ",
//    					(key.interestOps() & SelectionKey.OP_WRITE   ) != 0 ? "w" : " ",
//    					(key.interestOps() & SelectionKey.OP_READ    ) != 0 ? "r" : " ",
//                		key.attachment()
//                				);
				
                
                if( key.channel() == ssc ){
					if( key.isAcceptable() ){
						SocketChannel channel = ssc.accept();
						channel.configureBlocking(false);
						
						SelectionKey channelKey = channel.register( selector, SelectionKey.OP_READ );
						BasicConnection connection = new BasicConnection( channel, channelKey );
						channelKey.attach(connection);
						
						connection.accept(null);
					}
				}
				else {
					AConnection cons = (AConnection) key.attachment();
					if( key.isWritable() ){
						cons.unregisterWrite();
					}
					if( key.channel().isOpen() ){
						cons.accept( key );
					}
				}
				
				
			}
			
		}
	}
}
