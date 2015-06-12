package org.chabu;

public class ChabuConnectionAcceptInfo {
	
	/**
	 * @see ChabuErrorCode
	 */
	public int code;
	
	/**
	 * maximum length 200 bytes in UTF8.
	 */
	public String message;
	
	public ChabuConnectionAcceptInfo(){
	}
	public ChabuConnectionAcceptInfo( int code, String message ){
		this.code = code;
		this.message = message;
	}
}
