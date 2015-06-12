/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit.
 * 
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 * 
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
package org.chabu;

import java.nio.ByteBuffer;

public interface IChabuChannelUser {

	/**
	 * Called in the startup, so this object can notify the channel about recv/xmit requests.
	 * @param channel
	 */
	public void setChannel( IChabuChannel channel );
	
	/**
	 * Event to receive data from the network.
	 * If the buffer is not fully consumed in this call, 
	 * {@link IChabuChannel#evUserRecvRequest()} can be used to get called another time.
	 * @param bufferToConsume
	 */
	public void evRecv( ByteBuffer bufferToConsume );
	
	/**
	 * Event to pass data to the network.
	 * The passed buffer can be fill completely. If there is more data to be transferred,
	 * {@link IChabuChannel#evUserXmitRequest()} shall be used to schedule a call whenever
	 * it is possible in future.
	 * @param bufferToFill
	 */
	public boolean evXmit( ByteBuffer bufferToFill );
	
}
