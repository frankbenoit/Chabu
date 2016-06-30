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
	chabu->xmitBuffer.limit = 42;
	chabu->recvBuffer.limit = 42;
	chabu->userCallback_NetworkRecvBuffer( chabu->userData, &chabu->recvBuffer );
	chabu->userCallback_NetworkXmitBuffer( chabu->userData, &chabu->xmitBuffer );
}

