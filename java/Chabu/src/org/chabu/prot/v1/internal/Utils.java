/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit, Stuttgart, Germany <keinfarbton@gmail.com>
 * 
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 * 
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
package org.chabu.prot.v1.internal;

import java.io.PrintWriter;
import java.nio.ByteBuffer;

import org.chabu.prot.v1.ChabuErrorCode;
import org.chabu.prot.v1.ChabuException;

/**
 * 
 * @author Frank Benoit
 *
 */
public final class Utils {
	
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

	/**
	 * Aligns the value to the next 4-byte value if needed.
	 * <p/>
	 * <pre>
	 * 4 &rarr; 4
	 * 5 &rarr; 8
	 * 6 &rarr; 8
	 * 7 &rarr; 8
	 * 8 &rarr; 8
	 * 9 &rarr; 12
	 * ...
	 * </pre>
	 * 
	 */
	public static final int alignUpTo4(int v) {
		int tv = v & ~3;
		if( tv == v ) {
			return v;
		}
		else {
			return tv + 4;
		}
	}

	/**
	 * Tests if the value dividable by 4 with no reminder.
	 * <p/>
	 * <pre>
	 * 4 &rarr; true
	 * 5 &rarr; false
	 * 6 &rarr; false
	 * 7 &rarr; false
	 * 8 &rarr; true
	 * 9 &rarr; false
	 * ...
	 * </pre>
	 * 
	 */
	public static boolean isAligned4(int value) {
		return ( value & 3 ) == 0;
	}

	public static int safePosInt(long value) {
		if( value < 0 || value > Integer.MAX_VALUE ){
			fail(ChabuErrorCode.ASSERT, "" );
		}
		return (int)value;
	}
	public static int safeInt(long value) {
		if( value < Integer.MIN_VALUE || value > Integer.MAX_VALUE ){
			fail(ChabuErrorCode.ASSERT, "" );
		}
		return (int)value;
	}

	public static RuntimeException implMissing() {
		throw new UnsupportedOperationException("Impl missing");
	}

}
