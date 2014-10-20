package chabu.user;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.LinkedList;

import chabu.IChannel;
import chabu.INetworkUser;
import chabu.MctcpConnector;
import chabu.Utils;

public class ChannelTester {

	boolean print = false;
	static class WakeupEvent implements Comparable<WakeupEvent> {
		
		private StreamEndpoint ep;
		private long    millis;
		private boolean isTx;

		public WakeupEvent(StreamEndpoint ep, int millis, boolean isTx ) {
			this.ep = ep;
			// TODO Auto-generated constructor stub
			this.millis = System.currentTimeMillis() + millis;
			this.isTx = isTx;
		}
		@Override
		public int compareTo(WakeupEvent o) {
			return Long.compare(millis, o.millis);
		}
	}
	static class PausingIndex implements Comparable<PausingIndex>{
		long  idx;
		int   millis;
		@Override
		public int compareTo(PausingIndex o) {
			return Long.compare( idx, o.idx );
		}
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
		final LinkedList<PausingIndex> pausingIndices = new LinkedList<>();
		IChannel channel;
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
	private final LinkedList<WakeupEvent> events = new LinkedList<>();
	private INetworkUser server;
	private INetworkUser client;
	private final LinkedList<StreamEndpoint> allEndpoints = new LinkedList<>();
	
	private void channelHandler( IChannel ch ){
		handleEndpoint( ch, true  );
		handleEndpoint( ch, false );
	}

	private void handleEndpoint(IChannel ch, boolean isTx ) {
		
		ChState cs = (ChState)ch.getUserData();
		StreamEndpoint ep = isTx ? cs.tx            : cs.rx; 
		ByteBuffer buffer = isTx ? ch.txGetBuffer() : ch.rxGetBuffer();
		String action     = isTx ? "Send"           : "Recv";
		
		if( !ep.closed && !ep.paused ){
			long pauseIdx = Long.MAX_VALUE;
			if( !ep.pausingIndices.isEmpty() ){
				pauseIdx = ep.pausingIndices.getFirst().idx;
			}
			if( pauseIdx > ep.amount ){
				pauseIdx = ep.amount;
			}
			if( ep.idx < pauseIdx ){
				long diff = pauseIdx - ep.idx;
				int lim = buffer.limit();
				if( buffer.remaining() > diff ){
					buffer.limit( buffer.position() + (int)diff );
				}
				
				long sz = isTx ? 
						ep.ss.tdf.copySendData (buffer) : 
						ep.ss.tdf.checkRecvData(buffer) ;
						
				if( !isTx ) {
					buffer.limit(lim);
				}
				ep.idx += sz;
				if( sz > 0 && print ){
					System.out.printf("%s %s %6d rem:%8d buf:%8d\n", 
							ep.name, action, sz, ep.amount-ep.idx, buffer.remaining());
				}
				
				if( !ep.pausingIndices.isEmpty() && ep.idx == ep.pausingIndices.getFirst().idx ){
					PausingIndex pi = ep.pausingIndices.removeFirst();
					if(print) {
						System.out.printf("%s pausing for %sms\n", ep.name, pi.millis );
					}
					ep.paused = true;
					synchronized( this ){
						events.add( new WakeupEvent( ep, pi.millis, isTx ) );
						Collections.sort( events );
					}
				}
				else {
					if( isTx ){
						ch.registerWaitForWrite();
					}
					else {
						ch.registerWaitForRead();
					}
				}
			}
			if( ep.idx >= ep.amount ){
				if(print) {
					System.out.printf("%s %s completed ----------------------- %d\n", ep.name, action, buffer.position() );
				}
				ep.closed = true;
			}
		}

		if( cs.rx.closed && cs.tx.closed && ( !isTx || buffer.position() == 0)){
			ch.close();
			if(print) {
				System.out.printf("%s %s completed ----------------------- close\n", ep.name, action );
			}
			synchronized(ChannelTester.this){
				ChannelTester.this.notifyAll();
			}
		}
		else if( isTx ){
			ch.registerWaitForWrite();
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

	private ChannelTester(INetworkUser server, INetworkUser client, IChannel[] serverChannels, IChannel[] clientChannels){
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

			css.tx.channel = serverChannels[i];
			css.rx.channel = serverChannels[i];
			csc.tx.channel = clientChannels[i];
			csc.rx.channel = clientChannels[i];
			
			allEndpoints.add( sst.endPointRx );
			allEndpoints.add( sst.endPointTx );
			allEndpoints.add( ssr.endPointRx );
			allEndpoints.add( ssr.endPointTx );
			
			css.tx.name = String.format("S[%d].tx", i );
			css.rx.name = String.format("S[%d].rx", i );
			csc.tx.name = String.format("C[%d].tx", i );
			csc.rx.name = String.format("C[%d].rx", i );
			
			sst.tdf = td.createFlow(String.format("Ch%d>", i+1));
			ssr.tdf = td.createFlow(String.format("Ch%d<", i+1));
			
			serverChannels[i].setChannelId(i);
			clientChannels[i].setChannelId(i);
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
		PausingIndex pi = new PausingIndex();
		pi.idx = byteIdx;
		pi.millis = pauseMs;
		ep.pausingIndices.add(pi);
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
	public void runTest() {

		for( StreamEndpoint ep : allEndpoints ){
			Collections.sort( ep.pausingIndices );
		}
		
		server.start();
		client.start();
		
		synchronized ( this ){
			while( !isAllCompleted() ){
				while( !events.isEmpty() && System.currentTimeMillis() > events.getFirst().millis ){
					WakeupEvent ev = events.removeFirst();
					ev.ep.paused = false;
					if( ev.isTx ){
						ev.ep.channel.registerWaitForWrite();
					}
					else {
						ev.ep.channel.registerWaitForRead();
					}
				}
				Utils.waitOn(this, 500 );
			}
		}
		server.forceShutDown();
		client.forceShutDown();
		ChState cs = (ChState)server.getChannel(1).getUserData();
		if(print) cs.tx.ss.tdf.printStats();
		if(print) cs.rx.ss.tdf.printStats();
	}

	
}
