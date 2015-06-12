package org.chabu;

import static org.junit.Assert.*;

import org.chabu.ChabuBuilder;
import org.chabu.ChabuSetupInfo;
import org.chabu.IChabu;
import org.junit.Ignore;
import org.junit.Test;

@SuppressWarnings("unused")
public class TestTransfer {

	@Test
	public void First() throws Exception {

		TraceRunner r = setupTraceRunner();

		// Fill data
		r.applToChannel( 0, ""
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55");

		// recv initial ARMs
		r.wireRx(""
				+ "00 07 C3 00 00 00 00 07 FF "
				+ "00 07 C3 00 01 00 00 07 FF");

		// send initial ARM with the first data
		r.wireTx(""
				// ARM[0]=64
				+ "00 07 C3 00 00 00 00 00 64 "
				// SEQ[0]=0, DATA[50]
				+ "00 59 B4 00 00 00 00 00 00 00 50 "
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 " 
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "  
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "  
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "  
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "
				// ARM[1]=64
				+ "00 07 C3 00 01 00 00 00 64");
		
		// recv some data
		r.wireRx("00 11 B4 00 00 00 00 00 00 00 08 01 02 03 04 05 06 07 08");

		r.channelToAppl(0, "01 02 03 04 05 06 07 08");

//		r.wireRx("00 11 c4 00 00 00 00 00 00 00 08 01 02 03 04 05 06 07 08");
//		r.wireTx( "00" );
	}


	private TraceRunner setupTraceRunner() {
		ChabuSetupInfo ci = new ChabuSetupInfo();
		ci.applicationName = "ABC";
		ci.applicationVersion = 12345678;
		ci.maxReceiveSize = 0x100;
		
		IChabu chabu = ChabuBuilder
				.start(ci, new TestNetwork(), 3)
				.addChannel(0, 100, 1, new TestChannelUser())
				.addChannel(1, 100, 0, new TestChannelUser())
				.build();

		TraceRunner r = TraceRunner.test(chabu);
		
		// SETUP
		r.wireRxAutoLength(""
				+ "F0 "
				+ "01 "
				+ "07 ff "
				+ "00 00 00 06 "
				+ TestUtils.test2LengthAndHex("ABC"));
		    
		r.wireTxAutoLength(""
				+ "F0 "
				+ "01 "
				+ "01 00 "
				+ "00 bc 61 4e "
				+ TestUtils.test2LengthAndHex("ABC"));

		// ACCEPT
		r.wireRxAutoLength("E1");
		r.wireTxAutoLength("E1");
		return r;
	}

	
	/*
	 * Connection Setup:
	 * 
	 * 
	 * 
	 * Receiving:
	 * 
	 * 
	 * 
	 * 
	 * Transmitting:
	 * 
	 * 
	 * 
	 */
	
	@Test
	public void PayloadLimit() throws Exception {
		TraceRunner r = setupTraceRunner();
		DataGen dg = new DataGen("1", 42 );
		
		// Fill data
		r.applToChannel( 0, dg.getGenBytesString( 257 ) );

		// recv initial ARMs
		r.wireRx(""
				+ "00 07 C3 00 00 00 00 07 FF "
				+ "00 07 C3 00 01 00 00 07 FF");

		// send initial ARM with the first data
		r.wireTx(""
				// ARM[0]=64
				+ "00 07 C3 00 00 00 00 00 64 "
				// SEQ[0]=0, DATA[50]
				// len   SE chan. seq........ pls..
				+ "00 FE B4 00 00 00 00 00 00 00 F5 " + dg.getExpBytesString(245) + " "
				//+ "00 0A B4 00 00 00 00 01 00 00 01 " + dg.getExpBytesString( 12) + " "
				// ARM[1]=64
				//+ "00 07 C3 00 01 00 00 00 64"
				);
		
		dg.ensureSamePosition();
		
//		r.wireRx("00 11 c4 00 00 00 00 00 00 00 08 01 02 03 04 05 06 07 08");
//		r.wireTx( "00" );
	}
	
	@Test
	@Ignore
	public void Segmentation() throws Exception {
		TraceRunner.testFile("TestTransfer_Segmentation.txt");
	}
	
}
