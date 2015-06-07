package chabu.internal;

import java.io.PrintWriter;
import java.nio.ByteBuffer;

public class Utils {
	
	public static RuntimeException fail( String fmt, Object ... args ){
		throw new RuntimeException( String.format( fmt, args ));
	}
	
	/**
	 * Throw an exception if the condition is not true.
	 * @param cond
	 */
	public static void ensure( boolean cond, String fmt, Object ... args ){
		if( cond ) return;
		throw new RuntimeException( String.format( fmt, args ));
	}
	
	/**
	 * Throw an exception if the condition is not true.
	 * @param cond
	 */
	public static void ensure( boolean cond ){
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


}
