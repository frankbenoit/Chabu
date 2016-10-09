/*******************************************************************************
 * The MIT License =MIT)
 * Copyright =c) 2015 Frank Benoit, Stuttgart, Germany <keinfarbton@gmail.com>
 * 
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 * 
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
using System;
namespace Org.Chabu.Prot.V1
{


/**
 * Error codes produced by chabu.
 */
    public enum ChabuErrorCode : int {
	    OK_NOERROR       = 0x00000,
        UNKNOWN          = 0x10000,
        ASSERT           = 0x20000, 
	    NOT_ACTIVATED              = 0x30000,
	    IS_ACTIVATED               = 0x40000,

	    CONFIGURATION                        = 0xA000B, 
	    CONFIGURATION_PRIOCOUNT              = 0xA000B, 
	    CONFIGURATION_NETWORK                = 0xA000C, 
	    CONFIGURATION_CH_ID                  = 0xA000D, 
	    CONFIGURATION_CH_PRIO                = 0xA000E, 
	    CONFIGURATION_CH_USER                = 0xA000F, 
	    CONFIGURATION_CH_RECVSZ              = 0xA0010, 
	    CONFIGURATION_NO_CHANNELS            = 0xA0011, 
	    CONFIGURATION_VALIDATOR              = 0xA0012,

	    SETUP_LOCAL                          = 0x150000,
	    SETUP_LOCAL_MAXRECVSIZE_TOO_LOW      = 0x150001,
	    SETUP_LOCAL_MAXRECVSIZE_TOO_HIGH     = 0x150002,
	    SETUP_LOCAL_MAXRECVSIZE_NOT_ALIGNED  = 0x150003,
	    SETUP_LOCAL_APPLICATIONNAME_TOO_LONG = 0x150004, 
	    SETUP_LOCAL_APPLICATIONNAME_NULL     = 0x150005, 

	    SETUP_REMOTE                         = 0x1f0000,
	    SETUP_REMOTE_CHABU_NAME              = 0x1f0001,
	    SETUP_REMOTE_CHABU_VERSION           = 0x1f0002,
	    SETUP_REMOTE_MAXRECVSIZE_TOO_LOW     = 0x200003,
	    SETUP_REMOTE_MAXRECVSIZE_TOO_HIGH    = 0x200004,
	    SETUP_REMOTE_MAXRECVSIZE_NOT_ALIGNED = 0x200005,

	    PROTOCOL                             = 0x320000,
	    PROTOCOL_LENGTH                      = 0x32000A,
	    PROTOCOL_PCK_TYPE                    = 0x32000B,
	    PROTOCOL_ABORT_MSG_LENGTH            = 0x32000C, 
	    PROTOCOL_SETUP_TWICE                 = 0x32000D,
	    PROTOCOL_ACCEPT_TWICE                = 0x32000E,
	    PROTOCOL_EXPECTED_SETUP              = 0x32000F,
	    PROTOCOL_CHANNEL_RECV_OVERFLOW       = 0x320010,  
	    PROTOCOL_DATA_OVERFLOW               = 0x320011,
	    PROTOCOL_ACCEPT_WITHOUT_SETUP        = 0x320012,
	
	    REMOTE_ABORT                         = 0x640000, 

	    // Application can use code starting with 0x100 .. 0x1FF
	    APPLICATION_VALIDATOR   = 0x1000000
	}
}