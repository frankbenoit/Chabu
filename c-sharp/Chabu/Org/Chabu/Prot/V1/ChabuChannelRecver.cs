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
namespace Org.Chabu.Prot.V1
{


    public interface ChabuChannelRecver : ChabuChannelBase
    {


        /**
         * Set the limit value as absolute byte position, until were the application can receive data. This value is relative to the recv position.
         * An increase has the side effect that the remote host might be notified about the bigger amount of data that can be sent.
         */
        long RecvLimit { get; set; }

        /**
         * Add a positive value onto the current recv limit.
         * An increase has the side effect that the remote host might be notified about the bigger amount of data that can be sent.
         */
        long AddRecvLimit(int added);

        /**
         * Amount of bytes that the application already received.
         */
        long RecvPosition { get; }

        /**
         * Amount of bytes that the application is able to receive.
         */
        long RecvRemaining { get; }
    }
}