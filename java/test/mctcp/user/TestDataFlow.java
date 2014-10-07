package mctcp.user;

import java.nio.ByteBuffer;
import java.util.ArrayList;

public class TestDataFlow {
	
	private TestData td;
	private long startIdx;
	private long sendIdx;
	private long recvIdx;
	
	private static final long TICK_NS = 100_000_000;
	private ArrayList<Integer> sendStats = new ArrayList<>(1000);
	private ArrayList<Integer> recvStats = new ArrayList<>(1000);
	private String name;

	TestDataFlow( TestData td, String name ){
		this.td = td;
		this.name = name;
		startIdx = td.getRandomOffset();
		sendIdx = startIdx;
		recvIdx = sendIdx;
	}
	
	public long copySendData( ByteBuffer dst ){
		int sz = td.copySendData( dst, sendIdx );
		sendIdx += sz;
		updateStats(sz, sendStats);
		return sz;
	}

	public long checkRecvData( ByteBuffer src ){
		int sz = td.checkRecvData( src, recvIdx, startIdx );
		recvIdx += sz;
		updateStats(sz, recvStats);
		return sz;
	}
	
	private void updateStats(int sz, ArrayList<Integer> stats) {
		synchronized(stats){			
			int statsIdx = (int)(( System.nanoTime() - td.startTs ) / TICK_NS);
			if( stats.size() <= statsIdx ){
				int cap = (int)Math.ceil( Math.log(statsIdx)/Math.log(2) );
				stats.ensureCapacity( cap );
			}
			while( stats.size() <= statsIdx ){
				stats.add( stats.size(), 0 );
			}
			stats.set( statsIdx, sz + stats.get( statsIdx ) );
		}
	}
	public void printStats(){
		int size = Math.max(sendStats.size(), recvStats.size());
		for( int i = 0; i < size; i++ ){
			int rx = 0;
			int tx = 0;
			if( recvStats.size() > i ){
				rx = recvStats.get(i);
			}
			if( sendStats.size() > i ){
				tx = sendStats.get(i);
			}
			System.out.printf("%s [%3d] tx %5dk rx %5dk\n", name, i, tx/1000, rx/1000 );
		}
	}
}
