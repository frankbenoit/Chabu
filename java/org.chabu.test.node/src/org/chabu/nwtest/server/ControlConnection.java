package org.chabu.nwtest.server;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.chabu.nwtest.Parameter;
import org.chabu.nwtest.ParameterValue;
import org.chabu.nwtest.ParameterWithChilds;
import org.chabu.nwtest.XferItem;
import org.chabu.nwtest.XferItem.Category;
import org.chabu.prot.v1.Chabu;
import org.chabu.prot.v1.ChabuBuilder;
import org.chabu.prot.v1.ChabuChannel;

class ControlConnection extends AConnection {

	Chabu chabu;
	private ArrayList<TestChannelUser> chabuChannelUsers = new ArrayList<>( 20 );
	ByteBuffer recvBuffer = ByteBuffer.allocate(10000);
	ByteBuffer xmitBuffer = ByteBuffer.allocate(1000);
	ChabuBuilder builder;
	String firstError = null;
	private TestServerPort testServer;
	private Marshaller marshaller;
	private Unmarshaller unmarshaller;

	public ControlConnection( TestServerPort testServer, SocketChannel channel, SelectionKey key ) throws JAXBException {
		super( channel, key );
		this.testServer = testServer;
		JAXBContext jaxbContext = JAXBContext.newInstance(XferItem.class, Parameter.class, ParameterValue.class, ParameterWithChilds.class);
		marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		unmarshaller = jaxbContext.createUnmarshaller();
		recvBuffer.limit(0);
	}

	public void accept(SelectionKey t) throws Exception {
		try{
			if( !t.isValid() ){

			}
			if( t.isReadable() ){
				recvBuffer.compact();
				channel.read(recvBuffer);
				recvBuffer.flip();
				//System.out.printf("channel.read: %s%n", recvBuffer);
			}

			try{
				if( isRequestComplete() ){
					
					String reqStr = getRequest();
					//System.out.printf("recved req xml: >>%s<<%n", reqStr );
					XferItem req = (XferItem)unmarshaller.unmarshal(new StringReader( reqStr ));
					System.out.println("recved req name: "+ req.getName() );
					
					XferItem resp = process( req );
					if( resp == null ){
						// closing, so go out without writing
						System.err.println("process returned null!!!");
						return;
					}
					StringWriter sw = new StringWriter();
					marshaller.marshal(resp, sw);
					String respStr = sw.toString();
					//System.out.println("has resp: "+ respStr );
					ByteBuffer encodedResp = StandardCharsets.UTF_8.encode(CharBuffer.wrap(respStr));
					xmitBuffer.putInt(encodedResp.remaining());
					xmitBuffer.put(encodedResp);
				}
			}
			catch( RuntimeException e ){
				e.printStackTrace();
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

	private String getRequest() {
		int size = recvBuffer.getInt();
		//System.out.println("ControlConnection.getRequest()"+size);
		int oldLimit = recvBuffer.limit();
		try{
			recvBuffer.limit( recvBuffer.position() + size );
			//System.out.printf("recved req xml: >>%s<<%n", new HexString( recvBuffer.array(), 0, size ).toString().substring(0, Math.min(40, size/3)) );
			return StandardCharsets.UTF_8.decode(recvBuffer).toString().trim();
		}
		finally {
			recvBuffer.limit(oldLimit);
		}
	}

	private boolean isRequestComplete() {
		if( recvBuffer.remaining() >= 4 ){
			int size = recvBuffer.getInt( recvBuffer.position() );
			if( recvBuffer.remaining() >= size + 4 ){
				return true;
			}
		}
		return false;
	}

	private XferItem process(XferItem req) {
		int callIndex = req.getCallIndex();
		String name = req.getName();
		XferItem resp = null;
		
		switch(req.getName()){

		case "Setup":
			resp = setup( 
					req.getValueString("ChabuTestDirectorVersion"   ), 
					req.getValueString("NodeLabel"   ));
			break;
		case "ChabuBuilder.start":
			resp = builderStart( 
					req.getValueInt("ApplicationVersion"), 
					req.getValueString("ApplicationProtocolName"   ), 
					req.getValueInt   ("RecvPacketSize"        ), 
					req.getValueInt   ("PriorityCount"     ));
			break;
		case "ChabuBuilder.addChannel":
			resp = builderAddChannel( 
					req.getValueInt("Channel"   ), 
					req.getValueInt("Priority"  ));
			break;
		case "ChabuBuilder.build":
			resp = builderBuild();
			break;
		case "Chabu.close":
			resp = chabuClose();
			break;
		case "GetState":
			resp = chabuGetState();
			break;
		case "Channel.recv":
			resp = channelRecv( 
					req.getValueInt("Channel"), 
					req.getValueInt("Amount" ));
			break;
		case "Channel.xmit":
			resp = channelXmit( 
					req.getValueInt("Channel"), 
					req.getValueInt("Amount" ));
			break;
		case "Channel.ensureCompleted":
			resp = channelEnsureCompleted( 
					req.getValueInt("Channel"));
			break;
		case "Channel.state":
			resp = channelState( 
					req.getValueInt("Channel"));
			break;
		case "Connect":
			testServer.connectSync(req.getValueString("HostName"), req.getValueInt("Port"));
			resp = new XferItem();
			break;
		case "Close":
			testServer.close();
			resp = new XferItem();
			break;
		case "ExpectClose":
			testServer.expectClose();
			resp = new XferItem();
			break;
		case "EnsureClosed":
			testServer.ensureClosed();
			resp = new XferItem();
			break;
		default:
			resp = new XferItem();
			resp.setParameters(new Parameter[]{
					new ParameterValue("IsError", "1"),
					new ParameterValue("Message", "Unknown Command"),
			});
			break;
		}
		resp.setCategory(Category.RES);
		resp.setCallIndex(callIndex);
		resp.setName(name);
		resp.addParameter("NanoTime", System.nanoTime() );
		return resp;
	}

	private XferItem setup(String directoryVersion, String hostLabel) {
		System.out.printf("setup( %s, %s)\n",  directoryVersion, hostLabel );
		XferItem res = new XferItem();
		res.setParameters(new Parameter[]{
				new ParameterValue("Implementation", "Java"),
				new ParameterValue("ChabuProtocolVersion", ChabuBuilder.getChabuVersion() ),
		});
		return res;
	}

	private XferItem builderStart( int applicationVersion, String applicationProtocolName, int recvPacketSize, int priorityCount) {
		System.out.printf("builderStart( %s, %s, %s, %s)\n",  applicationVersion, applicationProtocolName, recvPacketSize, priorityCount );
		builder = ChabuBuilder.start( applicationVersion, applicationProtocolName, recvPacketSize, priorityCount, null);
		return new XferItem();
	}

	private XferItem builderAddChannel(int channel, int priority) {
		System.out.printf("builderAddChannel( %s, %s)\n",  channel, priority );
		chabuChannelUsers.ensureCapacity(channel+1);
		while( chabuChannelUsers.size() < channel+1 ){
			chabuChannelUsers.add(null);
		}
		chabuChannelUsers.set( channel, new TestChannelUser( this::errorReceiver ) );
		builder.addChannel( channel, priority, chabuChannelUsers.get(channel));
		return new XferItem();
	}

	private XferItem builderBuild() {
		System.out.printf("builderBuild()\n");
		chabu = builder.build();
		testServer.setChabu(chabu);
		builder = null;
		return new XferItem();
	}

	private XferItem chabuGetState() {
		System.out.printf("chabuGetState()\n");
		int channelCount = chabu.getChannelCount();
		Parameter[] channelInfo = new Parameter[ channelCount ];
		for( int channelId = 0; channelId < chabu.getChannelCount(); channelId++ ){
			ChabuChannel channel = chabu.getChannel(channelId);
			channelInfo[channelId] = new ParameterWithChilds(Integer.toString(channelId), new Parameter[]{
					new ParameterValue("recvPosition", channel.getRecvPosition()),
					new ParameterValue("recvLimit", channel.getRecvLimit()),
					new ParameterValue("xmitPosition", channel.getXmitPosition()),
					new ParameterValue("xmitLimit", channel.getXmitLimit()),
			});
		}
		XferItem xi = new XferItem();
		xi.setParameters(new Parameter[]{
				new ParameterValue("channelCount", channelCount),
				new ParameterWithChilds("channel", channelInfo),
				new ParameterValue("toString", chabu.toString())
		});
		return xi;
	}

	private XferItem chabuClose() {
		System.out.printf("chabuClose()%n");
		System.out.printf("Ch   | Recved   | Xmitted%n");
		System.out.printf("-----|----------|----------%n");
		for( TestChannelUser user : chabuChannelUsers ){
			System.out.printf("[% 2d] | % 8d | % 8d%n", user.getChannelId(), user.getSumRecved(), user.getSumXmitted());
		}
		System.out.printf("---------------------------%n");
		chabu = null;
		chabuChannelUsers.clear();
		return new XferItem();
	}

	private XferItem channelRecv(int channelId, int amount) {
		System.out.printf("channelRecv( %s, %s)\n",  channelId, amount );
		TestChannelUser user = chabuChannelUsers.get(channelId);
		user.addRecvAmount(amount);
		return new XferItem();
	}

	private XferItem channelXmit(int channelId, int amount) {
		System.out.printf("channelXmit( %s, %s)\n", channelId, amount );
		TestChannelUser user = chabuChannelUsers.get(channelId);
		user.addXmitAmount(amount);
		return new XferItem();
	}
	private XferItem channelEnsureCompleted(int channelId) {
		System.out.printf("channelEnsureCompleted( %s )\n", channelId );
		TestChannelUser user = chabuChannelUsers.get(channelId);
		user.ensureCompleted();
		return new XferItem();
	}
	private XferItem channelState(int channelId) {
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