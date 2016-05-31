package org.chabu.prot.v1.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;

import org.chabu.prot.v1.ChabuErrorCode;
import org.chabu.prot.v1.ChabuException;
import org.chabu.prot.v1.internal.Utils;
import org.junit.Test;

public class UtilsTest {

	@Test
	public void testAlignUpTo4() {
		assertEquals(  4, Utils.alignUpTo4( 4 ));
		assertEquals(  8, Utils.alignUpTo4( 5 ));
		assertEquals(  8, Utils.alignUpTo4( 6 ));
		assertEquals(  8, Utils.alignUpTo4( 7 ));
		assertEquals(  8, Utils.alignUpTo4( 8 ));
		assertEquals( 12, Utils.alignUpTo4( 9 ));
	}

	@Test
	public void testIsAligned4() {
		assertEquals( true , Utils.isAligned4( 4 ));
		assertEquals( false, Utils.isAligned4( 5 ));
		assertEquals( false, Utils.isAligned4( 6 ));
		assertEquals( false, Utils.isAligned4( 7 ));
		assertEquals( true , Utils.isAligned4( 8 ));
	}

	@Test
	public void printTraceHexData() throws Exception {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		ByteBuffer bb = ByteBuffer.allocate(100);
		Utils.printTraceHexData(pw, bb, 0, 100);
		assertThat(sw.toString()).isEqualTo(
				"    00 00 00 00  00 00 00 00  00 00 00 00  00 00 00 00\r\n" + 
				"    00 00 00 00  00 00 00 00  00 00 00 00  00 00 00 00\r\n" + 
				"    00 00 00 00  00 00 00 00  00 00 00 00  00 00 00 00\r\n" + 
				"    00 00 00 00  00 00 00 00  00 00 00 00  00 00 00 00\r\n" + 
				"    00 00 00 00  00 00 00 00  00 00 00 00  00 00 00 00\r\n" + 
				"    00 00 00 00  00 00 00 00  00 00 00 00  00 00 00 00\r\n" + 
				"    00 00 00 00\r\n" + 
				"    <<\r\n" + 
				"\r\n");
	}
	
	@Test
	public void fail() throws Exception {
		assertThatThrownBy(() -> Utils.fail( ChabuErrorCode.APPLICATION_VALIDATOR, ""))
			.isExactlyInstanceOf(ChabuException.class);
		assertThatThrownBy(() -> Utils.fail( 32, ""))
			.isExactlyInstanceOf(ChabuException.class);
	}
	@Test
	public void ensure() throws Exception {
		
		Utils.ensure( true, ChabuErrorCode.APPLICATION_VALIDATOR, "");
		Utils.ensure( true, 32, "");
		
		assertThatThrownBy(() -> Utils.ensure( false, ChabuErrorCode.APPLICATION_VALIDATOR, ""))
			.isExactlyInstanceOf(ChabuException.class);
		
		assertThatThrownBy(() -> Utils.ensure( false, 32, ""))
			.isExactlyInstanceOf(ChabuException.class);
	}
	
	@Test
	public void waitOn() throws Exception {
		
		Thread t = new Thread(() -> {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			Utils.notifyAllOn(UtilsTest.this);
		});
		
		t.start();
		
		Utils.waitOn(this);
	}
	
	@Test
	public void waitOnInterrupts() throws Exception {
		Thread current = Thread.currentThread();
		Thread t = new Thread(() -> {
			try {
				Thread.sleep(40);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			synchronized(UtilsTest.this){
				current.interrupt();
			}
		});

		t.start();
		
		assertThatThrownBy(()-> Utils.waitOn(this) )
			.isInstanceOf(RuntimeException.class);
	}
	
	@Test
	public void waitOnTimed() throws Exception {
		Utils.waitOn(this, 10);
	}
	
	@Test
	public void waitOnTimedInterrupts() throws Exception {
		Thread current = Thread.currentThread();
		Thread t = new Thread(() -> {
			try {
				Thread.sleep(40);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
			synchronized(UtilsTest.this){
				current.interrupt();
			}
		});
		
		t.start();
		
		assertThatThrownBy(()-> Utils.waitOn(this, 200) )
		.isInstanceOf(RuntimeException.class);
	}
	
	@Test
	public void notifyAllOn() throws Exception {
		
	}
}
