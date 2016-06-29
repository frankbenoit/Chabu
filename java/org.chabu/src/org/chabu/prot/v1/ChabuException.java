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
package org.chabu.prot.v1;

public class ChabuException extends RuntimeException {

	private final int code;
	private final int remoteCode;
	
	public ChabuException( String message, Object ... args ){
		super( String.format( message, args) );
		System.out.println("ChabuException: "+getMessage());
		this.code = ChabuErrorCode.UNKNOWN.getCode();
		this.remoteCode = 0;
	}

	public ChabuException( ChabuErrorCode error, String message, Object ... args ){
		super( String.format( message, args) );
		this.code = error.getCode();
		this.remoteCode = 0;
	}
	
	public ChabuException( ChabuErrorCode error, int remoteCode, String message, Object ... args ){
		super( String.format( message, args) );
		this.code = error.getCode();
		this.remoteCode = remoteCode;
	}
	
	public ChabuException( int code, String message, Object ... args ){
		super( String.format( message, args) );
		this.code = code;
		this.remoteCode = 0;
	}
	
	public int getCode() {
		return code;
	}
	public int getRemoteCode() {
		return remoteCode;
	}
}
