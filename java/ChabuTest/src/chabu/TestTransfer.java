package chabu;

import static org.junit.Assert.*;

import org.junit.Test;

public class TestTransfer {

	@Test
	public void First() throws Exception {
		TraceRunner.test("TestTransfer_First.txt");
	}

	@Test
	public void PayloadLimit() throws Exception {
		TraceRunner.test("TestTransfer_PayloadLimit.txt");
	}
	
	@Test
	public void Segmentation() throws Exception {
		TraceRunner.test("TestTransfer_Segmentation.txt");
	}
	
}
