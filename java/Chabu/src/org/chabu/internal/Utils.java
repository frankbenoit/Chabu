package org.chabu.internal;

import java.io.PrintWriter;
import java.nio.ByteBuffer;

import org.chabu.ChabuErrorCode;
import org.chabu.ChabuException;

public class Utils {
	
	public static RuntimeException failX( String fmt, Object ... args ){
		throw new RuntimeException( String.format( fmt, args ));
	}
	
	/**
	 * Throw an exception if the condition is not true.
	 * @param cond
	 */
	public static void ensureX( boolean cond, String fmt, Object ... args ){
		if( cond ) return;
		throw new RuntimeException( String.format( fmt, args ));
	}
	
	public static void fail( ChabuErrorCode error, String fmt, Object ... args ){
		throw new ChabuException( error, String.format( fmt, args ));
	}

	public static void fail( int error, String fmt, Object ... args ){
		throw new ChabuException( error, String.format( fmt, args ));
	}
	
	public static void ensure( boolean cond, ChabuErrorCode error, String fmt, Object ... args ){
		if( cond ) return;
		throw new ChabuException( error, String.format( fmt, args ));
	}
	
	public static void ensure( boolean cond, int code, String fmt, Object ... args ){
		if( cond ) return;
		throw new ChabuException( code, String.format( fmt, args ));
	}
	
	/**
	 * Throw an exception if the condition is not true.
	 * @param cond
	 */
	public static void ensureX( boolean cond ){
		if( cond ) return;
		throw new RuntimeException();
	}
	
	/**
	 * Wrap Object.wait() and throw RuntimeException instead of InterruptedException
	 */
	public static void waitOn( Object o ){
		synchronized( o ){
			try {
				o.wait();
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Wrap Object.wait() and throw RuntimeException instead of InterruptedException
	 */
	public static void waitOn( Object o, long timeout ){
		synchronized( o ){			
			try {
				o.wait( timeout );
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	public static void notifyAllOn( Object o ){
		synchronized(o){
			o.notifyAll();
		}
	}

	public static void dump(ByteBuffer bb) {
		
	}

	static void printTraceHexData(PrintWriter trc, ByteBuffer buf, int startPos, int endPos ) {
		int len = endPos - startPos;
		for( int i = 0; i < len; i += 16 ){
			trc.printf("    ");
			for( int k = 0; k+i < len && k < 16; k++ ){
				if( k > 0 ) {
					trc.print(" ");
					if( k % 4 == 0 ){
						trc.print(" ");							
					}
				}
				trc.printf("%02X", 0xFF & buf.get( i+k) );
			}
			trc.printf("%n");
		}
		trc.printf("    <<%n%n");
	}

	public static int transferRemaining( ByteBuffer src, ByteBuffer trg ){
		int xfer = Math.min( src.remaining(), trg.remaining() );
		int oldLimit = src.limit();
		src.limit( src.position() + xfer );
		trg.put( src );
		src.limit( oldLimit );
		return xfer;
	}
}
