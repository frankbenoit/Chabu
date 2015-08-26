using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using org.chabu.intern;

namespace org.chabu
{
    public static class Extensions
    {
        public static bool isByteOrderBigEndian = true;

        public static void compact(this System.IO.MemoryStream ms)
        {
            int cpySz = ms.remaining();
            {
                byte[] buffer = ms.GetBuffer();
                // Array.Copy behaves like memmove
                Array.Copy(buffer, ms.Position, buffer, 0, cpySz);
            }
            ms.limit(ms.capacity());
            ms.position(cpySz);
        }
        public static void flip(this System.IO.MemoryStream ms)
        {
            ms.SetLength( ms.Position );
            ms.Position = 0;
        }
        public static int remaining(this System.IO.MemoryStream ms)
        {
            return (int)(ms.Length - ms.Position);
        }
        public static bool hasRemaining(this System.IO.MemoryStream ms)
        {
            return ms.remaining() > 0;
        }
        public static int position(this System.IO.MemoryStream ms)
        {
            return (int)ms.Position;
        }
        public static System.IO.MemoryStream position(this System.IO.MemoryStream ms, int pos)
        {
            ms.Position = pos;
            return ms;
        }
        public static int limit(this System.IO.MemoryStream ms)
        {
            return (int)ms.Length;
        }
        public static System.IO.MemoryStream limit(this System.IO.MemoryStream ms, int lim)
        {
            ms.SetLength(lim);
            return ms;
        }

        public static System.IO.MemoryStream get(this System.IO.MemoryStream ms, sbyte[] trg)
        {
            org.chabu.intern.Utils.ensure(ms.Position + trg.Length >= ms.Length, 0, "{0} {1} {2}", ms.Position, trg.Length, ms.Length );
            Array.Copy(ms.GetBuffer(), ms.Position, trg, 0, trg.Length);
            ms.Position += trg.Length;
            return ms;
        }
        public static System.IO.MemoryStream get(this System.IO.MemoryStream ms, byte[] trg)
        {
            Utils.ensure(ms.Position + trg.Length <= ms.Length, 0, "");
            Array.Copy(ms.GetBuffer(), ms.Position, trg, 0, trg.Length);
            ms.Position += trg.Length;
            return ms;
        }
        public static sbyte get(this System.IO.MemoryStream ms)
        {
            sbyte res = get(ms, (int)ms.Position);
            ms.Position++;
            return res;
        }
        public static sbyte get(this System.IO.MemoryStream ms, int pos )
        {
            Utils.ensure(ms.Position + 1 <= ms.Length, 0, "" );
            byte[] buffer = ms.GetBuffer();
            sbyte res = (sbyte)buffer[pos];
            return res;
        }
        public static short getShort(this System.IO.MemoryStream ms)
        {
            short res = getShort(ms, (int)ms.Position);
            ms.Position += 2;
            return res;
        }
        public static short getShort(this System.IO.MemoryStream ms, int pos)
        {
            Utils.ensure(pos >= 0 && pos + 2 >= ms.Length, 0, "" );
            ushort res;
            byte[] buffer = ms.GetBuffer();
            if (isByteOrderBigEndian)
            {
                res = buffer[pos];
                res <<= 8;
                res |= (ushort)buffer[pos + 1];
            }
            else
            {
                res = buffer[pos + 1];
                res <<= 8;
                res |= (ushort)buffer[pos];
            }
            return (short)res;
        }
        public static int getInt(this System.IO.MemoryStream ms)
        {
            int res = getInt(ms, (int)ms.Position);
            ms.Position += 4;
            return res;
        }
        public static int getInt(this System.IO.MemoryStream ms, int pos)
        {
            Utils.ensure(pos >= 0 && pos + 4 <= ms.Length, 0, "");
            uint res;
            byte[] buffer = ms.GetBuffer();
            if (isByteOrderBigEndian)
            {
                res = buffer[pos];
                res <<= 8;
                res |= buffer[pos + 1];
                res <<= 8;
                res |= buffer[pos + 2];
                res <<= 8;
                res |= buffer[pos + 3];
            }
            else
            {
                res = buffer[pos + 3];
                res <<= 8;
                res |= buffer[pos + 2];
                res <<= 8;
                res |= buffer[pos + 1];
                res <<= 8;
                res |= buffer[pos];
            }
            return (int)res;
        }
        public static System.IO.MemoryStream put(this System.IO.MemoryStream ms, sbyte[] bytes)
        {
            Array.Copy(bytes, 0, ms.GetBuffer(), ms.Position, bytes.Length);
            ms.position(ms.position() + bytes.Length);
            return ms;
        }
        public static System.IO.MemoryStream put(this System.IO.MemoryStream ms, byte[] bytes)
        {
            Array.Copy(bytes, 0, ms.GetBuffer(), ms.Position, bytes.Length);
            ms.position(ms.position() + bytes.Length);
            return ms;
        }
        public static System.IO.MemoryStream put(this System.IO.MemoryStream ms, System.IO.MemoryStream other)
        {
            int copySz = other.remaining();
            Array.Copy(other.GetBuffer(), other.Position, ms.GetBuffer(), ms.Position, copySz);
            ms.position(ms.position() + copySz);
            other.position(other.position() + copySz);
            return ms;
        }
        public static System.IO.MemoryStream put(this System.IO.MemoryStream ms, sbyte value)
        {
            put(ms, ms.position(), value);
            ms.position(ms.position() + 1);
            return ms;
        }
        public static System.IO.MemoryStream put(this System.IO.MemoryStream ms, byte value)
        {
            put(ms, ms.position(), (sbyte)value);
            ms.position(ms.position() + 1);
            return ms;
        }
        public static System.IO.MemoryStream put(this System.IO.MemoryStream ms, int pos, sbyte value)
        {
            Utils.ensure(pos >= 0 && pos + 1 <= ms.Length, 0, "{0} {1}", pos, ms.Length );
            byte[] buffer = ms.GetBuffer();
            buffer[pos] = (byte)value;
            return ms;
        }
        public static System.IO.MemoryStream putShort(this System.IO.MemoryStream ms, short value)
        {
            putShort(ms, ms.position(), value);
            ms.position(ms.position() + 2);
            return ms;
        }
        public static System.IO.MemoryStream putShort(this System.IO.MemoryStream ms, int pos, short value)
        {
            Utils.ensure(pos >= 0 && pos + 2 <= ms.Length, 0, "");
            byte[] buffer = ms.GetBuffer();
            if (isByteOrderBigEndian)
            {
                buffer[pos] = (byte)(value >> 8);
                buffer[pos+1] = (byte)(value);
            }
            else
            {
                buffer[pos+1] = (byte)(value >> 8);
                buffer[pos] = (byte)(value);
            }
            return ms;
        }
        public static System.IO.MemoryStream putInt(this System.IO.MemoryStream ms, int value)
        {
            putInt(ms, ms.position(), value);
            ms.position(ms.position() + 4);
            return ms;
        }
        public static System.IO.MemoryStream putInt(this System.IO.MemoryStream ms, int pos, int value)
        {
            Utils.ensure(pos >= 0 && pos + 4 <= ms.Length, 0, "pos {0}, ms.Length {1}", pos, ms.Length );
            byte[] buffer = ms.GetBuffer();
            if (isByteOrderBigEndian)
            {
                buffer[pos + 0] = (byte)(value >> 24);
                buffer[pos + 1] = (byte)(value >> 16);
                buffer[pos + 2] = (byte)(value >> 8);
                buffer[pos + 3] = (byte)(value >> 0);
            }
            else
            {
                buffer[pos + 3] = (byte)(value >> 24);
                buffer[pos + 2] = (byte)(value >> 16);
                buffer[pos + 1] = (byte)(value >> 8);
                buffer[pos + 0] = (byte)(value >> 0);
            }
            return ms;
        }

        public static byte[] array(this System.IO.MemoryStream ms)
        {
            return ms.GetBuffer();
        }

        public static int arrayOffset(this System.IO.MemoryStream ms)
        {
            return 0;
        }
        public static int capacity(this System.IO.MemoryStream ms)
        {
            return ms.Capacity;
        }
        public static System.IO.MemoryStream clear(this System.IO.MemoryStream ms)
        {
            ms.position(0);
            ms.limit(ms.Capacity);
            return ms;
        }

        public static ChabuChannelImpl get(this List<ChabuChannelImpl> list, int pos)
        {
            return list[pos];
        }
        public static void add(this List<ChabuChannelImpl> list, ChabuChannelImpl c)
        {
            list.Add(c);
        }
        public static int size(this List<ChabuChannelImpl> list)
        {
            return list.Count;
        }


        public static int size(this System.Collections.BitArray ba)
        {
            return ba.Count;
        }
        public static void set(this System.Collections.BitArray ba, int firstIndex, int lastIndex)
        {
            for (int i = firstIndex; i < lastIndex; i++)
            {
                ba.Set(i, true);
            }
        }
        public static void set(this System.Collections.BitArray ba, int index)
        {
            ba.Set(index, true);
        }
        public static void clear(this System.Collections.BitArray ba, int index)
        {
            ba.Set(index, false);
        }
        public static int nextSetBit(this System.Collections.BitArray ba, int firstIndex)
        {
            int idx = firstIndex;
            while (idx < ba.Count)
            {
                if (ba.Get(idx))
                {
                    return idx;
                }
                idx++;
            }
            return -1;
        }

        // array fill
        public static void Fill<T>(this T[] originalArray, int startIdx, int endIdx, T with)
        {
            for (int i = startIdx; i < endIdx; i++)
            {
                originalArray[i] = with;
            }
        } 
        
 
        ///////////////////////////////////////////////////////
        // StringBuilder
        public static void append(this StringBuilder sb, string str)
        {
            sb.Append(str);
        }
        public static void append(this StringBuilder sb, char c)
        {
            sb.Append(c);
        }
        public static string toString(this StringBuilder sb)
        {
            return sb.ToString();
        }
        public static int length(this StringBuilder sb)
        {
            return sb.Length;
        }

        ///////////////////////////////////////////////////////
        // String
        public static string toString(this String s)
        {
            return s.ToString();
        }
        public static bool isEmpty(this String s)
        {
            return s.Length == 0;
        }
        public static int length(this String s)
        {
            return s.Length;
        }
        public static char charAt(this String s, int idx)
        {
            return s[idx];
        }
        public static string trim(this String s)
        {
            return s.Trim();
        }
        public static bool equals(this String s, string o)
        {
            return s.Equals(o);
        }
        public static byte[] getBytes(this String s, Encoding enc)
        {
            return enc.GetBytes(s);
        }

        public static int length(this byte[] a)
        {
            return a.Length;
        }

    }
}
