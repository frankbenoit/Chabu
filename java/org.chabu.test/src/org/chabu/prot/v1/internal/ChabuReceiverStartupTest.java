package org.chabu.prot.v1.internal;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;

import org.chabu.TestByteChannel;
import org.chabu.TestUtils;
import org.chabu.prot.v1.ChabuSetupInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ChabuReceiverStartupTest {

	@Test
	public void start() throws Exception {
		Setup setup = mock(Setup.class);
		ChabuReceiverStartup sut = new ChabuReceiverStartup(null, setup, ()->{});
		ArrayList<ChabuChannelImpl> channels = new ArrayList<>();
		channels.add( mock(ChabuChannelImpl.class ));
		TestByteChannel byteChannel = new TestByteChannel( 1000, 1000 );
		byteChannel.putRecvData(
				"00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 " +
				"55 00 00 00 "+TestUtils.getChabuVersionAsHex()+"00 00 03 E8 12 34 56 78 " + 
				"00 00 00 03 41 42 43 00");
		sut.recv(byteChannel);

		verify(setup).setRemote(eq( new ChabuSetupInfo( 1000, 0x12345678 , "ABC" )));
	}
	
}
