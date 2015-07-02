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
package org.chabu.container;

import java.nio.ByteBuffer;

/**
 * 
 * @author Frank Benoit
 *
 */
public interface ByteQueueOutputPort {

	/**
	 * Get the ByteQueue instance that is holding the data for this output port.
	 */
	ByteQueue getByteQueue();
	
	/**
	 * Get the amount of bytes that can be read from this output port.<br/>
	 * This calculates between last output port commit and last input port commit.
	 * @return number of bytes that can be read.
	 */
	int available();
	
	/**
	 * Get the amount of bytes that can be read from this output port.<br/>
	 * This calculates between last output port read action and last input port commit.
	 * @return number of bytes that can be read.
	 */
	int availableUncommitted();
	
	void read(byte[] buf, int offset, int len );

	int readInt();

	/**
	 * Make the readings onto this output port visible to the input port.
	 * 
	 * If a callback is set and new space get available with this commit, the callback 
	 * is called, to notify about the newly available space.
	 */
	void commit();

	/**
	 * Revert the effect of all read actions since last commit.
	 */
	void rollback();

	/**
	 * Consume all bytes that are available in this queue and commit it immediatly.
	 */
	void clear();

	/**
	 * Read as much data into the ByteBuffer, as is available and the ByteBuffer can take.
	 * @param bb
	 * @return amount of bytes copied
	 */
	int poll( ByteBuffer bb );	
	
	/**
	 * Read data into the ByteBuffer. Throw an RuntimeException if there was not enough 
	 * data available to full all remaining space in the ByteBuffer.
	 * 
	 * @param bb
	 * @return amount of bytes copied
	 * @see #poll(ByteBuffer) 
	 */
	int read( ByteBuffer bb );

	void setCallbackSupplied(ByteQueueDataAvailableListener callbackSupplied);

	void move(ByteQueueInputPort trgQueue, int size);

	/**
	 * Read a single byte.
	 */
	byte readByte();

	/**
	 * Skip bytes.
	 */
	void skip(int length);

}
