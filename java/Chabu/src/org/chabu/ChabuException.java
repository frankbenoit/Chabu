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
package org.chabu;

public class ChabuException extends RuntimeException {

	private final int code;
	
	public ChabuException( String message ){
		super( message );
		System.out.println("ChabuException: "+message);
		this.code = ChabuErrorCode.UNKNOWN.getCode();
	}

	public ChabuException( ChabuErrorCode error, String message ){
		super( message );
		this.code = error.getCode();
	}
	
	public ChabuException( int code, String message ){
		super( message );
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
}
