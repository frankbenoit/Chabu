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
package org.chabu.container.internal;

import java.nio.ByteBuffer;

import org.chabu.container.ByteQueue;
import org.chabu.container.ByteQueueDataAvailableListener;
import org.chabu.container.ByteQueueInport;
import org.chabu.container.ByteQueueOutport;

/**
 * 
 * @author Frank Benoit
 *
 */
final class ByteQueueOutportImpl implements ByteQueueOutport {
	/**
	 * 
	 */
	private final ByteQueueImpl queue;

	/**
	 * @param byteQueueImpl
	 */
	ByteQueueOutportImpl(ByteQueueImpl byteQueueImpl) {
		queue = byteQueueImpl;
	}

	private ByteBuffer readBuffer  = ByteBuffer.allocate(8);
	private ByteQueueDataAvailableListener  callbackSupplied;
	volatile int    readIdx;
	private int             readMarkIdx;

	@Override
	public ByteQueue getByteQueue() {
		return queue;
	}
	
	@Override
	public int available(){
		int wr = queue.inport.writeIdx;
		int rd = this.readIdx;
		if( rd > wr ){
			wr += queue.buf.length;
		}
		int res = wr - rd;
		if( ByteQueueImpl.useAsserts ) queue.Assert( res >= 0 );
		return res;
	}

	@Override
	public int availableUncommitted(){
		int wr = queue.inport.writeIdx;
		int rd = this.readMarkIdx;
		if( rd > wr ){
			wr += queue.buf.length;
		}
		int res = wr - rd;
		if( ByteQueueImpl.useAsserts ) queue.Assert( res >= 0 );
		return res;
	}
	
	@Override
	public int readInt(){
		readBuffer.clear();
		readBuffer.limit(4);
		read(readBuffer);
		readBuffer.flip();
		return readBuffer.getInt();
	}

	@Override
	public void commit() {
		readIdx = readMarkIdx;
		ByteQueueDataAvailableListener cb = this.callbackSupplied;
		if( cb != null ){
			cb.dataAvailable( this );
		}
		
	}
	
	@Override
	public void rollback() {
		readMarkIdx = readIdx;
	}
	

	@Override
	public int poll( ByteBuffer bb ){
		int cpySz = Math.min( bb.remaining(), availableUncommitted() );
		read(bb.array(), bb.arrayOffset()+bb.position(), cpySz );
		bb.position( bb.position() + cpySz );
		return cpySz; 
	}
	
	@Override
	public int read( ByteBuffer bb ){
		int cpySz = bb.remaining();
		if( cpySz > availableUncommitted() ) throw new RuntimeException(String.format("ByteQueue (%s) could not read the requested amount of data", queue.name ));
		read(bb.array(), bb.arrayOffset()+bb.position(), cpySz );
		bb.position( bb.position() + cpySz );
		return cpySz; 
	}
	

	@Override
	public void read(byte[] buf, int offset, int len) {
		int rd = this.readMarkIdx;
		int wr = queue.inport.writeMarkIdx;
	
		if( ByteQueueImpl.useAsserts ) queue.AssertPrintf(( availableUncommitted() >= len ) && ( len >= 0 ), "avail %s len %s", availableUncommitted(), len );
	
		int remaining = len;
		int cpyOffset = 0;
		int end = rd + remaining;
		if( end > queue.buf.length ){
			int cpy_len = queue.buf.length - rd;
	
			if( ByteQueueImpl.useAsserts ) queue.Assert( wr < rd );
			if( ByteQueueImpl.useAsserts ) queue.Assert( cpy_len < len );
			if( ByteQueueImpl.useAsserts ) queue.Assert( rd+cpy_len <= queue.buf.length );
			System.arraycopy(queue.buf, rd, buf, offset, cpy_len);
	
			remaining -= cpy_len;
			cpyOffset = cpy_len;
			rd = 0;
		}
		// the remaining piece of data end before the buffer ends.
		if( ByteQueueImpl.useAsserts ) queue.Assert( rd+remaining <= queue.buf.length );
		// the remaining piece of data ends before the wr -or- it starts behind the wr.
		if( ByteQueueImpl.useAsserts ) queue.Assert(( rd+remaining <= wr ) || (wr < rd));
		// the target data does not exceed the len of the passed buf.
		if( ByteQueueImpl.useAsserts ) queue.Assert( cpyOffset+remaining == len );
	
		System.arraycopy(queue.buf, rd, buf, offset+cpyOffset, remaining);

		readMarkIdx = rd + remaining;
	}

	private int readChunkSize(){
		int wr = queue.inport.writeIdx;
		int rd = this.readMarkIdx;
		if( wr >= rd ){
			return wr - rd;
		}
		else {
			return queue.buf.length - rd;
		}
	}

	@Override
	public void setCallbackSupplied(ByteQueueDataAvailableListener callbackSupplied ){
		if( callbackSupplied != null ){
			// avoid reconfigure by accident.
			// if this is intended, first set the call back to null
			throw new RuntimeException(String.format("ByteQueue (%s) has already supplied callback", queue.name ));
		}
		this.callbackSupplied = callbackSupplied;
	}
	
	@Override
	public void move( ByteQueueInport trgQueue, int size ){
		
		if( ByteQueueImpl.useAsserts ) queue.Assert( trgQueue.free() >= size);
		if( ByteQueueImpl.useAsserts ) queue.Assert( available() >= size);
	
		while( size > 0 ){
			int ptr = readMarkIdx;
			int cpy_len = readChunkSize();
			if( cpy_len > size ) {
				cpy_len = size;
			}
			trgQueue.write( queue.buf, ptr, cpy_len);
			size -= cpy_len;
		}
	}

	@Override
	public void clear(){
		readMarkIdx = queue.inport.writeIdx;
		commit();
	}

	@Override
	public String toString() {
		return String.format("ByteQueueOutport[ avail=%s availUncom=%s ]", available(), availableUncommitted() );
	}
}