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
using System;
using ByteBuffer = System.IO.MemoryStream;
using System.Threading;
using System.IO;
//using java.io.PrintWriter;
//using java.nio.ByteBuffer;

//using org.chabu.ChabuErrorCode;
//using org.chabu.ChabuException;
namespace org.chabu.intern
{

/**
 * 
 * @author Frank Benoit
 *
 */
public sealed class Utils {

    public static SystemException failX(String fmt, params object[] args)
    {
		throw new SystemException( String.Format( fmt, args ));
	}
	
	/**
	 * Throw an exception if the condition is not true.
	 * @param cond
	 */
	public static void ensureX( bool cond, String fmt, Object[] args ){
		if( cond ) return;
		throw new SystemException( String.Format( fmt, args ));
	}
	
    //public static void fail( ChabuErrorCode error, String fmt, Object[] args ){
    //    throw new ChabuException( error, String.Format( fmt, args ));
    //}

    public static void fail(int error, String fmt, params object[] args)
    {
		throw new ChabuException( error, String.Format( fmt, args ));
	}

    public static void ensure(bool cond, ChabuErrorCode error, String fmt, params object[] args)
    {
        if (cond) return;
        throw new ChabuException(error, String.Format(fmt, args));
    }
	
	public static void ensure( bool cond, int code, String fmt, params object[] args ){
		if( cond ) return;
		throw new ChabuException( code, String.Format( fmt, args ));
	}
	
	/**
	 * Throw an exception if the condition is not true.
	 * @param cond
	 */
	public static void ensureX( bool cond ){
		if( cond ) return;
		throw new SystemException();
	}
	
	/**
	 * Wrap Object.wait() and throw SystemException instead of InterruptedException
	 */
	public static void waitOn( Object o ){
		lock( o ){
			Monitor.Wait(o);
		}
	}

	/**
	 * Wrap Object.wait() and throw SystemException instead of InterruptedException
	 */
	public static void waitOn( Object o, long timeout ){
		lock( o ){			
			Monitor.Wait(o, System.TimeSpan.FromMilliseconds( timeout ));
		}
	}
	
	public static void notifyAllOn( Object o ){
		lock(o){
			Monitor.PulseAll(o);
		}
	}

	public static void dump(ByteBuffer bb) {
		
	}

	public static void printTraceHexData(TextWriter trc, ByteBuffer buf, int startPos, int endPos ) {
		int len = endPos - startPos;
		for( int i = 0; i < len; i += 16 ){
			trc.Write("    ");
			for( int k = 0; k+i < len && k < 16; k++ ){
				if( k > 0 ) {
                    trc.Write(" ");
					if( k % 4 == 0 ){
                        trc.Write(" ");							
					}
				}
                trc.Write("{0:X2}", 0xFF & buf.get(i + k));
			}
            trc.WriteLine();
		}
        trc.WriteLine("    <<");
        trc.WriteLine();
    }

	public static int transferRemaining( ByteBuffer src, ByteBuffer trg ){
		int xfer = Math.Min( src.remaining(), trg.remaining() );
		trg.Write( src.GetBuffer(), src.position(), xfer );
        src.Position += xfer;
		return xfer;
	}

	public static int transferUpTo( ByteBuffer src, ByteBuffer trg, int maxCount ){
        int xfer = Math.Min(Math.Min(src.remaining(), trg.remaining()), maxCount);
		//int oldLimit = src.limit();
		//src.limit( src.position() + xfer );
		trg.Write( src.GetBuffer(), (int)src.Position, xfer );
        src.Position += xfer;
		//src.limit( oldLimit );
		return xfer;
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
	public static int alignUpTo4(int v) {
		int tv = v & ~3;
		if( tv == v ) {
			return v;
		}
		else {
			return tv + 4;
		}
	}
}
}