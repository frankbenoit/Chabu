package org.chabu.prot.v1.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;

import org.assertj.core.api.Assertions;
import org.chabu.TestByteChannel;
import org.chabu.TestUtils;
import org.chabu.prot.v1.ChabuErrorCode;
import org.chabu.prot.v1.ChabuException;
import org.chabu.prot.v1.ChabuSetupInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ChabuReceiverStartupTest {

	Setup setup;
	@Mock
	AbortMessage abortMessage;
	@Mock
	ChabuChannelImpl channel;
	@Mock
	Runnable completionListener;
	ArrayList<ChabuChannelImpl> channels = new ArrayList<>();
	private ChabuReceiverStartup sut;
	private TestByteChannel byteChannel;

	@Before public void 
	setup() {
		setup = new Setup(new ChabuSetupInfo(), abortMessage, null );
		sut = new ChabuReceiverStartup(abortMessage, setup, completionListener );
		channels.add( mock(ChabuChannelImpl.class ));
		byteChannel = new TestByteChannel( 1000, 1000 );
	}
	
	@Test public void
	recv_remote_setup() throws Exception {
		byteChannel.putRecvData(
				"00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 " +
				"55 00 00 00 "+TestUtils.getChabuVersionAsHex()+"00 00 03 E8 12 34 56 78 " + 
				"00 00 00 03 41 42 43 00");
		sut.recv(byteChannel);

		assertThat(setup.getInfoRemote()).isEqualTo( new ChabuSetupInfo( 1000, 0x12345678 , "ABC" ));
	}	
	
	@Test public void
	recv_remote_setup_with_wrong_protocol_name() throws Exception {
		byteChannel.putRecvData(
				"00 00 00 28 77 77 00 F0 00 00 00 05 41 48 41 42 " +
						"55 00 00 00 "+TestUtils.getChabuVersionAsHex()+"00 00 03 E8 12 34 56 78 " + 
				"00 00 00 03 41 42 43 00");
		sut.recv(byteChannel);
		sut.recv(byteChannel);
		verify(abortMessage).setPending(Matchers.eq(ChabuErrorCode.SETUP_REMOTE_CHABU_NAME.getCode()), Matchers.anyString());
	}	
	
	@Test public void
	recv_remote_setup_with_wrong_protocol_major_version() throws Exception {
		byteChannel.putRecvData(
				"00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 " +
						"55 00 00 00 FF FF 00 00 00 00 03 E8 12 34 56 78 " + 
				"00 00 00 03 41 42 43 00");
		sut.recv(byteChannel);
		sut.recv(byteChannel);
		verify(abortMessage).setPending(Matchers.eq(ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION.getCode()), Matchers.anyString());
	}	
	
	@Test public void
	recv_remote_setup_with_single_bytes() throws Exception {
		String data = "00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 " +
				"55 00 00 00 "+TestUtils.getChabuVersionAsHex()+"00 00 03 E8 12 34 56 78 " + 
		"00 00 00 03 41 42 43 00 ";
		
		while( !data.isEmpty() ){
			String singleData = data.substring(0, 3);
			data = data.substring(3);
			byteChannel.putRecvData( singleData );
			sut.recv(byteChannel);
		}
		
		assertThat(setup.getInfoRemote()).isEqualTo( new ChabuSetupInfo( 1000, 0x12345678 , "ABC" ));
	}
	
	@Test public void
	recv_remote_setup_with_too_long_protocol_name() throws Exception {
		String data = "00 00 00 2C 77 77 00 F0 00 00 00 09 43 48 41 42 " +
				"55 00 00 00 00 00 00 00 "+TestUtils.getChabuVersionAsHex()+"00 00 03 E8 12 34 56 78 " + 
				"00 00 00 03 41 42 43 00 ";
		byteChannel.putRecvData( data );
		Assertions.assertThatThrownBy(()->{
			sut.recv(byteChannel);
		})
		.isInstanceOf(ChabuException.class)
		.hasMessageContaining("exceeds max allowed length");
		
	}
	
	@Test public void
	recv_remote_setup_with_too_long_appl_protocol_name() throws Exception {
		String data = "00 00 00 60 77 77 00 F0 00 00 00 05 43 48 41 42 " +
				"55 00 00 00 "+TestUtils.getChabuVersionAsHex()+"00 00 03 E8 12 34 56 78 " + 
				"00 00 00 39 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 ";
		byteChannel.putRecvData( data );
		Assertions.assertThatThrownBy(()->{
			sut.recv(byteChannel);
		})
		.isInstanceOf(ChabuException.class)
		.hasMessageContaining("exceeds max allowed length");
		
	}
	
	@Test public void
	recv_remote_setup_with_appl_protocol_name_not_fully_contained_in_recv_buffer() throws Exception {
		String data = "00 00 00 30 77 77 00 F0 00 00 00 05 43 48 41 42 " +
				"55 00 00 00 "+TestUtils.getChabuVersionAsHex()+"00 00 03 E8 12 34 56 78 " + 
				"00 00 00 30 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 ";
		byteChannel.putRecvData( data );
		Assertions.assertThatThrownBy(()->{
			sut.recv(byteChannel);
		})
		.isInstanceOf(ChabuException.class)
		.hasMessageContaining("exceeds packet length");
		
	}
	
	@Test public void
	recv_accept() throws Exception {
		byteChannel.putRecvData(
				"00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 " +
				"55 00 00 00 "+TestUtils.getChabuVersionAsHex()+"00 00 03 E8 12 34 56 78 " + 
				"00 00 00 03 41 42 43 00 " + 
				"00 00 00 08 77 77 00 E1 ");
		sut.recv(byteChannel);
		assertThat(setup.isRemoteAcceptReceived()).isTrue();
		verify(completionListener).run();
	}
	
	@Test public void
	recv_accept_without_setup() throws Exception {
		String data = "00 00 00 08 77 77 00 E1 ";
		byteChannel.putRecvData( data );
		sut.recv(byteChannel);
		verify(abortMessage).setPending(
				Matchers.eq(ChabuErrorCode.PROTOCOL_ACCEPT_WITHOUT_SETUP.getCode()), 
				Matchers.contains(""));
	}
	
	@Test public void
	recv_abort() throws Exception {
		String data = "00 00 00 08 77 77 00 D2 00 02 00 00 00 00 00 03 41 42 43 00 ";
		byteChannel.putRecvData( data );
		Assertions.assertThatThrownBy(()->{
			sut.recv(byteChannel);
		})
		.isInstanceOf(ChabuException.class)
		.hasMessageContaining("ABC");

	}
	
}
