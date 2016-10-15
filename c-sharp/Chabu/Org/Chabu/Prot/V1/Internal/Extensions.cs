/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit, Stuttgart, Germany <fr@nk-benoit.de>
 * 
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 * 
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;


namespace Org.Chabu.Prot.V1.Internal
{
    using ByteBuffer = Org.Chabu.Prot.Util.ByteBuffer;
    internal class StandardCharset
    {
    }
    internal static class StandardCharsets
    {
        public readonly static StandardCharset UTF_8 = new StandardCharset();
    }
    internal static class System
    {
        public static void arraycopy( byte[] src, int srcOffset, byte[] trg, int trgOffset, int length )
        {
            Array.Copy(src, srcOffset, trg, trgOffset, length);
        }
    }
    internal static class Extensions
    {

        //private static System.Runtime.CompilerServices.ConditionalWeakTable<System.IO.MemoryStream, Attrbutes> byteOrderAttributes = new System.Runtime.CompilerServices.ConditionalWeakTable<System.IO.MemoryStream, Attrbutes>();


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


        public static int size(this global::System.Collections.BitArray ba)
        {
            return ba.Count;
        }
        public static void set(this global::System.Collections.BitArray ba, int firstIndex, int lastIndex)
        {
            for (int i = firstIndex; i < lastIndex; i++)
            {
                ba.Set(i, true);
            }
        }
        public static void set(this global::System.Collections.BitArray ba, int index)
        {
            ba.Set(index, true);
        }
        public static void clear(this global::System.Collections.BitArray ba, int index)
        {
            ba.Set(index, false);
        }
        public static int nextSetBit(this global::System.Collections.BitArray ba, int firstIndex)
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
        public static byte[] getBytes(this String s, StandardCharset cs)
        {
            if( cs == StandardCharsets.UTF_8)
            {
                return s.getBytes(Encoding.UTF8);
            }
            throw new SystemException();
        }

        public static int length(this byte[] a)
        {
            return a.Length;
        }


    }
}
