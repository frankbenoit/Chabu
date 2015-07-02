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
 * The input port of a ByteQueue is used for all write actions. To make the data visible for the output port, a call to commit is needed.
 * A callback can be set, to get notified about newly available data.
 * 
 * @author Frank Benoit
 */
public interface ByteQueueInputPort {

	/**
	 * Get the ByteQueue instance that is holding the data for this input port.
	 */
	ByteQueue getByteQueue();

	/**
	 * Get the amount of bytes that can be written into this input port.<br/>
	 * This calculates between last input port commit and last output port commit.
	 * @return number of bytes that can be written.
	 */
	int freeCommitted();
	
	/**
	 * Get the amount of bytes that can be written into this input port.<br/>
	 * This calculates between last input port write action and last output port commit.
	 * @return number of bytes that can be written.
	 */
	int free();

	/**
	 * Write the content of the array into the queue. The content is taken starting at offset and as many bytes as length.
	 * @param buf array of data
	 * @param offset in the buf
	 * @param len the count of bytes to be copied.
	 */
	void write(byte[] buf, int offset, int len );
	
	/**
	 * Write data from the ByteBuffer into the ByteQueue.
	 * Take as much data as the ByteQueue can take. There may be still bytes available afterwards.
	 * @param bb
	 * @return amount of bytes copied
	 */
	public int offer( ByteBuffer bb );

	/**
	 * Write data from the ByteBuffer into the ByteQueue.
	 * Throw a RuntimeException if the ByteQueue could not take all data.
	 * 
	 * @param bb
	 * @return amount of bytes copied
	 */
	public int write( ByteBuffer bb );

	/**
	 * Write data from the ByteBuffer into the ByteQueue. Transfer the amount of bytes given as length.
	 * Throw a RuntimeException if the ByteQueue could not take all data, or if the ByteBuffer does not have enough remaining.
	 * 
	 * @param bb
	 * @return amount of bytes copied
	 */
	public int write( ByteBuffer bb, int length );
	
	/**
	 * Write a byte value into the queue.
	 * @param value
	 */
	public void writeByte( byte value );

	/**
	 * Write an 2-byte integer value into the queue. BIG_ENDIAN is used.
	 * @param value
	 */
	public void writeShort( short value );
	
	/**
	 * Write an 4-byte integer value into the queue. BIG_ENDIAN is used.
	 * @param value
	 */
	public void writeInt( int value );

	/**
	 * Write bytes with given value into the queue.
	 * @param value
	 * @param count
	 */
	void writePadding(byte value, int count);

	/**
	 * Commit all write actions since the last commit. This makes the written data visible for the out port.
	 * If a callback is set, call it to notify about data available.
	 */
	void commit();
	
	/**
	 * Revert all write actions since the last commit.
	 */
	void rollback();

	/**
	 * Set a call back to be notified about committed data.
	 * @param callbackConsumed
	 */
	void setCallbackConsumed(ByteQueueSpaceAvailableListener callbackConsumed);


	
}
