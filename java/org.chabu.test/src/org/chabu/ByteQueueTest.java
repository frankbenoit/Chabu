package org.chabu;

import java.util.Random;

import org.chabu.container.ByteQueue;
import org.chabu.container.ByteQueueBuilder;
import org.chabu.container.ByteQueueInputPort;
import org.chabu.container.ByteQueueOutputPort;

/**
 * Test the bandwidth of a org.chabu.container.ByteQueue
 * 
 * @author Frank Benoit
 */
public class ByteQueueTest {

	
	static PseudoRandom rndXmit = new PseudoRandom(44);
	static PseudoRandom rndRecv = new PseudoRandom(44);

	static ByteQueue q = ByteQueueBuilder.create( "testqueue", 1000 );
	final static int LEN = 300_000_000;
	
	static Runnable sender = new Runnable(){
		public void run() {
			ByteQueueInputPort inport = q.getInport();
			byte[] buf = new byte[q.capacity()];
			int remaining = LEN;
			Random rnd = new Random();
			while( remaining > 0 ){
				int free = inport.freeCommitted();
				if( free == 0 ){
					continue;
				}
				
				int sz = Math.min(remaining, rnd.nextInt( free ) + 1);
				//rndXmit.nextBytes(buf, 0, sz);
				inport.write( buf, 0, sz );
				inport.commit();
				remaining -= sz;
				
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
		ByteQueueOutputPort outport = q.getOutport();
		while( remaining > 0 ){
			int avail = outport.available();
			if( avail == 0 ){
				continue;
			}
			
			int sz = Math.min(remaining, rnd.nextInt( avail ) + 1);
			outport.read( buf, 0, sz );
			outport.commit();
			//rndRecv.nextBytesVerify(buf, 0, sz);
			remaining -= sz;
			
		}

		long ts2 = System.nanoTime();
		
		System.out.println("recv completed");
		
		double durMs = (ts2-ts1)/1e9;
		double bandwidth = LEN / durMs;
		System.out.printf("Bandwidth %sMB/s, dur %sms", Math.round(bandwidth*1e-6), Math.round( durMs*1000) );
		
	}
	
}
