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
package org.chabu;

import java.nio.ByteBuffer;

import org.chabu.container.ByteQueueOutputPort;

/**
 * Communication from IChabuChannel to the application. The application shall give one instance
 * of IChabuChannelUser to each channel.
 */
public interface ChabuChannelUser {

	/**
	 * Called in the startup, so this object can notify the channel about recv/xmit requests.
	 * @param channel
	 */
	void setChannel( ChabuChannel channel );
	
	/**
	 * Event to receive data from the network.
	 */
	void recvEvent( ByteQueueOutputPort queue );
	
	/**
	 * Event to pass data to the network.
	 */
	boolean xmitEvent( ByteBuffer bufferToFill );
	
}
