package org.chabu;

public class Random {

    private long seed;

    private static final long multiplier = 0x5_DEEC_E66DL;
    private static final long addend     = 0x0BL;
    private static final long mask       = (1L << 48) - 1;

    public Random(long seed) {
        this.seed = (seed ^ multiplier) & mask;
    }

    public void nextBytes(byte[] bytes, int offset, int length ) {
        for (int i = 0; i < length; ) {
           	bytes[offset+i++] = (byte)nextInt();
        }
    }

    public int nextInt() {
        this.seed = (this.seed * multiplier + addend) & mask;
        return (int)(this.seed >>> 16);
    }

}
