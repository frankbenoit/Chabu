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
using System.Diagnostics;

namespace Org.Chabu.Prot.V1.Internal
{
    using ByteBuffer = Org.Chabu.Prot.Util.ByteBuffer;

    public class ByteBufferUtils
    {
        public static int transferRemaining(ByteBuffer src, ByteBuffer trg)
        {
            int xfer = Math.Min(src.remaining(), trg.remaining());

            Array.Copy(src.array(), src.position(), trg.array(), trg.position(), xfer);
            trg.position(trg.position() + xfer);
            src.position(src.position() + xfer);

            return xfer;
        }

        public static int XtransferUpTo(ByteBuffer src, ByteBuffer trg, int maxCount)
        {
            int xfer = Math.Min(Math.Min(src.remaining(), trg.remaining()), maxCount);
            int oldLimit = src.limit();
            src.limit(src.position() + xfer);
            trg.put(src);
            src.limit(oldLimit);
            return xfer;
        }

        /**
         * Copy as many byte from source to target, until either source has no more data, target cannot take more or 
         * the trg.position() is equals to limit value.
         */
        public static void transferUntilTargetPos(ByteBuffer src, ByteBuffer trg, int trgPos)
        {
            int cpySz = Math.Min(src.remaining(), Math.Min(trgPos - trg.position(), trg.remaining()));
            if (cpySz <= 0)
            {
                return;
            }
            int oldLimit = src.limit();
            src.limit(src.position() + cpySz);
            trg.put(src);
            src.limit(oldLimit);
        }


    }
}