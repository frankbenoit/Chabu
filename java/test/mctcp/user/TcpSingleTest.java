package mctcp.user;

import java.io.IOException;

import org.junit.Test;

public class TcpSingleTest {

	
	
	@Test
	public void testTcp() throws IOException {
		ChannelTester ct = ChannelTester.createTcp();
		ct.setStreamAmount( 1, ChannelType.ClientRx, 100_000000L );
		ct.setStreamAmount( 1, ChannelType.ServerTx, 100_000000L );
		ct.setStreamAmount( 1, ChannelType.ClientTx, 100_000000L );
		ct.setStreamAmount( 1, ChannelType.ServerRx, 100_000000L );
		ct.addStreamEndpointPause( 1, ChannelType.ServerRx, 1_000000L, 500 );
		ct.runTest();
	}

	
}
