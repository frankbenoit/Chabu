package org.chabu;

public class Random {

    private long seed;
    private byte[] buffer = new byte[0x2000];
    private int idx = 0;

    private static final long multiplier = 0x5_DEEC_E66DL;
    private static final long addend     = 0x0BL;
    private static final long mask       = (1L << 48) - 1;

    public Random(long seed) {
        this.seed = (seed ^ multiplier) & mask;
        for (int i = 0; i < buffer.length; ) {
        	int v = nextInt_();
           	buffer[i++ ] = (byte)(v >>> 24);
           	buffer[i++ ] = (byte)(v >>> 16);
           	buffer[i++ ] = (byte)(v >>>  8);
           	buffer[i++ ] = (byte)(v);
        }
        
    }

    public void nextBytes(byte[] bytes, int offset, int length ) {
    	int endOffset = offset+length;
    	while( offset < endOffset ){
    		int cpySz = Math.min( endOffset - offset, buffer.length - idx );
    		try{
    			
    			System.arraycopy( buffer, idx, bytes, offset, cpySz );
    		}
    		catch( IndexOutOfBoundsException e){
    			System.out.printf("%s %s %s %s %s \n", buffer.length, idx, bytes.length, offset, cpySz );
    			throw e;
    		}
    		offset += cpySz;
    		idx += cpySz;
    		if( idx == buffer.length ){
    			idx = 0;
    		}
    	}
    }

    public byte nextByte() {
    	byte res = buffer[ idx++ % buffer.length ];
    	if( idx == buffer.length ){
    		idx = 0;
    	}
    	return res;
    }

    private int nextInt_() {
    	this.seed = (this.seed * multiplier + addend) & mask;
    	return (int)(this.seed >>> 16);
    }
    
}
