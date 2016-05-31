package org.chabu.nwtest.client;

import java.io.FileWriter;
import java.util.ArrayList;

import org.chabu.prot.v1.Chabu;
import org.chabu.prot.v1.ChabuBuilder;
import org.json.JSONObject;

public class TestClient {

	NetworkThread runner;
	int bandwidth = 1_000_000;

	public static void main(String[] args) throws Exception {
		int port = 15000;
		TestClient client = new TestClient();
		client.run(port);
	}

	public TestClient() {
		
	}
	void remoteBuilderStart( int applicationVersion, String applicationName, int recvBufferSz, int priorityCount) {
		runner.ctrlXfer( new JSONObject()
		.put("Command", "ChabuBuilder.start")
		.put("ApplicationVersion", applicationVersion)
		.put("ApplicationName", applicationName)
		.put("RecvBuffer", recvBufferSz)
		.put("PriorityCount", priorityCount));
	}

	void remoteBuilderAddChannel(int channel, int priority, int recvBufferSz, int xmitBufferSz) {
		runner.ctrlXfer( new JSONObject()
		.put("Command", "ChabuBuilder.addChannel")
		.put("Channel", channel)
		.put("Priority", priority)
		.put("RecvBuffer", recvBufferSz)
		.put("XmitBuffer", xmitBufferSz));
	}

	void remoteBuilderBuild() {
		runner.ctrlXfer( new JSONObject()
		.put("Command", "ChabuBuilder.build"));
	}

	void remoteChabuClose() {
		runner.ctrlXfer( new JSONObject()
		.put("Command", "Chabu.close"));
	}

	JSONObject remoteChabuGetState() {
		return runner.ctrlXfer( new JSONObject()
				.put("Command", "Chabu.getState"));
	}
	
	void remoteChannelRecv(int channelId, int amount) {
		runner.ctrlXfer( new JSONObject()
		.put("Command", "Channel.recv")
		.put("Channel", channelId)
		.put("Amount", amount));
	}

	void remoteChannelXmit(int channelId, int amount) {
		runner.ctrlXfer( new JSONObject()
		.put("Command", "Channel.xmit")
		.put("Channel", channelId)
		.put("Amount", amount));
	}
	void remoteChannelEnsureCompleted(int channelId) {
		runner.ctrlXfer( new JSONObject()
				.put("Command", "Channel.ensureCompleted")
				.put("Channel", channelId));
	}
	JSONObject remoteChannelState(int channelId) {
		return runner.ctrlXfer( new JSONObject()
		.put("Command", "Channel.state")
		.put("Channel", channelId));
	}
	void remoteCloseAll() {
		runner.ctrlXfer( new JSONObject()
				.put("Command", "Close"), true );
	}

	
	Chabu chabu;
	ArrayList<ChannelUser> channelUsers = new ArrayList<>();
	
	
	
	private void run(int port) throws InterruptedException {
		try{
			runner = new NetworkThread(port);
			connect();
			System.out.println("connect completed");

			remoteBuilderStart(0x1213, "ABC", 1400, 3);
			remoteBuilderAddChannel( 0, 0, 100000, 100000 );
			remoteBuilderAddChannel( 1, 1, 100000, 100000 );
			remoteBuilderAddChannel( 2, 1, 100000, 100000 );
			remoteBuilderAddChannel( 3, 1, 100000, 100000 );
			remoteBuilderAddChannel( 4, 1, 100000, 100000 );
			remoteBuilderAddChannel( 5, 1, 100000, 100000 );
			remoteBuilderAddChannel( 6, 2, 100000, 100000 );
			remoteBuilderBuild();


			channelUsers.clear();
			channelUsers.add( new ChannelUser() );
			channelUsers.add( new ChannelUser() );
			channelUsers.add( new ChannelUser() );
			channelUsers.add( new ChannelUser() );
			channelUsers.add( new ChannelUser() );
			channelUsers.add( new ChannelUser() );
			channelUsers.add( new ChannelUser() );

//			ChannelUser ch0 = channelUsers.get(0);
			chabu = ChabuBuilder
					.start( 0x13, "AAA", 1400, 3 )
					.addChannel(0, 100000, 0, channelUsers.get(0) )
					.addChannel(1, 100000, 0, channelUsers.get(1) )
					.addChannel(2, 100000, 1, channelUsers.get(2) )
					.addChannel(3, 100000, 1, channelUsers.get(3) )
					.addChannel(4, 100000, 1, channelUsers.get(4) )
					.addChannel(5, 100000, 1, channelUsers.get(5) )
					.addChannel(6, 100000, 2, channelUsers.get(6) )
					.addXmitRequestListener( runner::setTestWriteRequest )
					.build();
			runner.setChabu(chabu);
			
			msrBandwith();
			
			highTraffic();
			
			
			System.out.println( remoteChannelState(0) );
			remoteChabuClose();
			remoteCloseAll();

		} finally {
			runner.interrupt();
			runner.thread.join();
			NwtUtil.closeLog();
		}
	}

	private void highTraffic() {
		long tsStart = System.nanoTime();
		long tsEnd = Math.round( 10 * 1e9) + tsStart;

		NwtUtil.log("high Traffic ----------------------" );
		int loop = 0;
		try( FileWriter fw = new FileWriter("bandwidth.log.txt") ){
			while( tsEnd - System.nanoTime() > 0 ){

				loop++;
				System.out.println("Loop: "+loop);
				for( int i = 0; i < channelUsers.size(); i++ ){
					ChannelUser ch = channelUsers.get(i);
					
					fw.append(Long.toString(System.currentTimeMillis()));
					fw.append(",");
					fw.append(Long.toString(ch.getRecvStreamPosition()));
					fw.append(",");
					fw.append(Long.toString(ch.getXmitStreamPosition()));
					
					int recvStart = bandwidth / channelUsers.size();
					int recvSpace = bandwidth / channelUsers.size();
					int xmitStart = bandwidth / channelUsers.size();
					int xmitSpace = bandwidth / channelUsers.size();
					
					remoteChannelRecv(i, xmitSpace);
					remoteChannelXmit(i, recvStart);
					ch.addRecvAmount(recvSpace);
					ch.addXmitAmount(xmitStart);
					
					
				}
				fw.append("\r\n");
				
				Thread.sleep(500);
				
			}
		
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	void msrBandwith(){
		ChannelUser ch0 = channelUsers.get(0);
		
		for( int testSize = 500_000; testSize < 0x7FFF_FFFF; testSize <<= 1 ){
			ch0.addRecvAmount(testSize);
			ch0.addXmitAmount(testSize);
			remoteChannelXmit( 0, testSize );
			remoteChannelRecv( 0, testSize );
			
			System.out.printf("TestSize %10s time=%5s\n", testSize, System.currentTimeMillis()% 10_000 );
			NwtUtil.log("TestSize %10s----------------------", testSize );
			
			long ts = System.nanoTime();
			int max = 3000;
			while( ch0.hasPending() && max-- > 0 ){
				pause( 1 );
			}
			ts = System.nanoTime() - ts;
			ts /= 1000;

			if( max <= 0 ){
				System.err.println("Timeout "+ch0);
				NwtUtil.log("Timeout  %s", ch0 );
				NwtUtil.log("%s", remoteChabuGetState() );
				break;
			}
			pause( 10 );
//			remoteChannelEnsureCompleted( 0 );
//			ch0.ensureCompleted();
			
			if( ts > 250_000 )
			{
				bandwidth = (int)Math.round(testSize / (ts*1e-6));
				System.out.printf("time: %d.%03dms size=%10d bandwidth %dKb/s\n", ts / 1000, ts % 1000, testSize, bandwidth/1000 );
				System.out.printf("xmit: %s %s recv: %s %s\n", 
						ch0.getXmitStreamPosition(),
						ch0.getXmitPending(),
						ch0.getRecvStreamPosition(),
						ch0.getRecvPending());
				if( ts > 1000_000 ) {
					break;
				}
			}
		}
	}

	
	private void pause(int durationMs) {
		try {
			Thread.sleep(durationMs);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private void connect() {
		new Thread(runner).start();
		synchronized(runner){
			while( !runner.isConnectionCompleted() ){
				runner.doWait();
				System.out.println("client wait connect");
			}
		}
	}

}
