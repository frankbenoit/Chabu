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
namespace org.chabu
{


    using ByteBuffer = System.IO.MemoryStream;

    using org.chabu.container;

    public class ChabuChannelUserDefault : ChabuChannelUser
    {
        protected ByteBuffer recv;
        protected ByteBuffer xmit;
        protected ChabuChannel channel;

        public ChabuChannelUserDefault(ByteBuffer recv, ByteBuffer xmit)
        {
            this.recv = recv;
            this.xmit = xmit;
            recv.limit(recv.position());
        }

        //override
        public void setChannel(ChabuChannel channel)
        {
            this.channel = channel;
        }

        //override
        public bool xmitEvent(ByteBuffer bufferToFill)
        {
            xmit.flip();
            int oldLimit = xmit.limit();
            if (xmit.remaining() > bufferToFill.remaining())
            {
                xmit.limit(xmit.position() + bufferToFill.remaining());
            }
            bufferToFill.put(xmit);
            xmit.limit(oldLimit);
            xmit.compact();

            return false;
        }

        //override
        public void recvEvent(ByteQueueOutputPort queueOutput)
        {
            recv.compact();
            queueOutput.poll(recv);
            queueOutput.commit();
            recv.flip();
        }

    }
}