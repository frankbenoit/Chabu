package org.chabu.nwtest.client;

import java.util.ArrayList;

import org.chabu.ChabuBuilder;
import org.chabu.IChabu;
import org.json.JSONObject;

public class TestClient {

	NetworkThread runner;

	public static void main(String[] args) {
		TestClient client = new TestClient();
		client.run(args);
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
				.put("Command", "Close"), false );
	}

	
	IChabu chabu;
	ArrayList<ChannelUser> channelUsers = new ArrayList<>();
	
	
	
	private void run(String[] args) {
		try{
			runner = new NetworkThread(51504);
			connect();
			System.out.println("connect completed");

			remoteBuilderStart(0x1213, "ABC", 0x100, 3);
			remoteBuilderAddChannel( 0, 2, 1000, 1000 );
			remoteBuilderBuild();


			channelUsers.clear();
			channelUsers.add( new ChannelUser() );

			chabu = ChabuBuilder
					.start( 0x13, "AAA", 0x100, 3 )
					.addChannel(0, 0x100, 0, channelUsers.get(0) )
					.addXmitRequestListener( runner::setTestWriteRequest )
					.build();
			runner.setChabu(chabu);
			
			channelUsers.get(0).addRecvAmount(20000);
			channelUsers.get(0).addXmitAmount(20000);
			remoteChannelXmit( 0, 20000 );
			remoteChannelRecv( 0, 20000 );

			pause( 1_000 );

			remoteChannelEnsureCompleted( 0 );
			channelUsers.get(0).ensureCompleted();

			remoteChabuClose();
			remoteCloseAll();

		} finally {
			runner.interrupt();
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
			}
		}
	}

}
