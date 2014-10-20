package chabu.user;

import java.io.IOException;

import chabu.Channel;
import chabu.IChannel;

import org.junit.Test;

public class MctcpStreamTest {

	
	
	@Test
	public void testMctcp() throws IOException {
		ChannelTester ct = ChannelTester.createMctcp(
				new IChannel[]{
						new Channel( 10, 10 ),
				},
				new IChannel[]{
						new Channel( 10, 10 ),
				});
		
		ct.setStreamAmount( 1, ChannelType.ClientRx, 100_000000L );
		ct.setStreamAmount( 1, ChannelType.ServerTx, 100_000000L );
		ct.setStreamAmount( 1, ChannelType.ClientTx, 100_000000L );
		ct.setStreamAmount( 1, ChannelType.ServerRx, 100_000000L );
		ct.addStreamEndpointPause( 1, ChannelType.ServerRx, 1_000000L, 500 );
		ct.runTest();
	}
	
}
