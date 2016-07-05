/*
 * Chabu.c
 *
 *  Created on: 29.06.2016
 *      Author: Frank
 */

#include "Chabu.h"
#include "ChabuInternal.h"
#include <stdarg.h>
#include <stdio.h>

struct Chabu_StructInfo {
	const char* name;
};

const struct Chabu_StructInfo structInfo_chabu   = { "chabu" };
const struct Chabu_StructInfo structInfo_channel = { "channel" };


LIBRARY_API extern void Chabu_HandleNetwork ( struct Chabu_Data* chabu ){

	if( Chabu_ByteBuffer_hasRemaining( &chabu->xmitBuffer )){

		int written = chabu->userCallback_NetworkXmitBuffer( chabu->userData, &chabu->xmitBuffer );

		if( Chabu_ByteBuffer_hasRemaining( &chabu->xmitBuffer )){
			chabu->userCallback_NetworkRegisterWriteRequest( chabu->userData );
			return;
		}
	}
	chabu->recvBuffer.limit = 42;
	chabu->userCallback_NetworkRecvBuffer( chabu->userData, &chabu->recvBuffer );
}

