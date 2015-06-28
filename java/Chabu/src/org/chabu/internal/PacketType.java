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
package org.chabu.internal;

/**
 *
 * @author Frank Benoit
 */
public enum PacketType {
	SETUP ( 0xF0 ),
	ACCEPT( 0xE1 ),
	ABORT ( 0xD2 ),
	ARM   ( 0xC3 ),
	SEQ   ( 0xB4 ),
	;
	
	public final int id;

	private PacketType( int id ){
		this.id = id;
	}

	public static PacketType findPacketType( int id ){
		PacketType[] values = values();
		for( int i = 0; i < values.length; i++ ){
			if( values[i].id == id ) return values[i];
		}
		return null;
	}
}
