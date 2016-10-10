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
    using Org.Chabu.Prot.V1;
    using ByteBuffer = global::System.IO.MemoryStream;

    public class TestByteChannel : ByteChannel
    {
        private ByteBuffer readData;
        private ByteBuffer writeData;

        public TestByteChannel()
        {
        }
        public TestByteChannel(int recvCapacity, int xmitCapacity)
        {
            readData = new ByteBuffer(recvCapacity);
            readData.limit(recvCapacity);
            writeData = new ByteBuffer(xmitCapacity);
            writeData.limit(0);
        }

        public void putRecvData(ByteBuffer readData)
        {
            this.readData.put(readData);
        }

        public void putRecvData(String hexString)
        {
            byte[] data = TestUtils.hexStringToByteArray(hexString.Trim());
            readData.put(data);
        }

        public void resetXmitRecording(int length)
        {
            this.writeData.clear();
            writeData.limit(length);
        }
        public ByteBuffer getWriteData()
        {
            return writeData;
        }


        public bool isOpen()
        {
            return true;
        }

        public void close()
        {
        }

        public int read(ByteBuffer dst)
        {
            readData.flip();

            int res = ByteBufferUtils.transferRemaining(readData, dst);
            readData.compact();
            return res;
        }

        public int write(ByteBuffer src)
        {
            int count = 0;
            while (writeData.hasRemaining() && src.hasRemaining())
            {
                writeData.put(src.get());
                count++;
            }
            return count;
        }
    }
}