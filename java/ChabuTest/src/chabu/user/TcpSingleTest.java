package chabu.user;

import java.io.IOException;

import org.junit.Test;

public class TcpSingleTest {

	public static final long AMOUNT = (long)( TestConstants.BANDWIDTH * 2.0 );
	
	@Test
	public void testServerTx() throws IOException {
		ChannelTester ct = ChannelTester.createTcp();
		ct.setStreamAmount( 1, ChannelType.ClientRx, AMOUNT );
		ct.setStreamAmount( 1, ChannelType.ServerTx, AMOUNT );
		ct.runTest();
	}

	@Test
	public void testServerRx() throws IOException {
		ChannelTester ct = ChannelTester.createTcp();
		ct.setStreamAmount( 1, ChannelType.ClientTx, AMOUNT );
		ct.setStreamAmount( 1, ChannelType.ServerRx, AMOUNT );
		ct.runTest();
	}
	
	@Test
	public void testDuplex() throws IOException {
		ChannelTester ct = ChannelTester.createTcp();
		ct.setStreamAmount( 1, ChannelType.ClientRx, AMOUNT/2 );
		ct.setStreamAmount( 1, ChannelType.ServerTx, AMOUNT/2 );
		ct.setStreamAmount( 1, ChannelType.ClientTx, AMOUNT/2 );
		ct.setStreamAmount( 1, ChannelType.ServerRx, AMOUNT/2 );
		ct.runTest();
	}
	
	@Test
	public void testPausingServerTx() throws IOException {
		ChannelTester ct = ChannelTester.createTcp();
//		ct.print = true;
		ct.setStreamAmount( 1, ChannelType.ClientRx, AMOUNT/2 );
		ct.setStreamAmount( 1, ChannelType.ServerTx, AMOUNT/2 );
		ct.addStreamEndpointPause( 1, ChannelType.ServerTx, AMOUNT/4, 500 );
		ct.runTest();
	}
	
	@Test
	public void testPausingClientRx() throws IOException {
		ChannelTester ct = ChannelTester.createTcp();
//		ct.print = true;
		ct.setStreamAmount( 1, ChannelType.ClientRx, AMOUNT/2 );
		ct.setStreamAmount( 1, ChannelType.ServerTx, AMOUNT/2 );
		ct.addStreamEndpointPause( 1, ChannelType.ClientRx, AMOUNT/4, 500 );
		ct.runTest();
	}
	
	
}
