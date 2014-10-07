package mctcp.user;

import java.io.IOException;
import java.util.TreeMap;

import mctcp.IChannel;
import mctcp.INetworkConnector;
import mctcp.MctcpConnector;
import mctcp.Utils;

public class ChannelTester {

	final boolean print = false;
	static class WakeupEvent {
		long           nanoTime;
		StreamEndpoint endPoint;
	}
	static class StreamEndpoint {
		public StreamEndpoint(StreamState ss) {
			this.ss = ss;
		}
		boolean paused;
		long amount;
		long idx;
		boolean closed = false;
		final StreamState ss;
		String name;
	}
	
	static class StreamState {
		TestDataFlow tdf;
		StreamEndpoint endPointRx;
		StreamEndpoint endPointTx;
		public StreamState(){
			endPointRx = new StreamEndpoint(this);
			endPointTx = new StreamEndpoint(this);
		}
	}
	static class ChState {
		StreamEndpoint rx;
		StreamEndpoint tx;
	}
	
	private final TestData td;
	private final IChannel[] serverChannels;
	private final IChannel[] clientChannels;
	private final TreeMap<Long, WakeupEvent> events = new TreeMap<>();
	private INetworkConnector server;
	private INetworkConnector client; 
	
	private void channelHandler( IChannel ch ){
		handleRecv(ch);
		handleSend(ch);
	}

	private void handleSend(IChannel ch) {
		ChState cs = (ChState)ch.getUserData();

		StreamEndpoint ep = cs.tx;
		// sending
		if( !ep.closed ){
			if( ep.idx < ep.amount ){
				long diff = ep.amount - ep.idx;
				if( ch.txGetBuffer().remaining() > diff ){
					ch.txGetBuffer().limit( ch.txGetBuffer().position() + (int)diff );
				}
				long sz = ep.ss.tdf.copySendData(ch.txGetBuffer());
				ep.idx += sz;
				if( sz > 0 ){
					if(print) System.out.printf("%s Send %6d rem:%8d\n", ep.name, sz, ep.amount-ep.idx);
				}
				ch.registerWaitForWrite();
			}
			if( ep.idx >= ep.amount ){
				if(print) System.out.printf("%s Send completed ----------------------- %d\n", ep.name, ch.txGetBuffer().position() );
				ep.closed = true;
			}
		}

		if( cs.rx.closed && cs.tx.closed && ch.txGetBuffer().position() == 0){
			ch.close();
			if(print) System.out.printf("%s Send completed ----------------------- close\n", ep.name );
			synchronized(ChannelTester.this){
				ChannelTester.this.notifyAll();
			}
		}
		else {
			ch.registerWaitForWrite();
		}
	}

	private void handleRecv(IChannel ch) {
		ChState cs = (ChState)ch.getUserData();
		StreamEndpoint ep = cs.rx;
//		System.out.printf("%s Recv %s\n", ep.name, ep.closed );
		if( !ep.closed ){
			long sz = ep.ss.tdf.checkRecvData(ch.rxGetBuffer());
			ep.idx += sz;
			if( sz > 0 ){
				if(print) System.out.printf("%s Recv %6d rem:%8d buf:%d\n", ep.name, sz, ep.amount-ep.idx, ch.rxGetBuffer().remaining());
			}
			if( ep.idx >= ep.amount ){
				if(print) System.out.printf("%s Recv completed -----------------------\n", ep.name );
				ep.closed = true;
			}
		}
		
		if( cs.rx.closed && cs.tx.closed ){
			if(print) System.out.printf("%s Recv completed ----------------------- closed\n", ep.name );
			ch.close();
			synchronized(ChannelTester.this){
				ChannelTester.this.notifyAll();
			}
		}
	}

	public static ChannelTester createTcp() throws IOException {
		final TcpServer s = TcpServer.startServer(2000);
		final TcpServer c = TcpServer.startClient("localhost", 2000);
		ChannelTester res = new ChannelTester(
				s, c,
				new IChannel[]{
						s.getChannel(1),
				}, 
				new IChannel[]{
						c.getChannel(1),
				});
		s.getChannel(1).setHandler( res::channelHandler );
		c.getChannel(1).setHandler( res::channelHandler );
		return res;
	}
	
	public static ChannelTester createMctcp(IChannel[] serverChannels, IChannel[] clientChannels) throws IOException {
		final MctcpConnector s = MctcpConnector.startServer(2000);
		final MctcpConnector c = MctcpConnector.startClient("localhost", 2000);
		ChannelTester res = new ChannelTester( c, s, serverChannels, clientChannels );
		s.setChannelHandler( res::channelHandler );
		c.setChannelHandler( res::channelHandler );
		return res;
	}

	private ChannelTester(INetworkConnector server, INetworkConnector client, IChannel[] serverChannels, IChannel[] clientChannels){
		this.server = server;
		this.client = client;
		this.serverChannels = serverChannels;
		this.clientChannels = clientChannels;
		td = new TestData();
		Utils.ensure( serverChannels.length == clientChannels.length, "not equal count of channels" );
		for( int i = 0; i < serverChannels.length; i++ ){
			
			ChState css = new ChState();
			ChState csc = new ChState();
			StreamState sst = new StreamState();
			StreamState ssr = new StreamState();
			
			css.tx = sst.endPointTx;
			css.rx = ssr.endPointRx;
			csc.tx = ssr.endPointTx;
			csc.rx = sst.endPointRx;
			
			css.tx.name = String.format("S[%d].tx", i );
			css.rx.name = String.format("S[%d].rx", i );
			csc.tx.name = String.format("C[%d].tx", i );
			csc.rx.name = String.format("C[%d].rx", i );
			
			sst.tdf = td.createFlow(String.format("Ch%d>", i+1));
			ssr.tdf = td.createFlow(String.format("Ch%d<", i+1));
			
			serverChannels[i].setUserData(css);
			clientChannels[i].setUserData(csc);
		}
		
	}
	
	public void setStreamAmount( int channelId, ChannelType channelType, long amount ){
		IChannel ch[] = channelType.isServer ? serverChannels : clientChannels;
		ChState cs = (ChState)ch[channelId-1].getUserData();
		StreamEndpoint ep = channelType.isTx ? cs.tx : cs.rx;
		ep.amount = amount;
	}
	
	public void addStreamEndpointPause( int channelId, ChannelType channelType, long byteIdx, int pauseMs ){
		IChannel ch[] = channelType.isServer ? serverChannels : clientChannels;
		ChState cs = (ChState)ch[channelId-1].getUserData();
		StreamEndpoint ep = channelType.isTx ? cs.tx : cs.rx;

		//TODO ...
	}
	private boolean isAllCompleted(){
		for( IChannel ch : serverChannels ){
			if( !ch.isClosed() ) {
				return false;
			}
		}
		for( IChannel ch : clientChannels ){
			if( !ch.isClosed() ) {
				return false;
			}
		}
		return true;
	}
	public synchronized void runTest() {
		
		server.start();
		client.start();
		
		while( !isAllCompleted() ){
//			System.out.println("main wait");
			Utils.waitOn(this);
		}

		ChState cs = (ChState)server.getChannel(1).getUserData();
		if(print) cs.tx.ss.tdf.printStats();
		if(print) cs.rx.ss.tdf.printStats();
	}

	
}
