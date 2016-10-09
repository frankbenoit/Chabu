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
namespace Org.Chabu.Prot.V1
{


/**
 * Error codes produced by chabu.
 */
    public enum ChabuErrorCode {
	    OK_NOERROR       = 0 ,
	    UNKNOWN          = 1 ,
	    ASSERT           = 2 , 
	    NOT_ACTIVATED              = 3 ,
	    IS_ACTIVATED               = 4 ,

	    CONFIGURATION_PRIOCOUNT   = 11 , 
	    CONFIGURATION_NETWORK     = 12 , 
	    CONFIGURATION_CH_ID       = 13 , 
	    CONFIGURATION_CH_PRIO     = 14 , 
	    CONFIGURATION_CH_USER     = 15 , 
	    CONFIGURATION_CH_RECVSZ   = 16 , 
	    CONFIGURATION_NO_CHANNELS = 17 , 
	    CONFIGURATION_VALIDATOR   = 18 ,
	
	    SETUP_LOCAL_MAXRECVSIZE     = 21 ,
	    SETUP_LOCAL_APPLICATIONNAME = 23 , 
	
	    SETUP_REMOTE_CHABU_NAME     = 31 ,
	    SETUP_REMOTE_CHABU_VERSION  = 31 ,
	    SETUP_REMOTE_MAXRECVSIZE    = 32 ,
	
	    PROTOCOL_LENGTH              = 50 ,
	    PROTOCOL_PCK_TYPE            = 51 ,
	    PROTOCOL_ABORT_MSG_LENGTH    = 52 , 
	    PROTOCOL_SETUP_TWICE         = 53 ,
	    PROTOCOL_ACCEPT_TWICE        = 54 ,
	    PROTOCOL_EXPECTED_SETUP        = 55 ,
	    PROTOCOL_CHANNEL_RECV_OVERFLOW = 56 ,  
	    PROTOCOL_DATA_OVERFLOW=57,
	
	    REMOTE_ABORT                 =100, 

	    // Application can use code starting with 0x100 .. 0x1FF
	    APPLICATION_VALIDATOR   = 0x100 
	
    }
}