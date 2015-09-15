using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace org.chabu
{

    using RuntimeException = System.SystemException;

    public class PseudoRandom {

        private const int BUFFER_SZ = 0x2000;
	    private long seed;
        private byte[] buffer = new byte[BUFFER_SZ];
        private int idx = 0;

        private const long multiplier = 0x5DEECE66DL;
        private const long addend     = 0x0BL;
        private const long mask       = (1L << 48) - 1;

        public PseudoRandom(long seed) {
            this.seed = (seed ^ multiplier) & mask;
            for (int i = 0; i < buffer.Length; ) {
        	    int v = nextInt_();
           	    buffer[i++ ] = (byte)(v >> 24);
           	    buffer[i++ ] = (byte)(v >> 16);
           	    buffer[i++ ] = (byte)(v >>  8);
           	    buffer[i++ ] = (byte)(v);
            }
        
        }

        public void nextBytes(byte[] bytes, int offset, int length ) {
    	    int endOffset = offset+length;
    	    while( offset < endOffset ){
    		    int cpySz = Math.Min( endOffset - offset, BUFFER_SZ - idx );
    		    Array.Copy( buffer, idx, bytes, offset, cpySz );
    		    offset += cpySz;
    		    idx += cpySz;
    		    if( idx == BUFFER_SZ ){
    			    idx = 0;
    		    }
    	    }
        }

	    public void nextBytesVerify(byte[] bytes, int offset, int length ) {
		    for( int i = 0; i < length; i++ ){
			    byte exp = buffer[ idx % buffer.Length ];
			    byte cur = bytes[i+offset];
			    idx++;
                if (idx == BUFFER_SZ)
                {
                    idx = 0;
                }
                if (exp != cur)
                {
                    throw new RuntimeException(String.Format("mismatch at {0}: exp:0x{1:X2} != cur:0x{2:X2}", i, exp, cur));
			    }
		    }
	    }
    
        public byte nextByte() {
    	    byte res = buffer[ idx ];
            idx++;
    	    if( idx == BUFFER_SZ ){
    		    idx = 0;
    	    }
    	    return res;
        }

        private int nextInt_() {
    	    this.seed = (this.seed * multiplier + addend) & mask;
    	    return (int)(this.seed >> 16);
        }

    }
}