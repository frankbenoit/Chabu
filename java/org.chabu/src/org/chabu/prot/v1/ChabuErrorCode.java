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

/**
 * Error codes produced by chabu.
 */
public enum ChabuErrorCode {
	OK_NOERROR       ( 0 ),
	UNKNOWN          ( 1 ),
	ASSERT           ( 2 ), 
	NOT_ACTIVATED              ( 3 ),
	IS_ACTIVATED               ( 4 ),

	CONFIGURATION                        ( 10, 11 ), 
	CONFIGURATION_PRIOCOUNT              ( 10, 11 ), 
	CONFIGURATION_NETWORK                ( 10, 12 ), 
	CONFIGURATION_CH_ID                  ( 10, 13 ), 
	CONFIGURATION_CH_PRIO                ( 10, 14 ), 
	CONFIGURATION_CH_USER                ( 10, 15 ), 
	CONFIGURATION_CH_RECVSZ              ( 10, 16 ), 
	CONFIGURATION_NO_CHANNELS            ( 10, 17 ), 
	CONFIGURATION_VALIDATOR              ( 10, 18 ),
	
	SETUP_LOCAL                          ( 21 ),
	SETUP_LOCAL_MAXRECVSIZE_TOO_LOW      ( 21, 1 ),
	SETUP_LOCAL_MAXRECVSIZE_TOO_HIGH     ( 21, 2 ),
	SETUP_LOCAL_MAXRECVSIZE_NOT_ALIGNED  ( 21, 3 ),
	SETUP_LOCAL_APPLICATIONNAME_TOO_LONG ( 21, 4 ), 
	SETUP_LOCAL_APPLICATIONNAME_NULL     ( 21, 5 ), 
	
	SETUP_REMOTE                         ( 31 ),
	SETUP_REMOTE_CHABU_NAME              ( 31, 1 ),
	SETUP_REMOTE_CHABU_VERSION           ( 31, 2 ),
	SETUP_REMOTE_MAXRECVSIZE_TOO_LOW     ( 32, 3 ),
	SETUP_REMOTE_MAXRECVSIZE_TOO_HIGH    ( 32, 4 ),
	SETUP_REMOTE_MAXRECVSIZE_NOT_ALIGNED ( 32, 5 ),
	
	PROTOCOL                             ( 50 ),
	PROTOCOL_LENGTH                      ( 50, 10 ),
	PROTOCOL_PCK_TYPE                    ( 50, 11 ),
	PROTOCOL_ABORT_MSG_LENGTH            ( 50, 12 ), 
	PROTOCOL_SETUP_TWICE                 ( 50, 13 ),
	PROTOCOL_ACCEPT_TWICE                ( 50, 14 ),
	PROTOCOL_EXPECTED_SETUP              ( 50, 15 ),
	PROTOCOL_CHANNEL_RECV_OVERFLOW       ( 50, 16 ),  
	PROTOCOL_DATA_OVERFLOW               ( 50, 17),
	
	REMOTE_ABORT                         (100), 

	// Application can use code starting with 0x100 .. 0x1FF
	APPLICATION_VALIDATOR   ( 0x100 ), 
	;
	
	private final int code;

	private ChabuErrorCode( int code, int subCode ){
		this.code = code * 0x10000 + subCode;
	}
	private ChabuErrorCode( int code ){
		this.code = code * 0x10000;
	}
	
	public int getCode() {
		return code;
	}
	
}