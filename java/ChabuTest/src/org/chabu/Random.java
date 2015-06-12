package org.chabu;



public
class Random  {

    private long seed;

    private static final long multiplier = 0x5DEECE66DL;
    private static final long addend = 0xBL;
    private static final long mask = (1L << 48) - 1;

    public Random(long seed) {
        this.seed = initialScramble(seed);
    }

    private static long initialScramble(long seed) {
        return (seed ^ multiplier) & mask;
    }


    protected int next(int bits) {
        long oldseed, nextseed;
        long seed = this.seed;
        do {
            oldseed = seed;
            nextseed = (oldseed * multiplier + addend) & mask;
        } while (!compareAndSet(oldseed, nextseed));
        return (int)(nextseed >>> (48 - bits));
    }

    private boolean compareAndSet( long exp, long upd ){
    	if( seed == exp ){
    		seed = upd;
    		return true;
    	}
    	return false;
    }
    public void nextBytes(byte[] bytes, int offset, int length ) {
        for (int i = 0; i < length; ) {
        	int rnd = nextInt(), n = Math.min(length - i, Integer.SIZE/Byte.SIZE);
            for ( ; n-- > 0; rnd >>= Byte.SIZE ) {
            	bytes[offset+i++] = (byte)rnd;
            }
        }
    }

    public int nextInt() {
        return next(32);
    }

}
