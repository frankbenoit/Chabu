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
using System;
namespace org.chabu{

/**
 * Return value from the {@link org.chabu.ChabuConnectingValidator}
 */
public sealed class ChabuConnectionAcceptInfo {
	
	/**
	 * @see ChabuErrorCode
	 */
	public readonly int code;
	
	/**
	 * maximum length 48 bytes in UTF8.
	 */
	public readonly String message;
	
	public ChabuConnectionAcceptInfo( int code, String message ){
		this.code = code;
		this.message = message;
	}
}
}