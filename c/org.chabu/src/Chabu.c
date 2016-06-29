/*
 * Chabu.c
 *
 *  Created on: 29.06.2016
 *      Author: Frank
 */

#include "Chabu.h"

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

}

LIBRARY_API enum Chabu_ErrorCode  Chabu_LastError( struct Chabu_Data* chabu ){
	if( chabu == NULL ){
		return Chabu_ErrorCode_CHABU_IS_NULL;
	}
	if( chabu->errorFunction == NULL ){
		return Chabu_ErrorCode_CHABU_IS_NOT_INITIALIZED;
	}
	return Chabu_ErrorCode_ASSERT;
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
