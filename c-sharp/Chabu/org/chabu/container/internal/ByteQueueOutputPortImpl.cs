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
namespace org.chabu.container.intern{

    using System;
    using ByteBuffer = System.IO.MemoryStream;
    using org.chabu.container;

/**
 * 
 * @author Frank Benoit
 *
 */
sealed class ByteQueueOutputPortImpl : ByteQueueOutputPort {
	/**
	 * 
	 */
	private readonly ByteQueueImpl queue;

	/**
	 * @param byteQueueImpl
	 */
	public ByteQueueOutputPortImpl(ByteQueueImpl byteQueueImpl) {
		queue = byteQueueImpl;
	}

	private ByteQueueDataAvailableListener  callbackSupplied;
	public volatile int    readIdx;
	public int             readMarkIdx;

	//override
	public ByteQueue getByteQueue() {
		return queue;
	}
	
	//override
	public int availableCommitted(){
		int wr = queue.inport.writeIdx;
		int rd = this.readIdx;
		if( rd > wr ){
			wr += queue.buf.Length;
		}
		int res = wr - rd;
		if( ByteQueueImpl.useAsserts ) queue.Assert( res >= 0 );
		return res;
	}

	//override
	public int available(){
		int wr = queue.inport.writeIdx;
		int rd = this.readMarkIdx;
		if( rd > wr ){
			wr += queue.buf.Length;
		}
		int res = wr - rd;
		if( ByteQueueImpl.useAsserts ) queue.Assert( res >= 0 );
		return res;
	}
	
	//override
	public void skip(int length) {
		queue.Assert(length >= 0 && length <= available() );
		int nextIdx = readMarkIdx + length;
		readMarkIdx = ( nextIdx >= queue.buf.Length ) ? 0 : nextIdx;
	}

	
	//override
	public byte readByte() {
		queue.Assert( available() >= 1 );
		byte res = queue.buf[ readMarkIdx ];
		int nextIdx = readMarkIdx + 1;
		readMarkIdx = ( nextIdx >= queue.buf.Length ) ? 0 : nextIdx;
		return res;
	}

	//override
	public int readShort(){
		queue.Assert( available() >= 2 );
		
		int idx = readMarkIdx;
		short res;
		
		res = queue.buf[ idx ];
		if( ++idx >= queue.buf.Length ) idx = 0;
		
		res <<= 8; 
		
		res |= (short)(queue.buf[ idx ] & 0xFF);
		if( ++idx >= queue.buf.Length ) idx = 0;
		
		readMarkIdx = idx;

		return res;
	}

	//override
	public int readInt(){
		
		queue.Assert( available() >= 4 );
		
		int idx = readMarkIdx;
		int res;
		
		res = queue.buf[ idx ];
		if( ++idx >= queue.buf.Length ) idx = 0;
		
		res <<= 8; 
		
		res |= queue.buf[ idx ] & 0xFF;
		if( ++idx >= queue.buf.Length ) idx = 0;
		
		res <<= 8; 
		
		res |= queue.buf[ idx ] & 0xFF;
		if( ++idx >= queue.buf.Length ) idx = 0;
		
		res <<= 8; 
		
		res |= queue.buf[ idx ] & 0xFF;
		if( ++idx >= queue.buf.Length ) idx = 0;
		
		readMarkIdx = idx;

		return res;
	}
	
	//override
	public void commit() {
		readIdx = readMarkIdx;
		ByteQueueDataAvailableListener cb = this.callbackSupplied;
		if( cb != null ){
			cb.dataAvailable( this );
		}
		
	}
	
	//override
	public void rollback() {
		readMarkIdx = readIdx;
	}
	

	//override
	public int poll( ByteBuffer bb ){
		int cpySz = Math.Min( bb.remaining(), available() );
		read(bb.array(), bb.arrayOffset()+bb.position(), cpySz );
		bb.position( bb.position() + cpySz );
		return cpySz; 
	}
	
	//override
	public int read( ByteBuffer bb ){
		int cpySz = bb.remaining();
		if( cpySz > available() ) throw new SystemException(String.Format("ByteQueue ({0}) could not read the requested amount of data", queue.name ));
		read(bb.array(), bb.arrayOffset()+bb.position(), cpySz );
		bb.position( bb.position() + cpySz );
		return cpySz; 
	}
	

	//override
	public void read(byte[] buf, int offset, int len) {
		int rd = this.readMarkIdx;
		int wr = queue.inport.writeMarkIdx;
	
		if( ByteQueueImpl.useAsserts ) queue.AssertPrintf(( available() >= len ) && ( len >= 0 ), "avail %s len %s", available(), len );
	
		int remaining = len;
		int cpyOffset = 0;
		int end = rd + remaining;
		if( end > queue.buf.Length ){
			int cpy_len = queue.buf.Length - rd;
	
			if( ByteQueueImpl.useAsserts ) queue.Assert( wr < rd );
			if( ByteQueueImpl.useAsserts ) queue.Assert( cpy_len < len );
			if( ByteQueueImpl.useAsserts ) queue.Assert( rd+cpy_len <= queue.buf.Length );
			Array.Copy(queue.buf, rd, buf, offset, cpy_len);
	
			remaining -= cpy_len;
			cpyOffset = cpy_len;
			rd = 0;
		}
		// the remaining piece of data end before the buffer ends.
		if( ByteQueueImpl.useAsserts ) queue.Assert( rd+remaining <= queue.buf.Length );
		// the remaining piece of data ends before the wr -or- it starts behind the wr.
		if( ByteQueueImpl.useAsserts ) queue.Assert(( rd+remaining <= wr ) || (wr < rd));
		// the target data does not exceed the len of the passed buf.
		if( ByteQueueImpl.useAsserts ) queue.Assert( cpyOffset+remaining == len );

        Array.Copy(queue.buf, rd, buf, offset + cpyOffset, remaining);

		readMarkIdx = rd + remaining;
	}

	private int readChunkSize(){
		int wr = queue.inport.writeIdx;
		int rd = this.readMarkIdx;
		if( wr >= rd ){
			return wr - rd;
		}
		else {
			return queue.buf.Length - rd;
		}
	}

	//override
	public void setCallbackSupplied(ByteQueueDataAvailableListener callbackSupplied ){
		if( callbackSupplied != null ){
			// avoid reconfigure by accident.
			// if this is intended, first set the call back to null
			throw new SystemException(String.Format("ByteQueue ({0}) has already supplied callback", queue.name ));
		}
		this.callbackSupplied = callbackSupplied;
	}
	
	//override
	public void move( ByteQueueInputPort trgQueue, int size ){
		
		if( ByteQueueImpl.useAsserts ) queue.Assert( trgQueue.freeCommitted() >= size);
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

	//override
	public void clear(){
		readMarkIdx = queue.inport.writeIdx;
		commit();
	}

	//override
	public String toString() {
		return String.Format("ByteQueueOutport[ avail={0} availUncom={1} ]", availableCommitted(), available() );
	}

	//override
	public void ensureCommitted() {
		if( readMarkIdx != readIdx ){
			throw new SystemException();
		}
	}

}
}