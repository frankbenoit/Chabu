/*
 * ChabuInit.c
 *
 *  Created on: 30.06.2016
 *      Author: Frank
 */



#include "Chabu.h"

#include "ChabuInternal.h"
#include <stdarg.h>
#include <stdio.h>
#include <string.h>

static void buildPriorityChannelList( struct Chabu_Data* chabu );

void Chabu_ReportError( struct Chabu_Data* chabu, enum Chabu_ErrorCode error, const char* file, int line, const char* fmt, ... ){
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
		int           receivePacketSize,

		struct Chabu_Channel_Data*  channels,
		int                         channelCount,

		struct Chabu_Priority_Data* priorities,
		int                         priorityCount,

		Chabu_ErrorFunction               * userCallback_ErrorFunction,
		Chabu_AcceptConnection            * userCallback_AcceptConnection,
		Chabu_EventNotification           * userCallback_EventNotification,
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
	REPORT_ERROR_IF(( receivePacketSize < RPS_MIN || receivePacketSize > RPS_MAX ),
			chabu, Chabu_ErrorCode_INIT_PARAM_RPS_RANGE, "receivePacketSize must be in range %d .. %d", RPS_MIN, RPS_MAX );
	REPORT_ERROR_IF(( channels == NULL ),
			chabu, Chabu_ErrorCode_INIT_PARAM_CHANNELS_NULL, "channels must not be NULL" );
	REPORT_ERROR_IF(( channelCount <= 0 || channelCount > CHANNEL_COUNT_MAX ),
			chabu, Chabu_ErrorCode_INIT_PARAM_CHANNELS_RANGE, "count of channels must be in range 0 .. %d", CHANNEL_COUNT_MAX );
	REPORT_ERROR_IF(( priorities == NULL ),
			chabu, Chabu_ErrorCode_INIT_PARAM_PRIORITIES_NULL, "priorities must not be NULL" );
	REPORT_ERROR_IF(( priorityCount <= 0 || priorityCount > PRIORITY_COUNT_MAX ),
			chabu, Chabu_ErrorCode_INIT_PARAM_PRIORITIES_RANGE, "count of priorities must be in range 0 .. %d", PRIORITY_COUNT_MAX );
	REPORT_ERROR_IF(( userCallback_EventNotification == NULL ),
			chabu, Chabu_ErrorCode_INIT_EVENT_FUNC_NULL, "callback is null: 'userCallback_EventNotification'" );
	REPORT_ERROR_IF(( userCallback_AcceptConnection == NULL ),
			chabu, Chabu_ErrorCode_INIT_ACCEPT_FUNC_NULL, "callback is null: 'userCallback_AcceptConnection'" );
	REPORT_ERROR_IF(( userCallback_NetworkRecvBuffer == NULL ),
			chabu, Chabu_ErrorCode_INIT_NW_READ_FUNC_NULL, "callback is null: 'userCallback_NetworkRecvBuffer'" );
	REPORT_ERROR_IF(( userCallback_NetworkXmitBuffer == NULL ),
			chabu, Chabu_ErrorCode_INIT_NW_WRITE_FUNC_NULL, "callback is null: 'userCallback_NetworkXmitBuffer'" );

	if( chabu->lastError != Chabu_ErrorCode_OK_NOERROR ) return;

	chabu->userCallback_AcceptConnection = userCallback_AcceptConnection;
	chabu->userCallback_NetworkRecvBuffer = userCallback_NetworkRecvBuffer;
	chabu->userCallback_NetworkXmitBuffer = userCallback_NetworkXmitBuffer;
	chabu->userCallback_EventNotification = userCallback_EventNotification;

	chabu->userData = userData;

	chabu->channels     = channels;
	chabu->channelCount = channelCount;
	chabu->priorities    = priorities;
	chabu->priorityCount = priorityCount;

	chabu->receivePacketSize = receivePacketSize;

	Chabu_ByteBuffer_Init( &chabu->xmit.buffer, chabu->xmit.memory, sizeof(chabu->xmit.memory) );
	Chabu_ByteBuffer_Init( &chabu->recv.buffer, chabu->recv.memory, sizeof(chabu->recv.memory) );

	int i;

	for( i = 0; i < priorityCount; i++ ){
		struct Chabu_Priority_Data* prio = &priorities[i];
		prio->info = &structInfo_priority;

		prio->ctrl.lastSelectedChannelId = -1;
		prio->ctrl.request = NULL;

		prio->data.lastSelectedChannelId = -1;
		prio->data.request = NULL;
	}
	for( i = 0; i < channelCount; i++ ){
		struct Chabu_Channel_Data* ch = &channels[i];
		ch->info = &structInfo_channel;
		ch->channelId = i;
		ch->priority = -1;
		ch->chabu = chabu;

		ch->recvArm = 0;
		ch->recvSeq = 0;
		ch->recvRequest = false;

		ch->xmitArm   = 0;
		ch->xmitSeq   = 0;
		ch->xmitLimit = 0;
		ch->xmitRequestCtrl_Arm     = false;
		ch->xmitRequestCtrl_Davail  = false;
		ch->xmitRequestCtrl_Reset   = false;

//		ch->prioListNextChannel = NULL;
	}

	// <-- Call the user to configure the channels -->
	userCallback_EventNotification( userData, Chabu_Event_InitChannels );

	for( i = 0; i < channelCount; i++ ){
		struct Chabu_Channel_Data* ch = &channels[i];

		REPORT_ERROR_IF(( ch->priority < 0 || ch->priority >= priorityCount ),
				chabu, Chabu_ErrorCode_INIT_CHANNELS_NOT_CONFIGURED, "At least one channel was not configured: [%d]", i );
	}


	int length = 0x24 + Common_AlignUp4(Common_strnlen( applicationName, APN_MAX_LENGTH+1 ));
	Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, length );
	Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, PACKET_MAGIC | PacketType_Setup );
	Chabu_ByteBuffer_putString( &chabu->xmit.buffer, "CHABU" );
	Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, Chabu_ProtocolVersion );
	Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, chabu->receivePacketSize );
	Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, applicationVersion );
	Chabu_ByteBuffer_putString( &chabu->xmit.buffer, applicationName );
	Chabu_ByteBuffer_flip( &chabu->xmit.buffer );

	chabu->xmit.state = Chabu_XmitState_Setup;
	chabu->connectionInfoRemote.hasContent = false;
	chabu->connectionInfoLocal.hasContent = false;

	chabu->recv.state = Chabu_RecvState_Setup;
	chabu->recv.buffer.limit = 8;
	chabu->recv.seqChannel = NULL;
	chabu->recv.seqRemainingPayload = 0;
	chabu->recv.seqRemainingPadding = 0;

	chabu->xmitPing.inProgress = false;
	chabu->xmitPing.request = false;
	chabu->xmitPing.pingData = NULL;
	chabu->xmitPing.pongData = NULL;

	chabu->recvPing.request = false;

	buildPriorityChannelList( chabu );
}

static void buildPriorityChannelList( struct Chabu_Data* chabu ){
	int i;
	for( i = 0; i < chabu->channelCount; i++ ){
		struct Chabu_Channel_Data* ch = &chabu->channels[i];
		ch->xmitRequestCtrl.next = NULL;
		ch->xmitRequestCtrl.ch   = ch;
		ch->xmitRequestData.next = NULL;
		ch->xmitRequestData.ch   = ch;
//		if( chabu->priorities[ch->priority].channelList == NULL ){
//			chabu->priorities[ch->priority].channelList = ch;
//		}
//		else {
//			struct Chabu_Channel_Data* chPrevPrio = chabu->priorities[ch->priority].channelList;
//			while( chPrevPrio->prioListNextChannel ){
//				chPrevPrio++;
//			}
//			chPrevPrio->prioListNextChannel = ch;
//		}
	}
}

/**
 * Calls are only allowed from within the Chabu userCallback on event Chabu_Event_InitChannels
 */
LIBRARY_API void Chabu_ConfigureChannel (
		struct Chabu_Data* chabu,
		int channelId,
		int priority,
		Chabu_ChannelEventNotification * userCallback_ChannelEventNotification,
		Chabu_ChannelGetXmitBuffer * userCallback_ChannelGetXmitBuffer,
		Chabu_ChannelGetRecvBuffer * userCallback_ChannelGetRecvBuffer,
		void * userData ){


	struct Chabu_Channel_Data* ch = &chabu->channels[ channelId ];

	REPORT_ERROR_IF(( channelId < 0 || channelId >= chabu->channelCount ),
			chabu, Chabu_ErrorCode_INIT_CONFIGURE_INVALID_CHANNEL, "channel id invalid" );
	REPORT_ERROR_IF(( userCallback_ChannelEventNotification == NULL ),
			chabu, Chabu_ErrorCode_INIT_CHANNEL_FUNCS_NULL, "channel callback was NULL: 'userCallback_ChannelEventNotification'" );
	REPORT_ERROR_IF(( userCallback_ChannelGetXmitBuffer == NULL ),
			chabu, Chabu_ErrorCode_INIT_CHANNEL_FUNCS_NULL, "channel callback was NULL: 'userCallback_ChannelGetXmitBuffer'" );
	REPORT_ERROR_IF(( userCallback_ChannelGetRecvBuffer == NULL ),
			chabu, Chabu_ErrorCode_INIT_CHANNEL_FUNCS_NULL, "channel callback was NULL: 'userCallback_ChannelGetRecvBuffer'" );

	if( chabu->lastError != Chabu_ErrorCode_OK_NOERROR ) return;

	ch->priority = priority;
	ch->userCallback_ChannelEventNotification = userCallback_ChannelEventNotification;
	ch->userCallback_ChannelGetXmitBuffer     = userCallback_ChannelGetXmitBuffer;
	ch->userCallback_ChannelGetRecvBuffer     = userCallback_ChannelGetRecvBuffer;
	ch->userData = userData;
}


