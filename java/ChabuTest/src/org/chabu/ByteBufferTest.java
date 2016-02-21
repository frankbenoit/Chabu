package org.chabu;

import java.nio.ByteBuffer;
import java.util.Random;

/**
 * Test the bandwidth of a java.nio.ByteBuffer
 * 
 * @author Frank Benoit
 */
public class ByteBufferTest {

	
	static PseudoRandom rndXmit = new PseudoRandom(44);
	static PseudoRandom rndRecv = new PseudoRandom(44);

	static ByteBuffer q = ByteBuffer.allocate( 1000 );
	final static int LEN = 300_000_000;
	
	static Runnable sender = new Runnable(){
		public void run() {
			int remaining = LEN;
			Random rnd = new Random();
			byte[] buf = new byte[q.capacity()];
			while( remaining > 0 ){
				synchronized(ByteBufferTest.class){
					int rm = q.remaining();
					if( rm == 0 ){
						continue;
					}
					int sz = Math.min(remaining, rnd.nextInt( rm ) + 1);
					//rndXmit.nextBytes(buf, 0, sz);
					q.put( buf, 0, sz );
					remaining -= sz;
				}
				
				
				
			}
			System.out.println("xmit completed");
		}
	};
	
	
	public static void main(String[] args) {
		Thread t = new Thread(sender);
		t.start();
		
		int remaining = LEN;
		Random rnd = new Random();
		byte[] buf = new byte[q.capacity()];
		
		long ts1 = System.nanoTime();
		
		while( remaining > 0 ){
			synchronized(ByteBufferTest.class){
				if( q.position() == 0 ){
					continue;
				}
				q.flip();
				int sz = Math.min(remaining, rnd.nextInt( q.remaining() ) + 1);
				q.get( buf, 0, sz );
				//rndRecv.nextBytesVerify(buf, 0, sz);
				q.compact();
				remaining -= sz;
			}
		}

		long ts2 = System.nanoTime();
		
		System.out.println("recv completed");
		
		double durMs = (ts2-ts1)/1e9;
		double bandwidth = LEN / durMs;
		System.out.printf("Bandwidth %sMB/s, dur %sms", Math.round(bandwidth*1e-6), Math.round( durMs*1000) );
		
	}
	
}
