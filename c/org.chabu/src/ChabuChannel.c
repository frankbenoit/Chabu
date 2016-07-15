/*
 * ChabuChannel.c
 *
 *  Created on: 30.06.2016
 *      Author: Frank
 */

#include "Chabu.h"
#include "ChabuInternal.h"

LIBRARY_API extern void  Chabu_Channel_SetXmitLimit( struct Chabu_Data* chabu, int channelId, int64 limit ){

}

LIBRARY_API extern void  Chabu_Channel_AddXmitLimit( struct Chabu_Data* chabu, int channelId, int added ){

	if( added == 0 ) {
		return;
	}

	if( channelId < 0 || channelId >= chabu->channelCount ){
		Chabu_ReportError( chabu, Chabu_ErrorCode_PARAMETER, __FILE__, __LINE__,
				"ch[%d] does not exist. %d available channels", channelId, chabu->channelCount );
		return;
	}
	if( added < 0 ){
		Chabu_ReportError( chabu, Chabu_ErrorCode_PARAMETER, __FILE__, __LINE__,
				"Chabu_Channel_AddXmitLimit: parameter added was given with negative number: %d", added );
		return;
	}

	struct Chabu_Channel_Data* ch = &chabu->channels[ channelId ];
	ch->xmitLimit += added;
	if( !ch->xmitRequestData ){
		if( ch->xmitArm != ch->xmitSeq ){
			ch->xmitRequestData = true;
			chabu->userCallback_EventNotification( chabu->userData, Chabu_Event_NetworkRegisterWriteRequest );
		}
	}
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
	struct Chabu_Channel_Data* ch = &chabu->channels[ channelId ];
	ch->recvArm += added;
	ch->xmitRequestCtrl = true;
	chabu->userCallback_EventNotification( chabu->userData, Chabu_Event_NetworkRegisterWriteRequest );
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


