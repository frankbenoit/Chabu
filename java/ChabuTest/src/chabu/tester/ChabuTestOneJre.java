package chabu.tester;

import java.io.IOException;

import chabu.tester.dut.ChabuTestDutNw;

public class ChabuTestOneJre {
	public static void main(String[] args) throws InterruptedException, IOException {
		
		
		Thread chabuTester             = ChabuTesterAppWnd.mainInternalCreateThread( "Tester" );
		ChabuTestDutNw chabuTesterDut1 = ChabuTestDutNw.mainInternalCreateThread("Dut0", Constants.PORT_DUT0 );
		ChabuTestDutNw chabuTesterDut2 = ChabuTestDutNw.mainInternalCreateThread("Dut1", Constants.PORT_DUT1 );
		
		
		chabuTester    .join(); System.out.println("-- Tester terminated --");
		chabuTesterDut1.shutDown();
		chabuTesterDut2.shutDown();
		chabuTesterDut1.join(); System.out.println("-- Dut[0] terminated --");
		chabuTesterDut2.join(); System.out.println("-- Dut[1] terminated --");
		
		System.out.println("-- EXIT --");
		System.exit(0);
	}
}
