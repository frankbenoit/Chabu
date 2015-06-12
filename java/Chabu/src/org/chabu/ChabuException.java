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
