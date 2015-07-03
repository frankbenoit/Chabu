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
internal sealed class ByteQueueInputPortImpl : ByteQueueInputPort {
	
	/**
	 * 
	 */
	private readonly ByteQueueImpl queue;

	/**
	 * @param byteQueueImpl
	 */
	public ByteQueueInputPortImpl(ByteQueueImpl byteQueueImpl) {
		queue = byteQueueImpl;
	}
	
	// hold the space available listener, as this is what users of the input are interested in.
	ByteQueueSpaceAvailableListener callbackConsumed;
	
	public volatile int    writeIdx;
    public int writeMarkIdx;

	//override
	public ByteQueue getByteQueue() {
		return queue;
	}
	
	//override
	public int freeCommitted(){
		int wr = writeIdx;
		int rd = queue.outport.readIdx;
		if( rd > wr ){
			wr += queue.buf.Length;
		}
		int res = queue.buf.Length -1 - ( wr - rd );
		if( ByteQueueImpl.useAsserts ) queue.Assert( res >= 0 );
		return res;
	}
	
	//override
	public int free(){
		int wr = writeMarkIdx;
		int rd = queue.outport.readIdx;
		if( rd > wr ){
			wr += queue.buf.Length;
		}
		int res = queue.buf.Length -1 - ( wr - rd );
		if( ByteQueueImpl.useAsserts ) queue.Assert( res >= 0 );
		return res;
	}
	
	//override
	public int offer( ByteBuffer bb ){
		int cpySz = Math.Min( bb.remaining(), freeCommitted() );
		write( bb.array(), bb.arrayOffset()+bb.position(), cpySz );
		bb.position( bb.position() + cpySz );
		return cpySz; 
	}
	
	//override
	public void write(byte[] buf, int srcOffset, int len ){
		queue.Assert( len >= 0 );
		queue.Assert( freeCommitted() >= len );
	
		int wr = this.writeMarkIdx;
		if( ByteQueueImpl.useAsserts ) queue.AssertPrintf( wr < queue.buf.Length, "%d<%d in %s", wr, queue.buf.Length, queue.name );
	
		int remaining = len;
		int offset = 0;
		int end = wr + remaining;
		if( end > queue.buf.Length ){
			int cpy_len = queue.buf.Length - wr;
	
			if( ByteQueueImpl.useAsserts ) queue.Assert( cpy_len >= 0 );
			if( ByteQueueImpl.useAsserts ) queue.Assert( cpy_len <= len );
			if( ByteQueueImpl.useAsserts ) queue.Assert( wr >= 0 );

            Array.Copy(buf, srcOffset, queue.buf, wr, cpy_len);
	
			remaining -= cpy_len;
			if( ByteQueueImpl.useAsserts ) queue.Assert( remaining >= 0 );
			offset = cpy_len;
			wr = 0;
		}
		if( ByteQueueImpl.useAsserts ) queue.Assert( offset+remaining == len );
		if( ByteQueueImpl.useAsserts ) queue.Assert( wr+remaining <= queue.buf.Length );
		if( ByteQueueImpl.useAsserts ) queue.Assert( wr >= 0 );
	
		Array.Copy( buf, offset+srcOffset, queue.buf, wr, remaining );
	
		wr += remaining;
		if( wr == queue.buf.Length ){
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

	//override
	public int write( ByteBuffer bb ){
		return write( bb, bb.remaining() );
	}
	
	//override
	public int write( ByteBuffer bb, int length ){
		int cpySz = length;
		if( cpySz > freeCommitted() ) throw new SystemException(String.Format("ByteQueue ({0}) could not take all data", queue.name ));
		write(bb.array(), bb.arrayOffset()+bb.position(), cpySz );
		bb.position( bb.position() + cpySz );
		return cpySz; 		
	}
	
	//override
	public void writeByte( byte value ){

		queue.Assert( 1 <= free() );

		int idx = writeMarkIdx;
		
		queue.buf[ idx++ ] = value;
		if( idx >= queue.buf.Length ) idx = 0;
		
		writeMarkIdx = idx;
	}

	//override
	public void writeShort( short value ){

		queue.Assert( 2 <= free() );

		int idx = writeMarkIdx;
		
		queue.buf[ idx++ ] = (byte)(value >> 8);
		if( idx >= queue.buf.Length ) idx = 0;
		
		queue.buf[ idx++ ] = (byte)value;
		if( idx >= queue.buf.Length ) idx = 0;
		
		writeMarkIdx = idx;
	}
	
	//override
	public void writeInt( int value ){

		queue.Assert( 4 <= free() );

		int idx = writeMarkIdx;
		
		queue.buf[ idx++ ] = (byte)(value >> 24);
		if( idx >= queue.buf.Length ) idx = 0;
		
		queue.buf[ idx++ ] = (byte)(value >> 16);
		if( idx >= queue.buf.Length ) idx = 0;
		
		queue.buf[ idx++ ] = (byte)(value >> 8);
		if( idx >= queue.buf.Length ) idx = 0;
		
		queue.buf[ idx++ ] = (byte)value;
		if( idx >= queue.buf.Length ) idx = 0;
		
		writeMarkIdx = idx;
	}
	
	//override
	public void writePadding( byte value, int count ){

		queue.Assert( count <= free() );
		
		int idx = writeMarkIdx;
		
		int endIdx = idx + count;
		if( endIdx > queue.buf.Length ){
			int fillSz = queue.buf.Length - idx;
			queue.buf.Fill( idx, queue.buf.Length, value );
			idx = 0;
			endIdx -= fillSz; 
		}
		queue.buf.Fill( idx, endIdx, value );
		
		writeMarkIdx = endIdx;
	}
	
	//override
	public void setCallbackConsumed(ByteQueueSpaceAvailableListener callbackConsumed ){
		if( callbackConsumed != null ){
			// avoid reconfigure by accident.
			// if this is intended, first set the call back to null
			throw new SystemException(String.Format("ByteQueue ({0}) has already consumed callback", queue.name ));
		}
		this.callbackConsumed = callbackConsumed;
	}

    //override
    public void commit()
    {
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
	
	//override
	public void rollback() {
		this.writeMarkIdx = this.writeIdx;
	}

	//override
	public String toString() {
		return String.Format("ByteQueueInport[ free={0} freeUncom={1} ]", freeCommitted(), free() );
	}

	//override
	public void ensureCommitted() {
		if( writeMarkIdx != writeIdx ){
			throw new SystemException(String.Format("writeMarkIdx:{0} != writeIdx:{1}", writeMarkIdx, writeIdx ));
		}
	}

}
}