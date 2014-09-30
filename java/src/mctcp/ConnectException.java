package mctcp;

import java.io.IOException;

public class ConnectException extends IOException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int code;

	public ConnectException( int code, String message ){
		super(message);
		this.code = code;
	}
	
	public int getCode() {
		return code;
	}
}
