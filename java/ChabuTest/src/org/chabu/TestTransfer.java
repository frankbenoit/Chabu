package org.chabu;

import org.junit.Test;

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
				+ "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 07 FF "
				+ "00 00 00 10 77 77 00 C3 00 00 00 01 00 00 07 FF");

		// send initial ARM with the first data
		r.wireTx(""
				// ARM[0]=64
				+ "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 02 00 "
				// ARM[1]=64
				+ "00 00 00 10 77 77 00 C3 00 00 00 01 00 00 00 64 "
				+ "00 00 00 10 77 77 00 C3 00 00 00 02 00 00 00 64 "
				+ "00 00 00 10 77 77 00 C3 00 00 00 03 00 00 00 64 "
				+ "00 00 00 10 77 77 00 C3 00 00 00 04 00 00 00 64 "
				// SEQ[0]=0, DATA[50]
				// packetlen.< type......< chan......< seq.......< dav ......< pls.......<
				+ "00 00 00 68 77 77 00 B4 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 50 "
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 " 
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "  
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "  
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "  
				+ "AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55");
		
		// recv some data
		r.wireRx("00 00 00 20 77 77 00 B4 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 08 01 02 03 04 05 06 07 08");

		r.channelToAppl(0, "01 02 03 04 05 06 07 08");

//		r.wireRx("00 11 c4 00 00 00 00 00 00 00 08 01 02 03 04 05 06 07 08");
//		r.wireTx( "00" );
	}


	private TraceRunner setupTraceRunner() {
		
		Chabu chabu = ChabuBuilder
				.start(12345678, "ABC", 0x100, 3)
				.addChannel(0, 0x200, 2, new TestChannelUser())
				.addChannel(1, 100, 1, new TestChannelUser())
				.addChannel(2, 100, 1, new TestChannelUser())
				.addChannel(3, 100, 1, new TestChannelUser())
				.addChannel(4, 100, 0, new TestChannelUser())
				.build();

		TraceRunner r = TraceRunner.test(chabu);
		
		// SETUP
		r.wireRxAutoLength(""
				+ "77 77 00 F0 "
				+ "00 00 00 05 43 48 41 42 55 00 00 00 "
				+ "00 00 00 01 "
				+ "00 00 01 00 "
				+ "00 00 00 06 "
				+ TestUtils.test2LengthAndHex("ABC"));
		    
		r.wireTxAutoLength(""
				+ "77 77 00 F0 "
				+ "00 00 00 05 43 48 41 42 55 00 00 00 "
				+ "00 00 00 01 "
				+ "00 00 01 00 "
				+ "00 bc 61 4e "
				+ TestUtils.test2LengthAndHex("ABC"));

		// ACCEPT
		r.wireRxAutoLength("77 77 00 E1");
		r.wireTxAutoLength("77 77 00 E1");
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
				+ "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 07 FF "
				+ "00 00 00 10 77 77 00 C3 00 00 00 01 00 00 07 FF");

		// send initial ARM with the first data
		r.wireTx( ""
				// ARM[0]=64
				+ "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 02 00 "
				// ARM[1]=64
				+ "00 00 00 10 77 77 00 C3 00 00 00 01 00 00 00 64 "
				+ "00 00 00 10 77 77 00 C3 00 00 00 02 00 00 00 64 "
				+ "00 00 00 10 77 77 00 C3 00 00 00 03 00 00 00 64 "
				+ "00 00 00 10 77 77 00 C3 00 00 00 04 00 00 00 64 "
				// SEQ[0]=0, DATA[50]
				// len         SE          chan.       seq........ dav........ pls..
				+ "00 00 01 00 77 77 00 B4 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 E8 " + dg.getExpBytesString(232) + " "
				+ "00 00 00 34 77 77 00 B4 00 00 00 00 00 00 00 E8 00 00 00 00 00 00 00 19 " + dg.getExpBytesString( 25) + " 00 00 00"
				);
		
		r.wireTx( 20, "");

		dg.ensureSamePosition();
		
		r.wireRx(""
		// len         SE          chan.       seq........ dav........ pls..
		+ "00 00 01 00 77 77 00 B4 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 E8 " + dg.getExpBytesString(232) + " "
		+ "00 00 00 34 77 77 00 B4 00 00 00 00 00 00 00 E8 00 00 00 00 00 00 00 19 " + dg.getExpBytesString( 25) + " 00 00 00");
		
		r.channelToAppl( 0, dg.getGenBytesString(257));
		dg.ensureSamePosition();
		
		// SEQ[0]=F5+0c=0x101 +0x200 -> ARM = 0x301
		r.wireTx( 20, "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 03 01" );
		r.wireRx(     "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 08 FF " );
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
				+ "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 07 FF "
				+ "00 00 00 10 77 77 00 C3 00 00 00 01 00 00 07 FF "
				+ "00 00 00 10 77 77 00 C3 00 00 00 02 00 00 07 FF "
				+ "00 00 00 10 77 77 00 C3 00 00 00 03 00 00 07 FF "
				+ "00 00 00 10 77 77 00 C3 00 00 00 04 00 00 07 FF "
				);

		// send initial ARM with the first data
		r.wireTx( ""
				// ARM[0]=64
				+ "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 02 00 "
				// ARM[1]=64
				+ "00 00 00 10 77 77 00 C3 00 00 00 01 00 00 00 64 "
				+ "00 00 00 10 77 77 00 C3 00 00 00 02 00 00 00 64 "
				+ "00 00 00 10 77 77 00 C3 00 00 00 03 00 00 00 64 "
				+ "00 00 00 10 77 77 00 C3 00 00 00 04 00 00 00 64 "
				// SEQ[0]=0, DATA[50]
				
				// 2 blocks 236+21
				// packetlen.< type......< chan......< seq.......< dav ......< pls.......<
				+ "00 00 01 00 77 77 00 B4 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 E8 " + dg0.getExpBytesString(232) + " "
				+ "00 00 00 34 77 77 00 B4 00 00 00 00 00 00 00 E8 00 00 00 00 00 00 00 19 " + dg0.getExpBytesString(25) + " 00 00 00 "
				);
		
		// see the data of same priority in round robin
		// 1, 2, 3, 1, 2, 3
		r.wireTx( ""
				// packetlen.< type......< chan......< seq.......< dav ......< pls.......<
				+ "00 00 01 00 77 77 00 B4 00 00 00 01 00 00 00 00 00 00 00 00 00 00 00 E8 " + dg1.getExpBytesString(232) + " "
				+ "00 00 01 00 77 77 00 B4 00 00 00 02 00 00 00 00 00 00 00 00 00 00 00 E8 " + dg2.getExpBytesString(232) + " "
				+ "00 00 01 00 77 77 00 B4 00 00 00 03 00 00 00 00 00 00 00 00 00 00 00 E8 " + dg3.getExpBytesString(232) + " "
				+ "00 00 00 34 77 77 00 B4 00 00 00 01 00 00 00 E8 00 00 00 00 00 00 00 19 " + dg1.getExpBytesString(25) + " 00 00 00 "
				);

		r.wireTx( ""
				+ "00 00 00 34 77 77 00 B4 00 00 00 02 00 00 00 E8 00 00 00 00 00 00 00 19 " + dg2.getExpBytesString( 25) + " 00 00 00 "
				+ "00 00 00 34 77 77 00 B4 00 00 00 03 00 00 00 E8 00 00 00 00 00 00 00 19 " + dg3.getExpBytesString( 25) + " 00 00 00 "
				);
		
//		r.wireTx( 20, "");

		dg0.ensureSamePosition();
		
		r.wireRx(""
		+ "00 00 01 00 77 77 00 B4 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 E8 " + dg0.getExpBytesString(232) + " "
		+ "00 00 00 34 77 77 00 B4 00 00 00 00 00 00 00 E8 00 00 00 00 00 00 00 19 " + dg0.getExpBytesString( 25) + " 00 00 00");
		
		r.applToChannel( 2, dg2.getGenBytesString( 257 ) );
		r.applToChannel( 3, dg3.getGenBytesString( 257 ) );

		r.wireTx( ""
				+ "00 00 01 00 77 77 00 B4 00 00 00 02 00 00 01 01 00 00 00 00 00 00 00 E8 " + dg2.getExpBytesString(232) + " "
				);

		r.applToChannel( 2, dg2.getGenBytesString( 257 ) );

		r.wireTx( ""
				+ "00 00 01 00 77 77 00 B4 00 00 00 03 00 00 01 01 00 00 00 00 00 00 00 E8 " + dg3.getExpBytesString(232) + " "
				+ "00 00 01 00 77 77 00 B4 00 00 00 02 00 00 01 E9 00 00 00 00 00 00 00 E8 " + dg2.getExpBytesString(232) + " "
				+ "00 00 00 34 77 77 00 B4 00 00 00 03 00 00 01 E9 00 00 00 00 00 00 00 19 " + dg3.getExpBytesString( 25) + " 00 00 00 "
				);

		r.wireTx( ""
				+ "00 00 00 4C 77 77 00 B4 00 00 00 02 00 00 02 D1 00 00 00 00 00 00 00 32 " + dg2.getExpBytesString(0x32) + " 00 00 "
				+ "00 00 01 00 77 77 00 B4 00 00 00 04 00 00 00 00 00 00 00 00 00 00 00 E8 " + dg4.getExpBytesString(232) + " "
				+ "00 00 00 34 77 77 00 B4 00 00 00 04 00 00 00 E8 00 00 00 00 00 00 00 19 " + dg4.getExpBytesString( 25) + " 00 00 00"
				);

		r.channelToAppl( 0, dg0.getGenBytesString(257));
		dg0.ensureSamePosition();
		
		// SEQ[0]=F5+0c=0x101 +0x200 -> ARM = 0x301
		r.wireTx( 20, "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 03 01" );
		r.wireRx(     "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 08 FF " );
	}
	
}
