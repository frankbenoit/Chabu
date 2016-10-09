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
using System;

namespace Org.Chabu.Prot.V1
{
    public interface ChabuRecvByteTarget
    {

        /**
	     * Ask for a ByteBuffer with at least the given amount of bytes remaining.
	     * Depending on how the interface is used, the buffer might be used for receiving or for transmitting.
	     * The caller must call {@link #recvCompleted()} when done with the buffer. 
	     * @param size
	     * @return
	     */
        ByteBuffer getRecvBuffer(int size);

        /**
	     * Notification from Chabu, that the given buffer from the call to {@link #getRecvBuffer(int)} is not filled and handling is completed.
	     */
        void recvCompleted();

        /**
	     * Notification from Chabu, that the receive stream of this channel is reseted. 
	     */
        void recvReset();

        /**
	     * Chabu provides a reference to the channel instance.
	     */
        void setChannel(ChabuChannel channel);
    }
}