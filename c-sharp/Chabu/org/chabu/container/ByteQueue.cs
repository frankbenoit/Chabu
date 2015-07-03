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
namespace org.chabu.container
{

    /**
     * A ByteQueue is using internally a ring buffer to write and read byte 
     * sequences of arbitrary length.<br/>
     * This container is designed to be used from a single writer thread and 
     * a single reader thread.<br/>
     * Create instance of ByteQueue with the {@link ByteQueueBuilder}
     * 
     * @author Frank Benoit
     *
     */
    public interface ByteQueue
    {

        /**
         * Retrieve the capacity of the buffer in bytes.
         */
        int capacity();

        /**
         * Get the input port instance and use it for all write actions.
         */
        ByteQueueInputPort getInport();

        /**
         * Get the output port instance and use it for all read actions.
         */
        ByteQueueOutputPort getOutport();


    }
}