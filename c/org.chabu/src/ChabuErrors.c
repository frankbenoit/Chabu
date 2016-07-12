/*
 * ChabuErrors.c
 *
 *  Created on: 30.06.2016
 *      Author: Frank
 */

#include "Chabu.h"

extern const struct Chabu_StructInfo structInfo_chabu;
//extern const struct Chabu_StructInfo structInfo_channel;

LIBRARY_API enum Chabu_ErrorCode  Chabu_LastError( struct Chabu_Data* chabu ){
	if( chabu == NULL ){
		return Chabu_ErrorCode_CHABU_IS_NULL;
	}
	if( chabu->info != &structInfo_chabu ){
		return Chabu_ErrorCode_CHABU_IS_NOT_INITIALIZED;
	}
	return chabu->lastError;
}

LIBRARY_API extern const char*  Chabu_LastErrorStr( struct Chabu_Data* chabu ){
	switch( Chabu_LastError( chabu )){
	case Chabu_ErrorCode_OK_NOERROR                      : return "Chabu_ErrorCode_OK_NOERROR";
	case Chabu_ErrorCode_UNKNOWN                        : return "Chabu_ErrorCode_UNKNOWN";
	case Chabu_ErrorCode_ASSERT                         : return "Chabu_ErrorCode_ASSERT";
	case Chabu_ErrorCode_NOT_ACTIVATED                  : return "Chabu_ErrorCode_NOT_ACTIVATED";
	case Chabu_ErrorCode_IS_ACTIVATED                   : return "Chabu_ErrorCode_IS_ACTIVATED";
	case Chabu_ErrorCode_ILLEGAL_ARGUMENT               : return "Chabu_ErrorCode_ILLEGAL_ARGUMENT";
	case Chabu_ErrorCode_CHABU_IS_NULL                  : return "Chabu_ErrorCode_CHABU_IS_NULL";
	case Chabu_ErrorCode_CHABU_IS_NOT_INITIALIZED       : return "Chabu_ErrorCode_CHABU_IS_NOT_INITIALIZED";
	case Chabu_ErrorCode_INIT_ERROR_FUNC_NULL           : return "Chabu_ErrorCode_INIT_ERROR_FUNC_NULL";
	case Chabu_ErrorCode_INIT_PARAM_APNAME_NULL         : return "Chabu_ErrorCode_INIT_PARAM_APNAME_NULL";
	case Chabu_ErrorCode_INIT_PARAM_APNAME_TOO_LONG     : return "Chabu_ErrorCode_INIT_PARAM_APNAME_TOO_LONG";
	case Chabu_ErrorCode_INIT_PARAM_RPS_RANGE           : return "Chabu_ErrorCode_INIT_PARAM_RPS_RANGE";
	case Chabu_ErrorCode_INIT_CONFIGURE_INVALID_CHANNEL : return "Chabu_ErrorCode_INIT_CONFIGURE_INVALID_CHANNEL";
	case Chabu_ErrorCode_INIT_EVENT_FUNC_NULL           : return "Chabu_ErrorCode_INIT_EVENT_FUNC_NULL";
	case Chabu_ErrorCode_INIT_NW_READ_FUNC_NULL         : return "Chabu_ErrorCode_INIT_NW_READ_FUNC_NULL";
	case Chabu_ErrorCode_INIT_NW_WRITE_FUNC_NULL        : return "Chabu_ErrorCode_INIT_NW_WRITE_FUNC_NULL";
	case Chabu_ErrorCode_INIT_PARAM_CHANNELS_NULL       : return "Chabu_ErrorCode_INIT_PARAM_CHANNELS_NULL";
	case Chabu_ErrorCode_INIT_PARAM_CHANNELS_RANGE      : return "Chabu_ErrorCode_INIT_PARAM_CHANNELS_RANGE";
	case Chabu_ErrorCode_INIT_PARAM_PRIORITIES_NULL     : return "Chabu_ErrorCode_INIT_PARAM_PRIORITIES_NULL";
	case Chabu_ErrorCode_INIT_PARAM_PRIORITIES_RANGE    : return "Chabu_ErrorCode_INIT_PARAM_PRIORITIES_RANGE";
	case Chabu_ErrorCode_INIT_CHANNEL_FUNCS_NULL        : return "Chabu_ErrorCode_INIT_CHANNEL_FUNCS_NULL";
	case Chabu_ErrorCode_INIT_CHANNELS_NOT_CONFIGURED   : return "Chabu_ErrorCode_INIT_CHANNELS_NOT_CONFIGURED";
	case Chabu_ErrorCode_CONFIGURATION_PRIOCOUNT        : return "Chabu_ErrorCode_CONFIGURATION_PRIOCOUNT";
	case Chabu_ErrorCode_CONFIGURATION_NETWORK          : return "Chabu_ErrorCode_CONFIGURATION_NETWORK";
	case Chabu_ErrorCode_CONFIGURATION_CH_ID            : return "Chabu_ErrorCode_CONFIGURATION_CH_ID";
	case Chabu_ErrorCode_CONFIGURATION_CH_PRIO          : return "Chabu_ErrorCode_CONFIGURATION_CH_PRIO";
	case Chabu_ErrorCode_CONFIGURATION_CH_USER          : return "Chabu_ErrorCode_CONFIGURATION_CH_USER";
	case Chabu_ErrorCode_CONFIGURATION_CH_RECVSZ        : return "Chabu_ErrorCode_CONFIGURATION_CH_RECVSZ";
	case Chabu_ErrorCode_CONFIGURATION_NO_CHANNELS      : return "Chabu_ErrorCode_CONFIGURATION_NO_CHANNELS";
	case Chabu_ErrorCode_CONFIGURATION_VALIDATOR        : return "Chabu_ErrorCode_CONFIGURATION_VALIDATOR";
	case Chabu_ErrorCode_SETUP_LOCAL_MAXRECVSIZE        : return "Chabu_ErrorCode_SETUP_LOCAL_MAXRECVSIZE";
	case Chabu_ErrorCode_SETUP_LOCAL_APPLICATIONNAME    : return "Chabu_ErrorCode_SETUP_LOCAL_APPLICATIONNAME";
	case Chabu_ErrorCode_SETUP_REMOTE_CHABU_VERSION     : return "Chabu_ErrorCode_SETUP_REMOTE_CHABU_VERSION";
	case Chabu_ErrorCode_SETUP_REMOTE_CHABU_NAME        : return "Chabu_ErrorCode_SETUP_REMOTE_CHABU_NAME";
	case Chabu_ErrorCode_SETUP_REMOTE_MAXRECVSIZE       : return "Chabu_ErrorCode_SETUP_REMOTE_MAXRECVSIZE";
	case Chabu_ErrorCode_PROTOCOL_LENGTH                : return "Chabu_ErrorCode_PROTOCOL_LENGTH";
	case Chabu_ErrorCode_PROTOCOL_PCK_TYPE              : return "Chabu_ErrorCode_PROTOCOL_PCK_TYPE";
	case Chabu_ErrorCode_PROTOCOL_ABORT_MSG_LENGTH      : return "Chabu_ErrorCode_PROTOCOL_ABORT_MSG_LENGTH";
	case Chabu_ErrorCode_PROTOCOL_SETUP_TWICE           : return "Chabu_ErrorCode_PROTOCOL_SETUP_TWICE";
	case Chabu_ErrorCode_PROTOCOL_ACCEPT_TWICE          : return "Chabu_ErrorCode_PROTOCOL_ACCEPT_TWICE";
	case Chabu_ErrorCode_PROTOCOL_EXPECTED_SETUP        : return "Chabu_ErrorCode_PROTOCOL_EXPECTED_SETUP";
	case Chabu_ErrorCode_PROTOCOL_CHANNEL_RECV_OVERFLOW : return "Chabu_ErrorCode_PROTOCOL_CHANNEL_RECV_OVERFLOW";
	case Chabu_ErrorCode_PROTOCOL_DATA_OVERFLOW         : return "Chabu_ErrorCode_PROTOCOL_DATA_OVERFLOW";
	case Chabu_ErrorCode_REMOTE_ABORT                   : return "Chabu_ErrorCode_REMOTE_ABORT";
	case Chabu_ErrorCode_APPLICATION_VALIDATOR          : return "Chabu_ErrorCode_APPLICATION_VALIDATOR";
	default: return "<unknown>";
	}
}

