package org.chabu.prot.v1.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
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
	
	private String getStringData( String text ){
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		int length = Utils.alignUpTo4(bytes.length);
		byte[] data = new byte[length+4];
		data[0] = (byte)(bytes.length >>> 24);
		data[1] = (byte)(bytes.length >>> 16);
		data[2] = (byte)(bytes.length >>>  8);
		data[3] = (byte)(bytes.length >>>  0);
		System.arraycopy(bytes, 0, data, 4, bytes.length );
		String res = TestUtils.toHexString(data, true);
		return res + " ";
	}
	private String getIntData( int v ){
		return String.format("%02X %02X %02X %02X ", 
				( v >>> 24 ) & 0xFF, 
				( v >>> 16 ) & 0xFF, 
				( v >>>  8 ) & 0xFF, 
				( v >>>  0 ) & 0xFF); 
	}
	@Test
	public void getStringDataIsGood() throws Exception {
		assertThat(getStringData("ABC")).isEqualTo("00 00 00 03 41 42 43 00 ");
	}
	@Test
	public void getSetupRecvDataIsGood() throws Exception {
		assertThat(getSetupRecvData("CHABU", Constants.PROTOCOL_VERSION, 1000, 0x12345678, "ABC"))
			.isEqualTo("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 " +
				"55 00 00 00 "+TestUtils.getChabuVersionAsHex()+"00 00 03 E8 12 34 56 78 " + 
				"00 00 00 03 41 42 43 00 ");
	}
	private String getSetupRecvData(String protocolName, int chabuProtVersion, int recvPacketSize, int applProtVersion, String applProtName ) {
		return getIntData(0x1C + Utils.alignUpTo4(applProtName.length()) + Utils.alignUpTo4(protocolName.length()) ) + 
				"77 77 00 F0 " + 
				getStringData( protocolName) + 
				getIntData(chabuProtVersion) + 
				getIntData(recvPacketSize) + 
				getIntData(applProtVersion) + 
				getStringData( applProtName );
	}	
	
	private String getSetupRecvData() {
		return getSetupRecvData("CHABU", Constants.PROTOCOL_VERSION, 1000, 0x12345678, "ABC");
	}	

	private String getAcceptData() {
		return "00 00 00 08 77 77 00 E1 ";
	}

	private void prepareSetupData() {
		byteChannel.putRecvData( getSetupRecvData());
	}

	
	@Test public void
	recv_remote_setup() throws Exception {
		prepareSetupData();
		sut.recv(byteChannel);
		assertThat(setup.getInfoRemote()).isEqualTo( new ChabuSetupInfo( 1000, 0x12345678 , "ABC" ));
	}

	@Test public void
	recv_remote_setup_with_wrong_protocol_name() throws Exception {
		byteChannel.putRecvData(getSetupRecvData("Thabu", Constants.PROTOCOL_VERSION, 1000, 0x12345678, "ABC"));
		sut.recv(byteChannel);
		sut.recv(byteChannel);
		verify(abortMessage).setPending(Matchers.eq(ChabuErrorCode.SETUP_REMOTE_CHABU_NAME.getCode()), Matchers.anyString());
	}	
	
	@Test public void
	recv_remote_setup_with_wrong_protocol_major_version() throws Exception {
		byteChannel.putRecvData(getSetupRecvData("CHABU", Constants.PROTOCOL_VERSION + 0x10000, 1000, 0x12345678, "ABC"));
		sut.recv(byteChannel);
		verify(abortMessage).setPending(Matchers.eq(ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION.getCode()), Matchers.anyString());
	}	
	
	@Test public void
	recv_remote_setup_with_wrong_protocol_minor_version_ignored() throws Exception {
		byteChannel.putRecvData(getSetupRecvData("CHABU", Constants.PROTOCOL_VERSION + 0x1000, 1000, 0x12345678, "ABC"));
		sut.recv(byteChannel);
		verify(abortMessage, never()).setPending(Matchers.eq(ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION.getCode()), Matchers.anyString());
	}	
	
	@Test public void
	recv_remote_setup_with_single_bytes() throws Exception {
		String data = getSetupRecvData();
		
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
				getAcceptData());
		sut.recv(byteChannel);
		assertThat(setup.isRemoteAcceptReceived()).isTrue();
		verify(completionListener).run();
	}

	@Test public void
	recv_accept_without_setup() throws Exception {
		String data = getAcceptData();
		byteChannel.putRecvData( data );
		sut.recv(byteChannel);
		verify(abortMessage).setPending(
				Matchers.eq(ChabuErrorCode.PROTOCOL_ACCEPT_WITHOUT_SETUP), 
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
