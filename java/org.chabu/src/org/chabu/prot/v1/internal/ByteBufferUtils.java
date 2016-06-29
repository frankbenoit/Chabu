package org.chabu.prot.v1.internal;

import java.nio.ByteBuffer;

public class ByteBufferUtils {
	public static int transferRemaining( ByteBuffer src, ByteBuffer trg ){
		int xfer = Math.min( src.remaining(), trg.remaining() );
		int oldLimit = src.limit();
		src.limit( src.position() + xfer );
		trg.put( src );
		src.limit( oldLimit );
		return xfer;
	}

	public static int XtransferUpTo( ByteBuffer src, ByteBuffer trg, int maxCount ){
		int xfer = Math.min( Math.min( src.remaining(), trg.remaining() ), maxCount );
		int oldLimit = src.limit();
		src.limit( src.position() + xfer );
		trg.put( src );
		src.limit( oldLimit );
		return xfer;
	}

	/**
	 * Copy as many byte from source to target, until either source has no more data, target cannot take more or 
	 * the trg.position() is equals to limit value.
	 */
	public static void transferUntilTargetPos(ByteBuffer src, ByteBuffer trg, int trgPos ) {
		int cpySz = Math.min( src.remaining(), Math.min( trgPos - trg.position(), trg.remaining() ));
		if( cpySz <= 0 ){
			return;
		}
		int oldLimit = src.limit();
		src.limit( src.position() + cpySz );
		trg.put( src );
		src.limit( oldLimit );
	}
	

}
