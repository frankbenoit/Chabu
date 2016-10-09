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

    /**
     * The Chabu channel.
     * 
     * @author Frank Benoit
     */
    public interface ChabuChannel
    {

        /**
         * Schedule a call to {@link ChabuChannelUser#xmitEvent(java.nio.ByteBuffer)}
         * when it is possible in future.
         * The priority of the channel is important and if the receiver 
         * has notified about available space for receive.
         */
        void xmitRegisterRequest();

        /**
         * Schedule a call to {@link ChabuChannelUser#evRecv(java.nio.ByteBuffer)}. 
         * This can happen within this method call.
         */
        void recvRegisterRequest();

        /**
         * Retrieve the associated user object.
         */
        ChabuChannelUser getUser();

        /**
         * Get the number index of this channel.
         */
        int getChannelId();

        /**
         * Get the priority number configured for this channel.
         */
        int getPriority();
    }
}