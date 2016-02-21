package org.chabu.internal;

public interface Constants {
	
	static final String PROTOCOL_NAME    = "CHABU";
	
	/**
	 * Number constant for the current protocol version.<br/>
	 * Actually this is version 0.1.
	 */
	static final int    PROTOCOL_VERSION = 0x0000_0001;

	static final int MAX_RECV_LIMIT_HIGH = 0x10000;
	static final int MAX_RECV_LIMIT_LOW = 0x100;

}
