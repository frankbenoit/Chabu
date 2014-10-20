package chabu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

class BufferPool {
	
	private class BufferRef {
		ByteBuffer buf;
		BufferRef next;
	}
	
	private BufferRef bufs;
	private BufferRef emptyRefs;
	private int allocCnt = 0;
	private int freeCnt = 0;
	private int blockCount;
	
	BufferPool( ByteOrder byteOrder, int blockSize, int blockCount ){
		this.blockCount = blockCount;
		for( int i = 0; i < blockCount; i++ ){
			ByteBuffer buf = ByteBuffer.allocate( blockSize );
			buf.order(byteOrder);
			BufferRef ref = new BufferRef();
			ref.buf = buf;
			ref.next = bufs;
			bufs = ref;
		}
	}
	

	ByteBuffer allocBuffer(){
		BufferRef ref = bufs;
		Utils.ensure( ref != null, "No more Blocks allocs:%d frees:%d blockCount:%d", allocCnt, freeCnt, blockCount);
		
		allocCnt++;
		bufs = ref.next;
		ByteBuffer res = ref.buf;

		Utils.ensure( res != null, "Empty reference in BlockPool");

		ref.buf = null;
		
		ref.next = emptyRefs;
		emptyRefs = ref;
		
		res.clear();

		return res;
	}
	
	void freeBuffer( ByteBuffer buf ){
		freeCnt++;
		Utils.ensure( emptyRefs != null, "BlockPool: no empty ref");
		
		BufferRef ref = emptyRefs;
		emptyRefs = ref.next;
		
		Utils.ensure( ref.buf == null, "BlockPool: Empty reference in not empty");
		
		ref.buf = buf;
		ref.next = bufs;
		
		bufs = ref;
	}
	
	public static void main(String[] args) {
		BufferPool pool = new BufferPool( ByteOrder.BIG_ENDIAN, 10, 3 );
		
		pool.allocBuffer();
		pool.allocBuffer();
		pool.allocBuffer();
		
	}
}
