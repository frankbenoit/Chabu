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
package org.chabu.internal;

public enum PacketType {
	SETUP ( 0xF0, 10 ),
	ACCEPT( 0xE1,  8 ),
	ABORT ( 0xD2,  12 ),
	ARM   ( 0xC3,  16 ),
	SEQ   ( 0xB4,  20 ),
	;
	
	public final int id;
	public final int headerSize;

	private PacketType( int id, int minPacketSize ){
		this.id = id;
		this.headerSize = minPacketSize;
	}

	public static PacketType findPacketType( int id ){
		PacketType[] values = values();
		for( int i = 0; i < values.length; i++ ){
			if( values[i].id == id ) return values[i];
		}
		return null;
	}
}
