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
package org.chabu.prot.v1;

import java.io.IOException;
import java.nio.channels.ByteChannel;

/**
 * The main object communicating with the network and distributing/collecting the data to/from the
 * channels.
 *
 * @author Frank Benoit
 */
public interface Chabu {

	void handleChannel( ByteChannel channel ) throws IOException;
	
	/**
	 * Get the count of configured channels.
	 */
	int getChannelCount();

	/**
	 * Get a specific channel.
	 * @param channelId the index of the channel to be returned.
	 *                  Must within 0 &le; channelId &lt; #getChannelCount().
	 * @return the channel.
	 */
	ChabuChannel getChannel( int channelId );

	/**
	 * Adds a callback to be called when chabu wants to send over the network. It shall lead to a
	 * call of {@link #handleChannel(ByteChannel, boolean)}.
	 * <p/>
	 * At the moment only a single callback can be added.
	 *
	 * @param r the callback
	 */
	void addXmitRequestListener( Runnable r );
	
}
