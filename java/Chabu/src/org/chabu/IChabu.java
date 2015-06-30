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

import java.io.PrintWriter;
import java.nio.ByteBuffer;

/**
 * The main object communicating with the network and distributing/collecting the data to/from the
 * channels.
 *
 * @author Frank Benoit
 */
public interface IChabu {

	/**
	 * Called by the network, offering chabu the next portion of received data.
	 * Chabu will update the position of the buffer according to the bytes consumed.
	 *
	 * @param bufferToConsume buffer containing the data.
	 */
	void evRecv(ByteBuffer bufferToConsume);

	/**
	 * Called by the network, offering chabu to give the next portion of transmit data.
	 * Chabu will put as many bytes into the buffer as it can at the moment. The buffers
	 * position will be updated by chabu.
	 * <p/>
	 * The size of the buffer may influence the priority of the channels. If a very big buffer is
	 * offered that cannot be transmitted by the network immediately, chabu it putting in all data
	 * that is available at the moment. If afterwards a higher prior channel wants to transmit,
	 * the network has already received data and is busy.<br/>
	 * Hence it might be good to give only as much buffer space, that is expected to be transferred
	 * in the near time.
	 *
	 * @param bufferToFill the buffer to be filled by chabu. Must be not null.
	 * @return
	 */
	void evXmit(ByteBuffer bufferToFill);

	/**
	 * Set an optional printer for trace information.
	 * <p/>
	 * Chabu can write out the interactions to chabu and the channels in text format. This
	 * information can be used to replay the sequence. Might be helpful to reproduce failures.
	 *
	 * @param writer that takes the information.
	 */
	void setTracePrinter( PrintWriter writer );

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
	IChabuChannel getChannel( int channelId );

	/**
	 * Adds a callback to be called when chabu wants to send over the network. It shall lead to a
	 * call of #evXmit.
	 * <p/>
	 * At the moment only a single callback can be added.
	 *
	 * @param r the callback
	 */
	void addXmitRequestListener( Runnable r );
	
	/**
	 * Returns true if chabu want to receive another call to {@link #evXmit(ByteBuffer)}.
	 */
	boolean isXmitRequestPending();
}
