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

import org.chabu.container.ByteQueue;
import org.chabu.container.ByteQueueInputPort;
import org.chabu.container.ByteQueueOutputPort;

/**
 * 
 * @author Frank Benoit
 *
 */
public final class ByteQueueImpl implements ByteQueue {

	protected static boolean useAsserts = true;
	String name;
	byte[] buf;

	void Assert( boolean cond ){
		if( cond ) return;
		throw new RuntimeException(String.format("ByteQueue (%s)", name ));
	}
	void AssertPrintf(boolean cond, String string, Object ... args ) {
		if( cond ) return;
		throw new RuntimeException(String.format("ByteQueue (%s): %s", name, String.format(string, args) ));
	}

	final ByteQueueInputPortImpl inport = new ByteQueueInputPortImpl(this);
	final ByteQueueOutputPortImpl outport = new ByteQueueOutputPortImpl(this);
	
	public ByteQueueImpl( String name, int capacity ){
		this.name = name;
		this.buf = new byte[ capacity+1 ];
	}
		
	@Override
	public int capacity(){
		return this.buf.length-1;
	}
	
	@Override
	public ByteQueueInputPort getInport() {
		return inport;
	}

	@Override
	public ByteQueueOutputPort getOutport() {
		return outport;
	}
	

	@Override
	public String toString() {
		return String.format("ByteQueue[ cap=%s, %s, %s ]", capacity(), inport, outport );
	}

	

}
