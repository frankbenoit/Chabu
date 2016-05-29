package org.chabu.nwtest.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.chabu.prot.v1.Chabu;
import org.chabu.prot.v1.ChabuBuilder;
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
	
	Chabu chabu;
	private ArrayList<TestChannelUser> chabuChannelUsers = new ArrayList<>( 20 );

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
				
			case "Chabu.getState":
				return chabuGetState();
				
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
			chabuChannelUsers.set( channel, new TestChannelUser( channel, xmitBufferSz, this::errorReceiver ) );
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

		private JSONObject chabuGetState() {
			System.out.printf("chabuGetState()\n");
			return new JSONObject()
					.put("toString", chabu.toString());
		}

		private JSONObject chabuClose() {
			System.out.printf("chabuClose()\n");
			chabu = null;
			chabuChannelUsers.clear();
			return new JSONObject();
		}
		
		private JSONObject channelRecv(int channelId, int amount) {
			System.out.printf("channelRecv( %s, %s)\n",  channelId, amount );
			TestChannelUser user = chabuChannelUsers.get(channelId);
			user.addRecvAmount(amount);
			return new JSONObject();
		}

		private JSONObject channelXmit(int channelId, int amount) {
			System.out.printf("channelXmit( %s, %s)\n", channelId, amount );
			TestChannelUser user = chabuChannelUsers.get(channelId);
			user.addXmitAmount(amount);
			return new JSONObject();
		}
		private JSONObject channelEnsureCompleted(int channelId) {
			System.out.printf("channelEnsureCompleted( %s )\n", channelId );
			TestChannelUser user = chabuChannelUsers.get(channelId);
			user.ensureCompleted();
			return new JSONObject();
		}
		private JSONObject channelState(int channelId) {
			TestChannelUser user = chabuChannelUsers.get(channelId);
			return user.getState();
		}
		private void close() {
			
			try {
				selector.close();
			} catch (IOException e) {
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
		
		public ChabuConnection(SocketChannel channel, SelectionKey key ) {
			super( channel, key );
		}

		private void xmitRequest(){
			addXmitRequest(this);
		}

		public void accept(SelectionKey t) {
			try{
				if( t.isReadable() || t.isWritable() ){
					resetWriteReq();
					chabu.handleChannel(channel);
				}
			}
			catch( IOException e ){
				throw new RuntimeException(e);
			}
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
		
		
		ServerSocketChannel sscCtrl = ServerSocketChannel.open();
		ServerSocketChannel sscTest = ServerSocketChannel.open();
		sscCtrl.configureBlocking(false);
		sscTest.configureBlocking(false);
		sscCtrl.bind(new InetSocketAddress(port));
		sscTest.bind(new InetSocketAddress(port+1));
		InetSocketAddress laddr = (InetSocketAddress)sscCtrl.getLocalAddress();
		System.out.println("LocalPort = " + laddr.getPort() );
		
		selector = Selector.open();
		sscCtrl.register( selector, SelectionKey.OP_ACCEPT );
		sscTest.register( selector, SelectionKey.OP_ACCEPT );
		
		
		while( selector.isOpen() ){


			synchronized (xmitRequestsPending) {
				for( AConnection c : xmitRequestsPending ){
					c.registerWriteReq();
				}
				xmitRequestsPending.clear();
			}
			
			selector.select(2000);
			System.out.println("nw server select");

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
				
                
                if( key.channel() == sscCtrl ){
					if( key.isAcceptable() ){
						SocketChannel channel = sscCtrl.accept();
						channel.configureBlocking(false);
						
						SelectionKey channelKey = channel.register( selector, SelectionKey.OP_READ );
						ControlConnection connection = new ControlConnection( channel, channelKey );
						channelKey.attach(connection);
					}
				}
                else if( key.channel() == sscTest ){
                	if( key.isAcceptable() ){
                		SocketChannel channel = sscTest.accept();
                		channel.configureBlocking(false);
                		
                		SelectionKey channelKey = channel.register( selector, SelectionKey.OP_READ );
                		ChabuConnection connection = new ChabuConnection( channel, channelKey );
                		channelKey.attach(connection);
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
}
