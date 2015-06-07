package chabu;

import static org.junit.Assert.*;

import org.junit.Test;

@SuppressWarnings("unused")
public class TestSetupConnection {

	@Test
	public void First() throws Exception {
		
		ChabuConnectingInfo ci = new ChabuConnectingInfo();
		
		IChabu chabu = ChabuBuilder
				.start(ci)
				.setConnectionValidator(new IChabuConnectingValidator(){

					@Override
					public boolean isAccepted(ChabuConnectingInfo local,
							ChabuConnectingInfo remote) {
						// TODO Auto-generated method stub
						return false;
					}})
				.setNetwork(null)
				.setPriorityCount(3)
				.build();
		
		TraceRunner.testText( chabu, ""
				+ "// simple case for setup\n"
				+ "SETUP: {\r\n" + 
				"    \"ChabuProtocolVersion\"  : 1,\r\n" + 
				"    \"ByteOrderBigEndian\"    : true,\r\n" + 
				"    \"MaxReceivePayloadSize\" : 10,\r\n" + 
				"    \"ReceiveCannelCount\"    : 2,\r\n" + 
				"    \"ApplicationVersion\"    : 2,\r\n" + 
				"    \"ApplicationName\"       : \"ABC\",\r\n" + 
				"    \"PriorityCount\"         : 3,\r\n" + 
				"    \"Channels\" : [\r\n" + 
				"         { \"ID\" : 0, \"Priority\" : 1, \"RxSize\" : 20, \"TxSize\" : 20 },\r\n" + 
				"         { \"ID\" : 1, \"Priority\" : 0, \"RxSize\" : 20, \"TxSize\" : 20 }\r\n" + 
				"         ]\r\n" + 
				"}\r\n" + 
				"\r\n" + 
				"// Check the configuration block send by Chabu\r\n" + 
				"WIRE_RX: {}\r\n" + 
				"    01          // u8   protocol version\r\n" + 
				"	01          // bool endianess\r\n" + 
				"	00 0A       // u16  maxReceivePayloadSize	\r\n" + 
				"	00 02       // u16  receiveCannelCount\r\n" + 
				"	00 00 00 06 // u32  applicationVersion\r\n" + 
				"	03 58 59 5A // u8+. applicationName\r\n" + 
				"	<<\r\n" + 
				"    \r\n" + 
				"// Create config block\r\n" + 
				"WIRE_TX: {}\r\n" + 
				"    01          // u8   protocol version\r\n" + 
				"	01          // bool endianess\r\n" + 
				"	00 0a       // u16  maxReceivePayloadSize	\r\n" + 
				"	00 02       // u16  receiveCannelCount\r\n" + 
				"	00 00 00 02 // u32  applicationVersion\r\n" + 
				"	03 41 42 43 // u8+. applicationName\r\n" + 
				"	<<\r\n" + 
				"\n"
				);
	}

	
}
