/*
 * Chabu.c
 *
 *  Created on: 29.06.2016
 *      Author: Frank
 */

#include "Chabu.h"
#include <stdarg.h>
#include <stdio.h>

struct Chabu_StructInfo {
	const char* name;
};

const static struct Chabu_StructInfo structInfo_chabu   = { "chabu" };
const static struct Chabu_StructInfo structInfo_channel = { "channel" };


#define CHANNEL_COUNT_MAX  100
#define PRIORITY_COUNT_MAX 100
#define APN_MAX_LENGTH      56

#define REPORT_ERROR_IF( cond, c, e, f, ... ) do { if( (cond) ) { reportError( (c), (e), __FILE__, __LINE__, (f), ##__VA_ARGS__ ); } } while(false)
#define REPORT_ERROR( c, e, f, ... ) reportError( (c), (e), __FILE__, __LINE__, (f), ##__VA_ARGS__ )

static void reportError( struct Chabu_Data* chabu, enum Chabu_ErrorCode error, const char* file, int line, const char* fmt, ... ){
	va_list arglist;
	va_start( arglist, fmt );
	if( chabu->lastError == Chabu_ErrorCode_OK_NOERROR ){
		chabu->lastError = error;
		vsnprintf( chabu->errorMessage, sizeof( chabu->errorMessage), fmt, arglist );
		chabu->userCallback_ErrorFunction( chabu->userData, error, file, line, chabu->errorMessage );
	}
	va_end( arglist );
}

LIBRARY_API void Chabu_Init(
		struct Chabu_Data* chabu,

		int           applicationVersion,
		const char*   applicationName,

		struct Chabu_Channel_Data*  channels,
		int                         channelCount,

		struct Chabu_Priority_Data* priorities,
		int                         priorityCount,

		Chabu_ErrorFunction               * userCallback_ErrorFunction,
		Chabu_ConfigureChannels           * userCallback_ConfigureChannels,
		Chabu_NetworkRegisterWriteRequest * userCallback_NetworkRegisterWriteRequest,
		Chabu_NetworkRecvBuffer           * userCallback_NetworkRecvBuffer,
		Chabu_NetworkXmitBuffer           * userCallback_NetworkXmitBuffer,
		void * userData ){

	if( chabu == NULL ) return;

	memset( chabu, 0, sizeof(struct Chabu_Data));

	chabu->info = &structInfo_chabu;
	chabu->lastError = Chabu_ErrorCode_OK_NOERROR;

	if( userCallback_ErrorFunction == NULL ){
		chabu->lastError = Chabu_ErrorCode_INIT_ERROR_FUNC_NULL;
		return;
	}
	chabu->userCallback_ErrorFunction = userCallback_ErrorFunction;

	REPORT_ERROR_IF(( applicationName == NULL ),
			chabu, Chabu_ErrorCode_INIT_PARAM_APNAME_NULL, "application protocol name must not be NULL" );
	REPORT_ERROR_IF(( Common_strnlen( applicationName, APN_MAX_LENGTH+1 ) > APN_MAX_LENGTH ),
			chabu, Chabu_ErrorCode_INIT_PARAM_APNAME_TOO_LONG, "application protocol name exceeds maximum length of %d chars", APN_MAX_LENGTH );
	REPORT_ERROR_IF(( channels == NULL ),
			chabu, Chabu_ErrorCode_INIT_PARAM_CHANNELS_NULL, "channels must not be NULL" );
	REPORT_ERROR_IF(( channelCount <= 0 || channelCount > CHANNEL_COUNT_MAX ),
			chabu, Chabu_ErrorCode_INIT_PARAM_CHANNELS_RANGE, "count of channels must be in range 0 .. %d", CHANNEL_COUNT_MAX );
	REPORT_ERROR_IF(( priorities == NULL ),
			chabu, Chabu_ErrorCode_INIT_PARAM_PRIORITIES_NULL, "priorities must not be NULL" );
	REPORT_ERROR_IF(( priorityCount <= 0 || priorityCount > PRIORITY_COUNT_MAX ),
			chabu, Chabu_ErrorCode_INIT_PARAM_PRIORITIES_RANGE, "count of priorities must be in range 0 .. %d", PRIORITY_COUNT_MAX );
	REPORT_ERROR_IF(( userCallback_ConfigureChannels == NULL ),
			chabu, Chabu_ErrorCode_INIT_CONFIGURE_FUNC_NULL, "callback is null: 'userCallback_ConfigureChannels'" );
	REPORT_ERROR_IF(( userCallback_NetworkRegisterWriteRequest == NULL ),
			chabu, Chabu_ErrorCode_INIT_NW_WRITE_REQ_FUNC_NULL, "callback is null: 'userCallback_NetworkRegisterWriteRequest'" );
	REPORT_ERROR_IF(( userCallback_NetworkRecvBuffer == NULL ),
			chabu, Chabu_ErrorCode_INIT_NW_READ_FUNC_NULL, "callback is null: 'userCallback_NetworkRecvBuffer'" );
	REPORT_ERROR_IF(( userCallback_NetworkXmitBuffer == NULL ),
			chabu, Chabu_ErrorCode_INIT_NW_WRITE_FUNC_NULL, "callback is null: 'userCallback_NetworkXmitBuffer'" );

	if( chabu->lastError != Chabu_ErrorCode_OK_NOERROR ) return;

	chabu->userCallback_NetworkRecvBuffer = userCallback_NetworkRecvBuffer;
	chabu->userCallback_NetworkXmitBuffer = userCallback_NetworkXmitBuffer;
	chabu->userCallback_NetworkRegisterWriteRequest = userCallback_NetworkRegisterWriteRequest;

	chabu->userData = userData;

	chabu->channels     = channels;
	chabu->channelCount = channelCount;
	chabu->priorities    = priorities;
	chabu->priorityCount = priorityCount;

	int i;

	for( i = 0; i < channelCount; i++ ){
		struct Chabu_Channel_Data* ch = &channels[i];
		ch->info = &structInfo_channel;
		ch->channelId = i;
		ch->priority = -1;
		ch->chabu = chabu;
	}

	// <-- Call the user to configure the channels -->
	userCallback_ConfigureChannels( userData );

	for( i = 0; i < channelCount; i++ ){
		struct Chabu_Channel_Data* ch = &channels[i];

		REPORT_ERROR_IF(( ch->priority < 0 || ch->priority >= priorityCount ),
				chabu, Chabu_ErrorCode_INIT_CHANNELS_NOT_CONFIGURED, "At least one channel was not configured: [%d]", i );
	}


}

/**
 * Calls are only allowed from within the Chabu userCallback on event Chabu_Event_InitChannels
 */
LIBRARY_API void Chabu_ConfigureChannel (
		struct Chabu_Data* chabu,
		int channelId,
		int priority,
		Chabu_ChannelGetXmitBuffer * userCallback_ChannelGetXmitBuffer,
		Chabu_ChannelXmitCompleted * userCallback_ChannelXmitCompleted,
		Chabu_ChannelGetRecvBuffer * userCallback_ChannelGetRecvBuffer,
		Chabu_ChannelRecvCompleted * userCallback_ChannelRecvCompleted,
		void * userData ){


	struct Chabu_Channel_Data* ch = &chabu->channels[ channelId ];

	REPORT_ERROR_IF(( channelId < 0 || channelId >= chabu->channelCount ),
			chabu, Chabu_ErrorCode_INIT_CONFIGURE_INVALID_CHANNEL, "channel id invalid" );
	REPORT_ERROR_IF(( userCallback_ChannelGetXmitBuffer == NULL ),
			chabu, Chabu_ErrorCode_INIT_CHANNEL_FUNCS_NULL, "channel callback was NULL: 'userCallback_ChannelGetXmitBuffer'" );
	REPORT_ERROR_IF(( userCallback_ChannelXmitCompleted == NULL ),
			chabu, Chabu_ErrorCode_INIT_CHANNEL_FUNCS_NULL, "channel callback was NULL: 'userCallback_ChannelXmitCompleted'" );
	REPORT_ERROR_IF(( userCallback_ChannelGetRecvBuffer == NULL ),
			chabu, Chabu_ErrorCode_INIT_CHANNEL_FUNCS_NULL, "channel callback was NULL: 'userCallback_ChannelGetRecvBuffer'" );
	REPORT_ERROR_IF(( userCallback_ChannelRecvCompleted == NULL ),
			chabu, Chabu_ErrorCode_INIT_CHANNEL_FUNCS_NULL, "channel callback was NULL: 'userCallback_ChannelRecvCompleted'" );

	if( chabu->lastError != Chabu_ErrorCode_OK_NOERROR ) return;

	ch->priority = priority;
	ch->userCallback_ChannelGetXmitBuffer = userCallback_ChannelGetXmitBuffer;
	ch->userCallback_ChannelXmitCompleted = userCallback_ChannelXmitCompleted;
	ch->userCallback_ChannelGetRecvBuffer = userCallback_ChannelGetRecvBuffer;
	ch->userCallback_ChannelRecvCompleted = userCallback_ChannelRecvCompleted;
	ch->userData = userData;
}

LIBRARY_API enum Chabu_ErrorCode  Chabu_LastError( struct Chabu_Data* chabu ){
	if( chabu == NULL ){
		return Chabu_ErrorCode_CHABU_IS_NULL;
	}
	if( chabu->info != &structInfo_chabu ){
		return Chabu_ErrorCode_CHABU_IS_NOT_INITIALIZED;
	}
	return chabu->lastError;
}

/////////////////////////////////////////////////////////
// Network <-> Chabu

LIBRARY_API extern void Chabu_HandleNetwork ( struct Chabu_Data* chabu ){
}

/////////////////////////////////////////////////////////
// Chabu Channel <-> Application

LIBRARY_API extern void  Chabu_Channel_SetXmitLimit( struct Chabu_Data* chabu, int channelId, int64 limit ){

}
LIBRARY_API extern void  Chabu_Channel_AddXmitLimit( struct Chabu_Data* chabu, int channelId, int added ){

}
LIBRARY_API extern int64 Chabu_Channel_GetXmitLimit( struct Chabu_Data* chabu, int channelId ){
	return 0;
}
LIBRARY_API extern int64 Chabu_Channel_GetXmitPosition( struct Chabu_Data* chabu, int channelId ){
	return 0;
}
LIBRARY_API extern int   Chabu_Channel_GetXmitRemaining( struct Chabu_Data* chabu, int channelId ){
	return 0;
}

LIBRARY_API extern void  Chabu_Channel_SetRecvLimit( struct Chabu_Data* chabu, int channelId, int64 limit ){

}
LIBRARY_API extern void  Chabu_Channel_AddRecvLimit( struct Chabu_Data* chabu, int channelId, int added ){

}
LIBRARY_API extern int64 Chabu_Channel_GetRecvLimit( struct Chabu_Data* chabu, int channelId ){
	return 0;
}
LIBRARY_API extern int64 Chabu_Channel_GetRecvPosition( struct Chabu_Data* chabu, int channelId ){
	return 0;
}
LIBRARY_API extern int   Chabu_Channel_GetRecvRemaining( struct Chabu_Data* chabu, int channelId ){
	return 0;
}
