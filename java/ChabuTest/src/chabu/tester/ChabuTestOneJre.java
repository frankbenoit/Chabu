package chabu.tester;

import java.io.IOException;

import chabu.tester.dut.ChabuTestDutNw;

public class ChabuTestOneJre {
	public static void main(String[] args) throws InterruptedException, IOException {
		
		final int PORT_DUT0 = 2300;
		final int PORT_DUT1 = 2310;
		
		Thread chabuTester             = ChabuTester.mainInternalCreateThread( "Tester" );
		ChabuTestDutNw chabuTesterDut1 = ChabuTestDutNw.mainInternalCreateThread("Dut[0]", PORT_DUT0 );
		ChabuTestDutNw chabuTesterDut2 = ChabuTestDutNw.mainInternalCreateThread("Dut[1]", PORT_DUT1 );
		
		
		chabuTester    .join(); System.out.println("-- Tester terminated --");
		chabuTesterDut1.join(); System.out.println("-- Dut[0] terminated --");
		chabuTesterDut2.join(); System.out.println("-- Dut[1] terminated --");
		
		System.out.println("-- EXIT --");
		System.exit(0);
	}
}
