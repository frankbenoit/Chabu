using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;

using StringBuilder = System.Text.StringBuilder;
using RuntimeException = System.SystemException;

namespace org.chabu
{


    public class DataGen
    {

        PseudoRandom gen;
        PseudoRandom exp;
        long genPos = 0;
        long expPos = 0;
        private String name;

        public DataGen(String name, long seed)
        {
            this.name = name;
            gen = new PseudoRandom(seed);
            exp = new PseudoRandom(seed);
        }

        public void getGenBytes(byte[] bytes, int offset, int length)
        {
            gen.nextBytes(bytes, offset, length);
            genPos += length;
        }

        public String getGenBytesString(int numBytes)
        {
            genPos += numBytes;
            return getBytesString(gen, numBytes);
        }

        public void getExpBytes(byte[] bytes, int offset, int length)
        {
            expPos += length;
            exp.nextBytes(bytes, offset, length);
        }

        public String getExpBytesString(int numBytes)
        {
            expPos += numBytes;
            return getBytesString(exp, numBytes);
        }

        private String getBytesString(PseudoRandom rnd, int numBytes)
        {
            if (numBytes == 0)
            {
                return "";
            }
            byte[] bytes = new byte[numBytes];
            rnd.nextBytes(bytes, 0, numBytes);
            StringBuilder sb = new StringBuilder(numBytes * 3 - 1);
            for (int i = 0; i < numBytes; i++)
            {
                if (i > 0) sb.append(" ");
                sb.append(toHexDigit((bytes[i] & 0xF0) >> 4));
                sb.append(toHexDigit((bytes[i] & 0x0F) >> 0));
            }
            return sb.toString();
        }
        private char toHexDigit(int digit)
        {
            if (digit < 10) return (char)('0' + digit);
            return (char)('A' + digit - 10);
        }
        public void ensureSamePosition()
        {
            if (genPos != expPos)
            {
                throw new RuntimeException(String.Format("DataGen {0}, positions not equal: exp:{1} gen:{2}", name, expPos, genPos));
            }
        }
    }
}