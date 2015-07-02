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
import java.util.Arrays;

import org.chabu.container.ByteQueue;
import org.chabu.container.ByteQueueInputPort;
import org.chabu.container.ByteQueueSpaceAvailableListener;

/**
 * 
 * @author Frank Benoit
 *
 */
final class ByteQueueInputPortImpl implements ByteQueueInputPort {
	
	/**
	 * 
	 */
	private final ByteQueueImpl queue;

	/**
	 * @param byteQueueImpl
	 */
	ByteQueueInputPortImpl(ByteQueueImpl byteQueueImpl) {
		queue = byteQueueImpl;
	}
	
	// hold the space available listener, as this is what users of the input are interested in.
	ByteQueueSpaceAvailableListener callbackConsumed;
	
	volatile int    writeIdx;
	int             writeMarkIdx;

	@Override
	public ByteQueue getByteQueue() {
		return queue;
	}
	
	@Override
	public int freeCommitted(){
		int wr = writeIdx;
		int rd = queue.outport.readIdx;
		if( rd > wr ){
			wr += queue.buf.length;
		}
		int res = queue.buf.length -1 - ( wr - rd );
		if( ByteQueueImpl.useAsserts ) queue.Assert( res >= 0 );
		return res;
	}
	
	@Override
	public int free(){
		int wr = writeMarkIdx;
		int rd = queue.outport.readIdx;
		if( rd > wr ){
			wr += queue.buf.length;
		}
		int res = queue.buf.length -1 - ( wr - rd );
		if( ByteQueueImpl.useAsserts ) queue.Assert( res >= 0 );
		return res;
	}
	
	@Override
	public int offer( ByteBuffer bb ){
		int cpySz = Math.min( bb.remaining(), freeCommitted() );
		write( bb.array(), bb.arrayOffset()+bb.position(), cpySz );
		bb.position( bb.position() + cpySz );
		return cpySz; 
	}
	
	@Override
	public void write(byte[] buf, int srcOffset, int len ){
		queue.Assert( len >= 0 );
		queue.Assert( freeCommitted() >= len );
	
		int wr = this.writeMarkIdx;
		if( ByteQueueImpl.useAsserts ) queue.AssertPrintf( wr < queue.buf.length, "%d<%d in %s", wr, queue.buf.length, queue.name );
	
		int remaining = len;
		int offset = 0;
		int end = wr + remaining;
		if( end > queue.buf.length ){
			int cpy_len = queue.buf.length - wr;
	
			if( ByteQueueImpl.useAsserts ) queue.Assert( cpy_len >= 0 );
			if( ByteQueueImpl.useAsserts ) queue.Assert( cpy_len <= len );
			if( ByteQueueImpl.useAsserts ) queue.Assert( wr >= 0 );
	
			System.arraycopy( buf, srcOffset, queue.buf, wr, cpy_len);
	
			remaining -= cpy_len;
			if( ByteQueueImpl.useAsserts ) queue.Assert( remaining >= 0 );
			offset = cpy_len;
			wr = 0;
		}
		if( ByteQueueImpl.useAsserts ) queue.Assert( offset+remaining == len );
		if( ByteQueueImpl.useAsserts ) queue.Assert( wr+remaining <= queue.buf.length );
		if( ByteQueueImpl.useAsserts ) queue.Assert( wr >= 0 );
	
		System.arraycopy( buf, offset+srcOffset, queue.buf, wr, remaining );
	
		wr += remaining;
		if( wr == queue.buf.length ){
			wr = 0;
		}
	
		int rd = 0;
		if( ByteQueueImpl.useAsserts ) rd = queue.outport.readIdx; // get rd_idx before writing wr_idx, to avoid race condition for check
		
		this.writeMarkIdx = wr;
		
		if( ByteQueueImpl.useAsserts ) {
			if( len != 0 ){
				queue.AssertPrintf( rd != wr, "after adding data, they cannot be equal: %d %d", rd, wr );
			}
		}
	
	
	}

	@Override
	public int write( ByteBuffer bb ){
		return write( bb, bb.remaining() );
	}
	
	@Override
	public int write( ByteBuffer bb, int length ){
		int cpySz = length;
		if( cpySz > freeCommitted() ) throw new RuntimeException(String.format("ByteQueue (%s) could not take all data", queue.name ));
		write(bb.array(), bb.arrayOffset()+bb.position(), cpySz );
		bb.position( bb.position() + cpySz );
		return cpySz; 		
	}
	
	@Override
	public void writeByte( byte value ){

		queue.Assert( 1 <= free() );

		int idx = writeMarkIdx;
		
		queue.buf[ idx++ ] = value;
		if( idx >= queue.buf.length ) idx = 0;
		
		writeMarkIdx = idx;
	}

	@Override
	public void writeShort( short value ){

		queue.Assert( 2 <= free() );

		int idx = writeMarkIdx;
		
		queue.buf[ idx++ ] = (byte)(value >> 8);
		if( idx >= queue.buf.length ) idx = 0;
		
		queue.buf[ idx++ ] = (byte)value;
		if( idx >= queue.buf.length ) idx = 0;
		
		writeMarkIdx = idx;
	}
	
	@Override
	public void writeInt( int value ){

		queue.Assert( 4 <= free() );

		int idx = writeMarkIdx;
		
		queue.buf[ idx++ ] = (byte)(value >> 24);
		if( idx >= queue.buf.length ) idx = 0;
		
		queue.buf[ idx++ ] = (byte)(value >> 16);
		if( idx >= queue.buf.length ) idx = 0;
		
		queue.buf[ idx++ ] = (byte)(value >> 8);
		if( idx >= queue.buf.length ) idx = 0;
		
		queue.buf[ idx++ ] = (byte)value;
		if( idx >= queue.buf.length ) idx = 0;
		
		writeMarkIdx = idx;
	}
	
	@Override
	public void writePadding( byte value, int count ){

		queue.Assert( count <= free() );
		
		int idx = writeMarkIdx;
		
		int endIdx = idx + count;
		if( endIdx > queue.buf.length ){
			int fillSz = queue.buf.length - idx;
			Arrays.fill( queue.buf, idx, queue.buf.length, value );
			idx = 0;
			endIdx -= fillSz; 
		}
		Arrays.fill( queue.buf, idx, endIdx, value );
		
		writeMarkIdx = endIdx;
	}
	
	@Override
	public void setCallbackConsumed(ByteQueueSpaceAvailableListener callbackConsumed ){
		if( callbackConsumed != null ){
			// avoid reconfigure by accident.
			// if this is intended, first set the call back to null
			throw new RuntimeException(String.format("ByteQueue (%s) has already consumed callback", queue.name ));
		}
		this.callbackConsumed = callbackConsumed;
	}

	public void commit() {
		if( this.writeIdx == this.writeMarkIdx ){
			// no effect, do nothing
			return;
		}
		this.writeIdx = this.writeMarkIdx;
		ByteQueueSpaceAvailableListener cb = this.callbackConsumed;
		if( cb != null ){
			cb.spaceAvailable( this );
		}
	}
	
	@Override
	public void rollback() {
		this.writeMarkIdx = this.writeIdx;
	}

	@Override
	public String toString() {
		return String.format("ByteQueueInport[ free=%s freeUncom=%s ]", freeCommitted(), free() );
	}

	@Override
	public void ensureCommitted() {
		if( writeMarkIdx != writeIdx ){
			throw new RuntimeException(String.format("writeMarkIdx:%d != writeIdx:%d", writeMarkIdx, writeIdx ));
		}
	}

}