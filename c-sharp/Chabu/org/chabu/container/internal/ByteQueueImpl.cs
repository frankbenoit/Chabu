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
namespace org.chabu.container.intern{
using System;
    using org.chabu.container;

/**
 * 
 * @author Frank Benoit
 *
 */
    internal sealed class ByteQueueImpl : ByteQueue
    {

        public static bool useAsserts = true;
        public String name;
        public byte[] buf;

        public void Assert(bool cond)
        {
            if (cond) return;
            throw new SystemException(String.Format("ByteQueue ({0})", name));
        }
        public void AssertPrintf(bool cond, String str, params object[] args)
        {
            if (cond) return;
            throw new SystemException(String.Format("ByteQueue ({0}): {1}", name, String.Format(str, args)));
        }

        public readonly ByteQueueInputPortImpl inport;
        public readonly ByteQueueOutputPortImpl outport;

        public ByteQueueImpl(String name, int capacity)
        {
            this.name = name;
            this.buf = new byte[capacity + 1];
            inport = new ByteQueueInputPortImpl(this);
            outport = new ByteQueueOutputPortImpl(this);
        }

        //@Override
        public int capacity()
        {
            return this.buf.Length - 1;
        }

        //@Override
        public ByteQueueInputPort getInport()
        {
            return inport;
        }

        //@Override
        public ByteQueueOutputPort getOutport()
        {
            return outport;
        }


        //@Override
        public String toString()
        {
            return String.Format("ByteQueue[ cap={0}, {1}, {2} ]", capacity(), inport, outport);
        }


    }
}
