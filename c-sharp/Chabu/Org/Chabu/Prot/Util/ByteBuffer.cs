using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Org.Chabu.Prot.V1.Internal;

namespace Org.Chabu.Prot.Util
{
    public class ByteBuffer
    {
        private readonly byte[] bytes;
        private readonly int bytesOffset;
        private readonly int arrayLength;
        private ByteOrder byteOrder;
        private int relPosition;
        private int relLimit;

        public static ByteBuffer allocate( int capacity)
        {
            return new ByteBuffer(capacity);
        }

        public ByteBuffer(int capacity)
            : this( new byte[capacity] )
        {
        }
        public ByteBuffer(byte[] bytes, int bytesOffset, int arrayLength)
        {
            this.bytes = bytes;
            this.bytesOffset = bytesOffset;
            this.arrayLength = arrayLength;
            relPosition = 0;
            relLimit = arrayLength;
        }
        public ByteBuffer(byte[] bytes)
            : this( bytes, 0, bytes.Length )
        {
        }

        public static ByteBuffer wrap(byte[] buffer)
        {
            return new ByteBuffer(buffer, 0, buffer.Length);
        }

        public ByteBuffer duplicate()
        {
            var res = new ByteBuffer(bytes, bytesOffset, arrayLength);
            res.relLimit = relLimit;
            res.relPosition = relPosition;
            res.byteOrder = byteOrder;
            return res;
        }

        public ByteOrder order()
        {
            return byteOrder;
        }

        public ByteBuffer order( ByteOrder byteOrder)
        {
            this.byteOrder = byteOrder;
            return this;
        }

        public int capacity()
        {
            return arrayLength;
        }
        public int position()
        {
            return relPosition;
        }
        public int limit()
        {
            return relLimit;
        }

        public ByteBuffer position(int newPos)
        {
            relPosition = newPos;
            return this;
        }
        public ByteBuffer limit(int newLimit)
        {
            relLimit = newLimit;
            return this;
        }


        public void compact()
        {
            int cpySz = remaining();
            if (relPosition > 0)
            {
                // bytes.Copy behaves like memmove
                Buffer.BlockCopy(bytes, bytesOffset + relPosition, bytes, 0, cpySz);
                //bytes.Copy(buffer, relPosition, buffer, 0, cpySz);
            }
            relLimit = arrayLength;
            relPosition = cpySz;
        }

        public void flip()
        {
            relLimit = relPosition;
            relPosition = 0;
        }
        public int remaining()
        {
            return relLimit - relPosition;
        }
        public bool hasRemaining()
        {
            return remaining() > 0;
        }


        public ByteBuffer get(sbyte[] trg)
        {
            Utils.ensure(relPosition + trg.Length >= arrayLength, 0, "{0} {1} {2}", relPosition, trg.Length, arrayLength);
            Array.Copy(
                bytes, relPosition + bytesOffset,
                trg, 0, trg.Length);
            relPosition += trg.Length;
            return this;
        }
        public ByteBuffer get( byte[] trg)
        {
            Utils.ensure(relPosition + trg.Length <= arrayLength, 0, "");
            Array.Copy(
                bytes, relPosition+bytesOffset,
                trg, 0, trg.Length);
            relPosition += trg.Length;
            return this;
        }
        public sbyte get()
        {
            sbyte res = get((int)relPosition);
            relPosition++;
            return res;
        }
        public sbyte get( int pos)
        {
            //            Utils.ensure(relPosition + 1 <= arrayLength, 0, "pos {0}, length {1}", relPosition, arrayLength );
            byte[] buffer = bytes;
            sbyte res = (sbyte)buffer[pos];
            return res;
        }
        public short getShort()
        {
            short res = getShort((int)relPosition);
            relPosition += 2;
            return res;
        }
        public short getShort(int pos)
        {
            Utils.ensure(pos >= 0 && pos + 2 <= arrayLength, 0, "");
            ushort res;
            byte[] buffer = bytes;
            if (byteOrder == ByteOrder.BIG_ENDIAN)
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
        public int getInt()
        {
            int res = getInt((int)relPosition);
            relPosition += 4;
            return res;
        }
        public int getInt(int pos)
        {
            Utils.ensure(pos >= 0 && pos + 4 <= arrayLength, 0, "");
            uint res;
            byte[] buffer = bytes;
            if (byteOrder == ByteOrder.BIG_ENDIAN)
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
        public long getLong()
        {
            long res = getLong((int)relPosition);
            relPosition += 8;
            return res;
        }
        public long getLong(int pos)
        {
            Utils.ensure(pos >= 0 && pos + 8 <= arrayLength, 0, "");
            long res;
            byte[] buffer = bytes;
            if (byteOrder == ByteOrder.BIG_ENDIAN)
            {
                res = buffer[pos];
                res <<= 8;
                res |= buffer[pos + 1];
                res <<= 8;
                res |= buffer[pos + 2];
                res <<= 8;
                res |= buffer[pos + 3];
                res <<= 8;
                res |= buffer[pos + 4];
                res <<= 8;
                res |= buffer[pos + 5];
                res <<= 8;
                res |= buffer[pos + 6];
                res <<= 8;
                res |= buffer[pos + 7];
            }
            else
            {
                res = buffer[pos + 7];
                res <<= 8;
                res |= buffer[pos + 6];
                res <<= 8;
                res |= buffer[pos + 5];
                res <<= 8;
                res |= buffer[pos + 4];
                res <<= 8;
                res |= buffer[pos + 3];
                res <<= 8;
                res |= buffer[pos + 2];
                res <<= 8;
                res |= buffer[pos + 1];
                res <<= 8;
                res |= buffer[pos];
            }
            return res;
        }
        public ByteBuffer put(sbyte[] bytes)
        {
            put(bytes, 0, bytes.Length);
            return this;
        }
        public ByteBuffer put(sbyte[] bytes, int offset, int length)
        {
            Utils.ensure(length <= remaining(), "ByteBuffer.put, overflow");
            Array.Copy(
                bytes, offset,
                array(), position() + arrayOffset(), bytes.Length);
            relPosition = relPosition + bytes.Length;
            return this;
        }
        public ByteBuffer put(byte[] bytes)
        {
            put(bytes, 0, bytes.Length);
            return this;
        }
        public ByteBuffer put(byte[] bytes, int offset, int length)
        {
            Utils.ensure(length <= remaining(), "ByteBuffer.put, overflow");
            Array.Copy(
                bytes, offset,
                array(), position() + arrayOffset(), bytes.Length);
            relPosition = relPosition + bytes.Length;
            return this;
        }
        public ByteBuffer put(ByteBuffer other)
        {
            int copySz = other.remaining();
            Array.Copy(
                other.array(), other.position()+other.arrayOffset(), 
                array(), position()+arrayOffset(), copySz);
            relPosition = relPosition + copySz;
            other.position(other.position() + copySz);
            return this;
        }
        public ByteBuffer put(sbyte value)
        {
            put( relPosition, value);
            relPosition = relPosition + 1;
            return this;
        }
        public ByteBuffer put( byte value)
        {
            put(relPosition, (sbyte)value);
            relPosition = relPosition + 1;
            return this;
        }
        public ByteBuffer put( int pos, sbyte value)
        {
            Utils.ensure(pos >= 0 && pos + 1 <= arrayLength, 0, "{0} {1}", pos, arrayLength);
            byte[] buffer = bytes;
            buffer[pos] = (byte)value;
            return this;
        }
        public ByteBuffer putShort( short value)
        {
            putShort(relPosition, value);
            relPosition = relPosition + 2;
            return this;
        }
        public ByteBuffer putShort( int pos, short value)
        {
            Utils.ensure(pos >= 0 && pos + 2 <= arrayLength, 0, "");
            byte[] buffer = bytes;
            if (byteOrder == ByteOrder.BIG_ENDIAN)
            {
                buffer[pos] = (byte)(value >> 8);
                buffer[pos + 1] = (byte)(value);
            }
            else
            {
                buffer[pos + 1] = (byte)(value >> 8);
                buffer[pos] = (byte)(value);
            }
            return this;
        }
        public ByteBuffer putInt(uint value)
        {
            return putInt((int)value);
        }
        public ByteBuffer putInt(int pos, uint value)
        {
            return putInt(pos, (int)value);
        }
        public ByteBuffer putInt(int value)
        {
            putInt(relPosition, value);
            relPosition = relPosition + 4;
            return this;
        }
        public ByteBuffer putInt( int pos, int value)
        {
            Utils.ensure(pos >= 0 && pos + 4 <= arrayLength, 0, "pos {0}, arrayLength {1}", pos, arrayLength);
            byte[] buffer = bytes;
            if (byteOrder == ByteOrder.BIG_ENDIAN)
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
            return this;
        }

        public ByteBuffer putLong(ulong value)
        {
            return putLong((long)value);
        }
        public ByteBuffer putLong(int pos, ulong value)
        {
            return putLong(pos, (long)value);
        }

        public ByteBuffer putLong(long value)
        {
            putLong(relPosition, value);
            relPosition = relPosition + 8;
            return this;
        }

        public ByteBuffer putLong(int pos, long value)
        {
            Utils.ensure(pos >= 0 && pos + 8 <= arrayLength, 0, "pos {0}, arrayLength {1}", pos, arrayLength);
            byte[] buffer = bytes;
            if (byteOrder == ByteOrder.BIG_ENDIAN)
            {
                buffer[pos + 0] = (byte)(value >> 56);
                buffer[pos + 1] = (byte)(value >> 48);
                buffer[pos + 2] = (byte)(value >> 40);
                buffer[pos + 3] = (byte)(value >> 32);
                buffer[pos + 4] = (byte)(value >> 24);
                buffer[pos + 5] = (byte)(value >> 16);
                buffer[pos + 6] = (byte)(value >> 8);
                buffer[pos + 7] = (byte)(value >> 0);
            }
            else
            {
                buffer[pos + 7] = (byte)(value >> 56);
                buffer[pos + 6] = (byte)(value >> 48);
                buffer[pos + 5] = (byte)(value >> 40);
                buffer[pos + 4] = (byte)(value >> 32);
                buffer[pos + 3] = (byte)(value >> 24);
                buffer[pos + 2] = (byte)(value >> 16);
                buffer[pos + 1] = (byte)(value >> 8);
                buffer[pos + 0] = (byte)(value >> 0);
            }
            return this;
        }



        public ByteBuffer clear()
        {
            relPosition = 0;
            relLimit = arrayLength;
            return this;
        }


        public byte[] array()
        {
            return bytes;
        }

        public int arrayOffset()
        {
            return bytesOffset;
        }








    }
}
