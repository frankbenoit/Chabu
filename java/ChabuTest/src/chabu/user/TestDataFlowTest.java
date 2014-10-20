package chabu.user;

import java.nio.ByteBuffer;

import org.junit.Before;
import org.junit.Test;

public class TestDataFlowTest {

	private TestData testData;

	@Before
	public void setUp() throws Exception {
		testData = new TestData();
	}

	/**
	 * Test simple read and check of data.
	 */
	@Test
	public void test() {
		TestDataFlow f = testData.createFlow("");
		ByteBuffer b = ByteBuffer.allocate(10000);
		long AMOUNT = 1000000L;
		long idx = 0;
		while( idx < AMOUNT ){
			b.clear();
			if( b.limit() + idx > AMOUNT ){
				b.limit((int)(AMOUNT-idx));
			}
			idx += b.limit();
			f.copySendData(b);
			b.flip();
			f.checkRecvData(b);
		}
	}

}
