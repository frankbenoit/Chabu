package chabu.user;

public interface TestConstants {

	/**
	 * The bytes per second transfered over the local loopback interface.
	 * Tests will use that to create amount of bytes to transfer to result 
	 * in a test with a predictable run duration.
	 */
	long BANDWIDTH = 100_000_000;

}
