package org.chabu.nwtest.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import org.chabu.prot.v1.Chabu;
import org.chabu.prot.v1.ChabuBuilder;
import org.json.JSONException;
import org.json.JSONObject;

class ControlConnection extends AConnection {

	Chabu chabu;
	private ArrayList<TestChannelUser> chabuChannelUsers = new ArrayList<>( 20 );
	ByteBuffer recvBuffer = ByteBuffer.allocate(1000);
	ByteBuffer xmitBuffer = ByteBuffer.allocate(1000);
	ChabuBuilder builder;
	String firstError = null;
	private TestServerPort testServer;

	public ControlConnection( TestServerPort testServer, SocketChannel channel, SelectionKey key ) {
		super( channel, key );
		this.testServer = testServer;
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

					JSONObject resp = process( req );
					if( resp == null ){
						// closing, so go out without writing
						return;
					}
					xmitBuffer.put(StandardCharsets.UTF_8.encode(CharBuffer.wrap(resp.toString())));
				}
			}
			catch( JSONException e ){
				recvBuffer.position(0);
			}

			xmitBuffer.flip();
			channel.write(xmitBuffer);
			xmitBuffer.compact();
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
			testServer.close();
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
		testServer.setChabu(chabu);
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

	private void errorReceiver( String msg ){
		if( firstError == null ){
			System.out.println("Error: "+msg);
			firstError = msg;
		}
	}
}