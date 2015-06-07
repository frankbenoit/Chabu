package chabu;

import static org.junit.Assert.*;

import org.junit.Test;

@SuppressWarnings("unused")
public class TestTransfer {

	@Test
	public void First() throws Exception {
		TraceRunner.testFile("TestTransfer_First.txt");
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
		TraceRunner.testFile("TestTransfer_PayloadLimit.txt");
	}
	
	@Test
	public void Segmentation() throws Exception {
		TraceRunner.testFile("TestTransfer_Segmentation.txt");
	}
	
}
