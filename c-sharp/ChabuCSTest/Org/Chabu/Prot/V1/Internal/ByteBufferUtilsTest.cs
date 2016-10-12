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

namespace Org.Chabu.Prot.V1.Internal
{
    using Microsoft.VisualStudio.TestTools.UnitTesting;
    using ByteBuffer = global::System.IO.MemoryStream;

    [TestClass]
    public class ByteBufferUtilsTest
    {

        private static readonly int TRG_PATTERN = 0xA5;
        private static readonly int SRC_PATTERN = 0xF3;


        [TestMethod]
        public void testTransferUntilTargetPos()
        {
            ByteBuffer src = new ByteBuffer(100).clear();
            ByteBuffer trg = new ByteBuffer(100).clear();

            bbSetup(src, SRC_PATTERN, 0, 100);
            bbSetup(trg, TRG_PATTERN, 0, 100);

            ByteBufferUtils.transferUntilTargetPos(src, trg, 8);

            bbTestPosLim(trg, 8, 100);
            bbTestContent(trg, 0, 8);
        }

        [TestMethod]
        public void testTransferUntilTargetPos_TargetTooSmall()
        {
            ByteBuffer src = new ByteBuffer(100).clear();
            ByteBuffer trg = new ByteBuffer(100).clear();

            bbSetup(src, SRC_PATTERN, 0, 100);
            bbSetup(trg, TRG_PATTERN, 10, 12);

            ByteBufferUtils.transferUntilTargetPos(src, trg, trg.position() + 8);

            bbTestPosLim(trg, 12, 12);
            bbTestContent(trg, 10, 12);
        }

        [TestMethod]
        public void testTransferUntilTargetPos_SourceTooSmall()
        {
            ByteBuffer src = new ByteBuffer(100).clear();
            ByteBuffer trg = new ByteBuffer(100).clear();

            bbSetup(src, SRC_PATTERN, 0, 5);
            bbSetup(trg, TRG_PATTERN, 10, 20);

            ByteBufferUtils.transferUntilTargetPos(src, trg, trg.position() + 8);

            bbTestPosLim(trg, 15, 20);
            bbTestContent(trg, 10, 15);
        }

        [TestMethod]
        public void testTransferRemaining_SourceTooSmall()
        {
            ByteBuffer src = new ByteBuffer(100).clear();
            ByteBuffer trg = new ByteBuffer(100).clear();

            bbSetup(src, SRC_PATTERN, 0, 5);
            bbSetup(trg, TRG_PATTERN, 0, 20);

            ByteBufferUtils.transferRemaining(src, trg);

            bbTestPosLim(trg, 5, 20);
            bbTestContent(trg, 0, 5);
        }

        [TestMethod]
        public void testTransferRemaining_TargetTooSmall()
        {
            ByteBuffer src = new ByteBuffer(100).clear();
            ByteBuffer trg = new ByteBuffer(100).clear();

            bbSetup(src, SRC_PATTERN, 0, 20);
            bbSetup(trg, TRG_PATTERN, 5, 10);

            ByteBufferUtils.transferRemaining(src, trg);

            bbTestPosLim(trg, 10, 10);
            bbTestContent(trg, 5, 10);
        }

        private void bbTestPosLim(ByteBuffer trg, int pos, int limit)
        {
            Assert.AreEqual(pos, trg.position());
            Assert.AreEqual(limit, trg.limit());
        }

        private void bbTestContent(ByteBuffer bb, int startPos, int endPos)
        {
            int i = 0;
            for (; i < startPos && i < bb.limit(); i++)
            {
                sbyte b = bb.get(i);
                Assert.AreEqual(TRG_PATTERN, b & 0xFF, String.Format("Index %d", i));
            }
            for (; i < endPos && i < bb.limit(); i++)
            {
                sbyte b = bb.get(i);
                Assert.AreEqual(SRC_PATTERN, b & 0xFF, String.Format("Index %d", i));
            }
            for (; i < bb.limit(); i++)
            {
                sbyte b = bb.get(i);
                Assert.AreEqual(TRG_PATTERN, b & 0xFF, String.Format("Index %d", i));
            }
        }

        private void bbSetup(ByteBuffer src, int fillPattern, int pos, int limit)
        {
            src.clear();
            for (int i = 0; i < src.capacity(); i++)
            {
                src.put(i, (sbyte)fillPattern);
            }
            src.position(pos);
            src.limit(limit);
        }

    }
}