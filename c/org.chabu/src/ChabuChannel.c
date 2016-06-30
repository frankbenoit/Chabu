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


