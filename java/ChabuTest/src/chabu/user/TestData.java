package chabu.user;

import java.nio.ByteBuffer;
import java.util.Random;

public class TestData {

	private final byte[] rawdata;
	private final Random rnd;
	final long startTs;

	public TestData(){
		rnd = new Random();
		int sz = 10000 + rnd.nextInt(1000);
		rawdata = new byte[ sz ];
		rnd.nextBytes(rawdata);
		startTs = System.nanoTime();
	}

	public int getRandomOffset() {
		return rnd.nextInt(rawdata.length);
	}

	public TestDataFlow createFlow(String name) {
		return new TestDataFlow(this, name );
	}

	public int copySendData(ByteBuffer dst, long sendIdx) {
		int res = 0;
		int idx = (int)(sendIdx % rawdata.length);
		while( dst.hasRemaining() ){
			int len = rawdata.length-idx;
			if( len > dst.remaining() ){
				len = dst.remaining();
			}
			dst.put( rawdata, idx, len);
			res += len;
			idx = 0;
		}
		return res;
	}

	public int checkRecvData(ByteBuffer src, long recvIdx, long startIdx ) {
		int res = 0;
		int idx = (int)(recvIdx % rawdata.length);
		while( src.hasRemaining() ){
			byte val = src.get();
			if( val != rawdata[idx] ){
				throw new RuntimeException( String.format("Data mismatch %02X:%02X @%d", val, rawdata[idx], recvIdx + res - startIdx ));
			}
			res++;
			idx++;
			if( idx >= rawdata.length ){
				idx = 0;
			}
		}
		return res;
	}
	
}
