package chabu.tester.dut;

public class Tausworthe  {
    private final long seed;

    private long s1;
    private long s2;
    private long s3;

    public Tausworthe() {
        seed = 5323;
        setSeed(seed);
    }

    public Tausworthe(long seed) {
        this.seed = seed;
        setSeed(seed);
    }

    private long getLCG(long n) {
        return (69069 * n) & 0xffffffffL;
    }

    private void setSeed(long seed) {
        if (seed == 0) {
            seed = 1;
        }

        s1 = getLCG(seed);
        if (s1 < 2) {
            s1 += 2;
        }
        s2 = getLCG(s1);
        if (s2 < 8) {
            s2 += 8;
        }
        s3 = getLCG(s2);
        if (s3 < 16) {
            s3 += 16;
        }

        next(32);
        next(32);
        next(32);
        next(32);
        next(32);
        next(32);
    }

    private int next(int bits) {
        s1 = (((s1 & 4294967294L) << 12) & 0xffffffffL) ^ ((((s1 << 13) & 0xffffffffL) ^ s1) >>> 19);
        s2 = (((s2 & 4294967288L) <<  4) & 0xffffffffL) ^ ((((s2 <<  2) & 0xffffffffL) ^ s2) >>> 25);
        s3 = (((s3 & 4294967280L) << 17) & 0xffffffffL) ^ ((((s3 <<  3) & 0xffffffffL) ^ s3) >>> 11);

        return (int) ((s1 ^ s2 ^ s3) >>> (32 - bits));
    }

    public boolean nextBoolean() {
        return next(1) != 0;
    }

    public int nextInt() {
        return next(32);
    }

    public int nextInt(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("n must be positive");
        }

        if ((n & -n) == n) // i.e., n is a power of 2
        {
            return (int) ((n * (long) next(31)) >> 31);
        }

        int bits, val;
        do {
            bits = next(31);
            val = bits % n;
        } while (bits - val + (n - 1) < 0);
        return val;
    }

    public long nextLong() {
        return ((long) (next(32)) << 32) + next(32);
    }

    public float nextFloat() {
        return next(24) / ((float) (1 << 24));
    }

    public double nextDouble() {
        return (((long) next(26) << 27) + next(27)) / (double) (1L << 53);
    }

    public void nextBytes(byte[] bytes) {
        for (int i = 0, len = bytes.length; i < len;) {
            for (int rnd = nextInt(),
                    n = Math.min(len - i, Integer.SIZE / Byte.SIZE);
                    n-- > 0; rnd >>= Byte.SIZE) {
                bytes[i++] = (byte) rnd;
            }
        }
    }
}