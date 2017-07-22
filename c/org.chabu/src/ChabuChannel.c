/*
 * ChabuChannel.c
 *
 *  Created on: 30.06.2016
 *      Author: Frank
 */

#include "Chabu.h"
#include "ChabuInternal.h"

static struct Chabu_Channel_Data* getCheckedChannelId( struct Chabu_Data* chabu, int channelId ){
	if( channelId < 0 || channelId >= chabu->channelCount ){
		Chabu_ReportError( chabu, Chabu_ErrorCode_PARAMETER, __FILE__, __LINE__,
				"ch[%d] does not exist. %d available channels", channelId, chabu->channelCount );
		return NULL;
	}
	return &chabu->channels[ channelId ];
}

LIBRARY_API extern void  Chabu_Channel_SetXmitLimit( struct Chabu_Data* chabu, int channelId, int64 limit ){
	struct Chabu_Channel_Data* ch = getCheckedChannelId( chabu, channelId );
	if( !ch ) return;

	if( limit < ch->xmit.limit ) {
		Chabu_ReportError( chabu, Chabu_ErrorCode_PARAMETER, __FILE__, __LINE__,
				"Chabu_Channel_SetXmitLimit: parameter limit was smaller then previously set." );
		return;
	}

	ch->xmit.limit = limit;
	if( ch->xmit.arm != ch->xmit.seq ){
		Chabu_Priority_SetRequestData( chabu, ch );
		chabu->user.eventNotification( chabu->user.data, Chabu_Event_NetworkRegisterWriteRequest );
	}
}

LIBRARY_API extern void  Chabu_Channel_AddXmitLimit( struct Chabu_Data* chabu, int channelId, int added ){

	struct Chabu_Channel_Data* ch = getCheckedChannelId( chabu, channelId );
	if( !ch ) return;

	if( added < 0 ) {
		Chabu_ReportError( chabu, Chabu_ErrorCode_PARAMETER, __FILE__, __LINE__,
				"Chabu_Channel_AddXmitLimit: parameter added was given with negative number: %d", added );
		return;
	}
	if( added == 0 ) {
		return;
	}

	ch->xmit.limit += added;
	if( ch->xmit.arm != ch->xmit.seq ){
		Chabu_Priority_SetRequestData( chabu, ch );
		chabu->user.eventNotification( chabu->user.data, Chabu_Event_NetworkRegisterWriteRequest );
	}
}

LIBRARY_API extern int64 Chabu_Channel_GetXmitLimit( struct Chabu_Data* chabu, int channelId ){
	struct Chabu_Channel_Data* ch = getCheckedChannelId( chabu, channelId );
	if( !ch ) return 0;
	return ch->xmit.limit;
}

LIBRARY_API extern int64 Chabu_Channel_GetXmitPosition( struct Chabu_Data* chabu, int channelId ){
	struct Chabu_Channel_Data* ch = getCheckedChannelId( chabu, channelId );
	if( !ch ) return 0;
	return ch->xmit.seq;
}

LIBRARY_API extern int64 Chabu_Channel_GetXmitRemaining( struct Chabu_Data* chabu, int channelId ){
	struct Chabu_Channel_Data* ch = getCheckedChannelId( chabu, channelId );
	if( !ch ) return 0;
	return ch->xmit.limit - ch->xmit.seq;
}

LIBRARY_API extern void  Chabu_Channel_SetRecvLimit( struct Chabu_Data* chabu, int channelId, int64 limit ){
	struct Chabu_Channel_Data* ch = getCheckedChannelId( chabu, channelId );
	if( !ch ) return;
	ch->xmit.limit = limit;
}

LIBRARY_API extern void  Chabu_Channel_AddRecvLimit( struct Chabu_Data* chabu, int channelId, int added ){
	struct Chabu_Channel_Data* ch = getCheckedChannelId( chabu, channelId );
	if( !ch ) return;
	if( added < 0 ) {
		Chabu_ReportError( chabu, Chabu_ErrorCode_PARAMETER, __FILE__, __LINE__,
				"Chabu_Channel_AddRecvLimit: parameter added was given with negative number: %d", added );
		return;
	}
	if( added == 0 ) {
		return;
	}
	ch->recv.arm += added;
	ch->recv.limit += added;
	Chabu_Priority_SetRequestCtrl_Arm( chabu, ch );
	chabu->user.eventNotification( chabu->user.data, Chabu_Event_NetworkRegisterWriteRequest );
}

LIBRARY_API extern int64 Chabu_Channel_GetRecvLimit( struct Chabu_Data* chabu, int channelId ){
	struct Chabu_Channel_Data* ch = getCheckedChannelId( chabu, channelId );
	if( !ch ) return 0;
	return ch->recv.limit;
}

LIBRARY_API extern int64 Chabu_Channel_GetRecvPosition( struct Chabu_Data* chabu, int channelId ){
	struct Chabu_Channel_Data* ch = getCheckedChannelId( chabu, channelId );
	if( !ch ) return 0;
	return ch->recv.seq;
}

LIBRARY_API extern int64   Chabu_Channel_GetRecvRemaining( struct Chabu_Data* chabu, int channelId ){
	struct Chabu_Channel_Data* ch = getCheckedChannelId( chabu, channelId );
	if( !ch ) return 0;
	return ch->recv.limit - ch->recv.seq;
}

