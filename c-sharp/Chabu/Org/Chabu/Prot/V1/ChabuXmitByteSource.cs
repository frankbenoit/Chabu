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

namespace Org.Chabu.Prot.V1
{
    using ByteBuffer = Org.Chabu.Prot.Util.ByteBuffer;

    public interface ChabuXmitByteSource
    {

        /// <summary>Ask for a ByteBuffer with at least the given amount of bytes remaining.
        /// </summary>
        /// Depending on how the interface is used, the buffer might be used for receiving or for transmitting.
        /// The caller must call {@link #xmitCompleted()} when done with the buffer. 
        /// <param name="size">The minimum amount of byes that must be available in the returned buffer</param>
        ByteBuffer GetXmitBuffer(int size);

        /// <summary>The caller is done with the ByteBuffer received earlier from <see cref="GetXmitBuffer(int)"/> .
        /// </summary>
        void XmitCompleted();

        /// <summary>Reset all data for this channel
        /// </summary>
        void XmitReset();

        /// <summary>Set the channel used for this data stream
        /// </summary>
        void SetChannel(ChabuChannel channel);

    }
}