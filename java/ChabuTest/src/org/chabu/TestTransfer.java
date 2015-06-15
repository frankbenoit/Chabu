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
				+ "00 07 C3 00 00 00 00 02 00 "
				// ARM[1]=64
				+ "00 07 C3 00 01 00 00 00 64 "
				+ "00 07 C3 00 02 00 00 00 64 "
				+ "00 07 C3 00 03 00 00 00 64 "
				+ "00 07 C3 00 04 00 00 00 64 "
				// SEQ[0]=0, DATA[50]
				+ "00 59 B4 00 00 00 00 00 00 00 50 "
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 " 
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "  
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "  
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "  
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55");
		
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
				.addChannel(0, 0x200, 2, new TestChannelUser())
				.addChannel(1, 100, 1, new TestChannelUser())
				.addChannel(2, 100, 1, new TestChannelUser())
				.addChannel(3, 100, 1, new TestChannelUser())
				.addChannel(4, 100, 0, new TestChannelUser())
				.build();

		TraceRunner r = TraceRunner.test(chabu);
		
		// SETUP
		r.wireRxAutoLength(""
				+ "F0 "
				+ "01 "
				+ "01 00 "
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
		r.wireTx( ""
				// ARM[0]=64
				+ "00 07 C3 00 00 00 00 02 00 "
				// ARM[1]=64
				+ "00 07 C3 00 01 00 00 00 64 "
				+ "00 07 C3 00 02 00 00 00 64 "
				+ "00 07 C3 00 03 00 00 00 64 "
				+ "00 07 C3 00 04 00 00 00 64 "
				// SEQ[0]=0, DATA[50]
				// len   SE chan. seq........ pls..
				+ "00 FE B4 00 00 00 00 00 00 00 F5 " + dg.getExpBytesString(245) + " "
				+ "00 15 B4 00 00 00 00 00 F5 00 0C " + dg.getExpBytesString( 12) + " "
				);
		
		r.wireTx( 20, "");

		dg.ensureSamePosition();
		
		r.wireRx(""
		+ "00 FE B4 00 00 00 00 00 00 00 F5 " + dg.getExpBytesString(245) + " "
		+ "00 15 B4 00 00 00 00 00 F5 00 0C " + dg.getExpBytesString( 12));
		
		r.channelToAppl( 0, dg.getGenBytesString(257));
		dg.ensureSamePosition();
		
		// SEQ[0]=F5+0c=0x101 +0x200 -> ARM = 0x301
		r.wireTx( 20, "00 07 C3 00 00 00 00 03 01" );
		r.wireRx( "00 07 C3 00 00 00 00 08 FF " );
	}
	
	@Test
	public void Priorities() throws Exception {
		TraceRunner r = setupTraceRunner();
		DataGen dg0 = new DataGen("0", 42 );
		DataGen dg1 = new DataGen("1", 42 );
		DataGen dg2 = new DataGen("2", 42 );
		DataGen dg3 = new DataGen("3", 42 );
		DataGen dg4 = new DataGen("4", 42 );
		
		// Fill data all 5 channels
		r.applToChannel( 0, dg0.getGenBytesString( 257 ) );
		r.applToChannel( 1, dg1.getGenBytesString( 257 ) );
		r.applToChannel( 2, dg2.getGenBytesString( 257 ) );
		r.applToChannel( 3, dg3.getGenBytesString( 257 ) );
		r.applToChannel( 4, dg4.getGenBytesString( 257 ) );

		// ARM all channels
		r.wireRx(""
				+ "00 07 C3 00 00 00 00 07 FF "
				+ "00 07 C3 00 01 00 00 07 FF "
				+ "00 07 C3 00 02 00 00 07 FF "
				+ "00 07 C3 00 03 00 00 07 FF "
				+ "00 07 C3 00 04 00 00 07 FF "
				);

		// send initial ARM with the first data
		r.wireTx( ""
				// ARM[0]=64
				+ "00 07 C3 00 00 00 00 02 00 "
				// ARM[1]=64
				+ "00 07 C3 00 01 00 00 00 64 "
				+ "00 07 C3 00 02 00 00 00 64 "
				+ "00 07 C3 00 03 00 00 00 64 "
				+ "00 07 C3 00 04 00 00 00 64 "
				// SEQ[0]=0, DATA[50]
				// len   SE chan. seq........ pls..
				+ "00 FE B4 00 00 00 00 00 00 00 F5 " + dg0.getExpBytesString(245) + " "
				+ "00 15 B4 00 00 00 00 00 F5 00 0C " + dg0.getExpBytesString( 12) + " "
				);
		r.wireTx( ""
				+ "00 FE B4 00 01 00 00 00 00 00 F5 " + dg1.getExpBytesString(245) + " "
				+ "00 FE B4 00 02 00 00 00 00 00 F5 " + dg2.getExpBytesString(245) + " "
				+ "00 FE B4 00 03 00 00 00 00 00 F5 " + dg3.getExpBytesString(245) + " "
				+ "00 15 B4 00 01 00 00 00 F5 00 0C " + dg1.getExpBytesString( 12) + " "
				);

		r.wireTx( ""
				+ "00 15 B4 00 02 00 00 00 F5 00 0C " + dg2.getExpBytesString( 12) + " "
				+ "00 15 B4 00 03 00 00 00 F5 00 0C " + dg3.getExpBytesString( 12) + " "
				);
		
//		r.wireTx( 20, "");

		dg0.ensureSamePosition();
		
		r.wireRx(""
		+ "00 FE B4 00 00 00 00 00 00 00 F5 " + dg0.getExpBytesString(245) + " "
		+ "00 15 B4 00 00 00 00 00 F5 00 0C " + dg0.getExpBytesString( 12));
		
		r.applToChannel( 2, dg2.getGenBytesString( 257 ) );
		r.applToChannel( 3, dg3.getGenBytesString( 257 ) );

		r.wireTx( ""
				+ "00 FE B4 00 02 00 00 01 01 00 F5 " + dg2.getExpBytesString(245) + " "
				);

		r.applToChannel( 2, dg2.getGenBytesString( 257 ) );

		r.wireTx( ""
				+ "00 FE B4 00 03 00 00 01 01 00 F5 " + dg3.getExpBytesString(245) + " "
				+ "00 FE B4 00 02 00 00 01 F6 00 F5 " + dg2.getExpBytesString(245) + " "
				+ "00 15 B4 00 03 00 00 01 F6 00 0C " + dg3.getExpBytesString( 12) + " "
				);

		r.wireTx( ""
				+ "00 21 B4 00 02 00 00 02 eb 00 18 " + dg2.getExpBytesString( 24) + " "
				+ "00 FE B4 00 04 00 00 00 00 00 F5 " + dg4.getExpBytesString(245) + " "
				+ "00 15 B4 00 04 00 00 00 F5 00 0C " + dg4.getExpBytesString( 12) + " "
				);

		r.channelToAppl( 0, dg0.getGenBytesString(257));
		dg0.ensureSamePosition();
		
		// SEQ[0]=F5+0c=0x101 +0x200 -> ARM = 0x301
		r.wireTx( 20, "00 07 C3 00 00 00 00 03 01" );
		r.wireRx( "00 07 C3 00 00 00 00 08 FF " );
	}
	
}
