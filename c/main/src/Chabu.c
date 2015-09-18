/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Frank Benoit - Germany, Stuttgart, fr@nk-benoit.de
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#include <string.h>
#include <stdint.h>
#include "Common.h"
#include "Chabu.h"
#include "QueueVar.h"
#include "ByteBuffer.h"

#define Chabu_PROTCOL_VERSION 1

//#define CHABU_DBGPRINTF(...) dbg_printf(__VA_ARGS__)
//#define CHABU_DBGMEMORY(...) dbg_memory(__VA_ARGS__)
#define CHABU_DBGPRINTF(...)
#define CHABU_DBGMEMORY(...)

#define CHABU_PACKETTYPE_MAGIC  0x77770000

#define CHABU_PACKETTYPE_SETUP  0xF0
#define CHABU_PACKETTYPE_ACCEPT 0xE1
#define CHABU_PACKETTYPE_ABORT  0xD2
#define CHABU_PACKETTYPE_ARM    0xC3
#define CHABU_PACKETTYPE_SEQ    0xB4
#define CHABU_MINIMUM_PACKET_SIZE 8


#define CHABU_PACKET_OFFSET_PACKETSIZE   0
#define CHABU_PACKET_OFFSET_PACKETTYPE   4

#define CHABU_ARMPACKET_OFFSET_CHANNEL   8
#define CHABU_ARMPACKET_OFFSET_ARM      12

#define CHABU_SEQPACKET_OFFSET_CHANNEL   8
#define CHABU_SEQPACKET_OFFSET_SEQ      12
#define CHABU_SEQPACKET_OFFSET_DATASIZE 16
#define CHABU_SEQPACKET_OFFSET_DATADATA 20

static void Chabu_Channel_XmitRequestArm( struct Chabu_Channel_Data* channel );

void Chabu_Init(
		struct Chabu_Data* chabu,

		int     priorityCount,

		int     applicationVersion,
		char*   applicationName,
		uint8*  recvBufferMemory,
		int recvBufferSize,

		uint8*  xmitBufferMemory,
		int xmitBufferSize,

		TChabu_UserCallback * userCallback,
		void * userData, 
		TChabu_AssertFunction assertFunction ){

	if( chabu == NULL ){
		assertFunction( Chabu_ErrorCode_ILLEGAL_ARGUMENT, userData, chabu, __FILE__, __LINE__, "The chabu pointer is NULL" );
		return;
	}
	memset( chabu, 0, sizeof(struct Chabu_Data));
	chabu->assertFunction = assertFunction;
	Chabu_AssertPrintf( Chabu_ErrorCode_ILLEGAL_ARGUMENT, recvBufferMemory != NULL, "recvBufferMemory is NULL" );
	Chabu_AssertPrintf( Chabu_ErrorCode_ILLEGAL_ARGUMENT, xmitBufferMemory != NULL, "xmitBufferMemory is NULL" );
	Chabu_AssertPrintf( Chabu_ErrorCode_ILLEGAL_ARGUMENT, (((int)recvBufferMemory) & 0x3) == 0, "recvBufferMemory is not 4-byte aligned" );
	Chabu_AssertPrintf( Chabu_ErrorCode_ILLEGAL_ARGUMENT, (((int)xmitBufferMemory) & 0x3) == 0, "xmitBufferMemory is not 4-byte aligned" );

	Chabu_AssertPrintf( Chabu_ErrorCode_SETUP_LOCAL_MAXRECVSIZE, recvBufferSize >= 0x100, "recvBufferSize shall be at least 0x100, but is %d", recvBufferSize );
	Chabu_AssertPrintf( Chabu_ErrorCode_ILLEGAL_ARGUMENT, xmitBufferSize >= 0x100, "xmitBufferSize shall be at least 0x100, but is %d", xmitBufferSize );

	Chabu_AssertPrintf( Chabu_ErrorCode_SETUP_LOCAL_APPLICATIONNAME, applicationName != NULL, "applicationName shall not be NULL" );

	Chabu_AssertPrintf( Chabu_ErrorCode_CONFIGURATION_PRIOCOUNT, (priorityCount >= 1) && (priorityCount < Chabu_PRIORITY_COUNT_MAX), "prioCount is not in valid range [0..%d), it is %d", Chabu_PRIORITY_COUNT_MAX, priorityCount );

	chabu->channelCount = 0;
	chabu->priorityCount = priorityCount;
	chabu->activated = false;

	chabu->connectionInfoLocal.receiveBufferSize = recvBufferSize;
	chabu->connectionInfoLocal.applicationVersion    = applicationVersion;
	chabu->connectionInfoLocal.applicationNameLength = Chabu_strnlen(applicationName, Chabu_APPLICATION_NAME_SIZE_MAX+1);

	Chabu_AssertPrintf( Chabu_ErrorCode_SETUP_LOCAL_APPLICATIONNAME, 
		chabu->connectionInfoLocal.applicationNameLength <= Chabu_APPLICATION_NAME_SIZE_MAX, 
		"applicationName is biggern than Chabu_APPLICATION_NAME_SIZE_MAX (%d)", Chabu_APPLICATION_NAME_SIZE_MAX  );

	strncpy( chabu->connectionInfoLocal.applicationName, applicationName, Chabu_APPLICATION_NAME_SIZE_MAX );

	memset( chabu->connectionInfoRemote.applicationName, 0, Chabu_APPLICATION_NAME_SIZE_MAX );

	ByteBuffer_Init( &chabu->recvBuffer, recvBufferMemory, recvBufferSize);
	chabu->recvBuffer.limit    = CHABU_MINIMUM_PACKET_SIZE;

	ByteBuffer_Init( &chabu->xmitBuffer, xmitBufferMemory, xmitBufferSize);

	chabu->xmitBuffer.data     = xmitBufferMemory;
	chabu->xmitBuffer.capacity = xmitBufferSize;
	chabu->xmitBuffer.limit    = 0;
	chabu->xmitBuffer.position = 0;
	chabu->xmitMaxPacketSize   = 0x100;

#ifdef Chabu_USE_LOCK
	Chabu_LOCK_CREATE(data->lock);
#endif

	chabu->xmitChannelIdx = 0;

	chabu->setupRx   = false;
	chabu->setupTx   = false;
	chabu->acceptRx  = false;
	chabu->acceptRx  = false;
	chabu->xmitAbort = false;

	chabu->userCallback = userCallback;
	chabu->userData     = userData;

	int i;
	
	for( i = 0; i < Chabu_CHANNEL_COUNT_MAX; i++ ){
		chabu->channels[i] = NULL;
	}

	for( i = 0; i < Chabu_PRIORITY_COUNT_MAX; i++ ){
		chabu->xmitRequestArmListHead[i] = NULL;
		chabu->xmitRequestArmListTail[i] = NULL;
		chabu->xmitRequestDataListHead[i] = NULL;
		chabu->xmitRequestDataListTail[i] = NULL;
	}
}

static void Chabu_Channel_QueueSupplied( void* ctx, struct QueueVar* queue, int amount ){
	UNUSED(queue);
	UNUSED(amount);
	struct Chabu_Channel_Data* channel = (struct Chabu_Channel_Data*)ctx;
	if( QueueVar_Available( channel->xmitQueue ) > 0 ){
		Chabu_Channel_XmitRequestData( channel );
	}
}
static void Chabu_Channel_QueueConsumed( void* ctx, struct QueueVar* queue, int amount ){
	UNUSED(queue);
	struct Chabu_Channel_Data* channel = (struct Chabu_Channel_Data*)ctx;
	channel->recvArm += amount;
	channel->recvRequest = true;

	// update the ARM -> send it
	Chabu_Channel_XmitRequestArm( channel );
}
void Chabu_Init_AddChannel (
		struct Chabu_Data* chabu,
		int channelId,
		struct Chabu_Channel_Data* channel,
		TChabu_ChannelUserCallback * userCallback, void * userData,
		int priority,
		struct QueueVar* recvQueue,
		struct QueueVar* xmitQueue ){

	Chabu_AssertPrintf( Chabu_ErrorCode_CONFIGURATION_CH_ID, channelId == chabu->channelCount, "channel id is not in sequence, exp=%d but is %d", chabu->channelCount, channelId );
	Chabu_AssertPrintf( Chabu_ErrorCode_CONFIGURATION_CH_ID, Chabu_CHANNEL_COUNT_MAX >= chabu->channelCount, "Too many channels, maximum count is %d", Chabu_CHANNEL_COUNT_MAX );
	Chabu_AssertPrintf( Chabu_ErrorCode_ILLEGAL_ARGUMENT, recvQueue != NULL, "recvQueue is NULL" );
	Chabu_AssertPrintf( Chabu_ErrorCode_ILLEGAL_ARGUMENT, xmitQueue != NULL, "recvQueue is NULL" );
	Chabu_AssertPrintf( Chabu_ErrorCode_CONFIGURATION_CH_PRIO, (priority >= 0) && (priority < chabu->priorityCount), "priority is not in range [0..%d], it is %d", chabu->priorityCount-1, priority );
	Chabu_AssertPrintf( Chabu_ErrorCode_CONFIGURATION_CH_USER, userCallback != NULL, "userCallback is NULL" );

	memset( channel, 0, sizeof(struct Chabu_Channel_Data));
	chabu->channels[ chabu->channelCount ] = channel;

	channel->chabu        = chabu;
	channel->channelId    = chabu->channelCount;
	channel->userCallback = userCallback;
	channel->userData     = userData;
	channel->priority     = priority;

	channel->xmitQueue      = xmitQueue;
	channel->xmitArm        = 0;
	channel->xmitSeq        = 0;

	channel->recvQueue      = recvQueue;
	channel->recvArm        = 0;
	channel->recvSeq        = 0;
	channel->recvRequest    = false;

	channel->recvArm        += QueueVar_Free( recvQueue );

	channel->xmitRequestArm      = false;
	channel->xmitRequestArmNext  = NULL;
	channel->xmitRequestData     = false;
	channel->xmitRequestDataNext = NULL;

	QueueVar_SetCallbackConsumed( recvQueue, &Chabu_Channel_QueueConsumed, channel );
	QueueVar_SetCallbackSupplied( xmitQueue, &Chabu_Channel_QueueSupplied, channel );
	chabu->channelCount++;
}

void Chabu_Init_Complete ( struct Chabu_Data* chabu ){
	int i;
	Chabu_AssertPrintf( Chabu_ErrorCode_CONFIGURATION_NO_CHANNELS, chabu->channelCount, "No channels added" );
	chabu->activated = true;
	for( i = 0; i < chabu->channelCount; i++ ){
		struct Chabu_Channel_Data* ch = chabu->channels[i];
		ch->userCallback( ch->userData, ch, Chabu_Channel_Event_Activated );
		Chabu_Channel_XmitRequestArm( ch );
	}
}

static struct Chabu_Channel_Data * calcNextXmitChannelData( struct Chabu_Data* chabu ) {
	int priority = chabu->channelCount-1;
	while( priority >= 0 ){
		if( chabu->xmitRequestDataListHead[priority] ){
			struct Chabu_Channel_Data * foundChannel = chabu->xmitRequestDataListHead[priority];
			struct Chabu_Channel_Data * next = foundChannel->xmitRequestDataNext;
			chabu->xmitRequestDataListHead[priority] = next;
			if( next == NULL ){
				chabu->xmitRequestDataListTail[priority] = NULL;
			}
			foundChannel->xmitRequestDataNext = NULL;
			foundChannel->xmitRequestData = 0;
			return foundChannel;
		}
		priority--;
	}
	return NULL;
}

static struct Chabu_Channel_Data * calcNextXmitChannelArm( struct Chabu_Data* chabu ) {
	int priority = chabu->channelCount-1;
	while( priority >= 0 ){
		if( chabu->xmitRequestArmListHead[priority] ){
			struct Chabu_Channel_Data * foundChannel = chabu->xmitRequestArmListHead[priority];
			struct Chabu_Channel_Data * next = foundChannel->xmitRequestArmNext;
			chabu->xmitRequestArmListHead[priority] = next;
			if( next == NULL ){
				chabu->xmitRequestArmListTail[priority] = NULL;
			}
			foundChannel->xmitRequestArmNext = NULL;
			foundChannel->xmitRequestArm = 0;
			return foundChannel;
		}
		priority--;
	}
	return NULL;
}


/**
 * Try to copy as much as possible into the buffer.
 * The amount of bytes copied is returned.
 */
static int copyMemoryToBuffer( struct ByteBuffer_Data* trgBuf, void* srcData, int srcLength ){
	int cpySz = trgBuf->limit - trgBuf->position;
	if( cpySz > srcLength){
		cpySz = srcLength;
	}
	memcpy( trgBuf->data + trgBuf->position, srcData, cpySz );
	trgBuf->position += cpySz;
	return cpySz;
}
static int copyBufferToBuffer( struct ByteBuffer_Data* trgBuf, struct ByteBuffer_Data* srcBuf ){
	int cpySzTrg = trgBuf->limit - trgBuf->position;
	int cpySzSrc = srcBuf->limit - srcBuf->position;
	int cpySz = ( cpySzTrg < cpySzSrc ) ? cpySzTrg : cpySzSrc;
	memcpy( trgBuf->data + trgBuf->position, srcBuf->data + srcBuf->position, cpySz );
	trgBuf->position += cpySz;
	srcBuf->position += cpySz;
	return cpySz;
}


static void handleRecvSetupProblem( struct Chabu_Data* chabu, enum Chabu_Event ev ){

}
static void handleRecvSetup( struct Chabu_Data* chabu ){
	if( !chabu->setupRx ){
		chabu->setupRx = true;

		// PS
		// PT
		uint32 idx = 8;

		// PN "CHABU"
		uint32 pnLen = UINT32_GET_UNALIGNED_HTON( chabu->recvBuffer.data + idx );
		idx += 4;
		char*  pnStr = (char*)(chabu->recvBuffer.data + idx);
		idx = align4( idx + pnLen);

		if( pnLen != 5 ) {
			handleRecvSetupProblem( chabu, Chabu_Event_Error_RxSetup_PN );
			return;
		}
		if( strncmp( "CHABU", pnStr, 5 ) != 0 ){
			handleRecvSetupProblem( chabu, Chabu_Event_Error_RxSetup_PN );
			return;
		}

		// PV protocol version
		uint32 pv = UINT32_GET_UNALIGNED_HTON( chabu->recvBuffer.data + idx );
		idx += 4;

		if( UINT32_HI(pv) != UINT32_HI(Chabu_PROTCOL_VERSION) ) {
			handleRecvSetupProblem( chabu, Chabu_Event_Error_RxSetup_PV );
			return;
		}

		// RS receive buffer size
		uint32 rs = UINT32_GET_UNALIGNED_HTON( chabu->recvBuffer.data + idx );
		idx += 4;
		if( rs < CHABU_MINIMUM_PACKET_SIZE ) {
			handleRecvSetupProblem( chabu, Chabu_Event_Error_RxSetup_RS );
			return;
		}

		// AV application version
		uint32 av = UINT32_GET_UNALIGNED_HTON( chabu->recvBuffer.data + idx );
		idx += 4;

		// AN application name
		uint32 anLen = UINT32_GET_UNALIGNED_HTON( chabu->recvBuffer.data + idx );
		idx += 4;
		char*  anStr = (char*)(chabu->recvBuffer.data + idx);
		idx = align4( idx + pnLen);

		if( anLen < CHABU_MINIMUM_PACKET_SIZE ) {
			handleRecvSetupProblem( chabu, Chabu_Event_Error_RxSetup_AN_Length );
			return;
		}
		if( anLen > Chabu_APPLICATION_NAME_SIZE_MAX ){
			anLen = Chabu_APPLICATION_NAME_SIZE_MAX;
		}
		memcpy( chabu->connectionInfoRemote.applicationName, anStr, anLen );
		chabu->connectionInfoRemote.applicationName[anLen] = 0;
		chabu->connectionInfoRemote.applicationNameLength = (uint8)anLen;

		chabu->connectionInfoRemote.applicationVersion = av;

		chabu->userCallback( chabu->userData, chabu, Chabu_Event_Connecting );

		CHABU_DBGPRINTF( "Chabu_PutRecvData recv protocol spec %d bytes consumed, startupRx:%d startupTx:%d", (idx - idx2), chabu->startupRx, chabu->startupTx );
		return;
	}


}
static void handleRecvAccept( struct Chabu_Data* chabu ){
	chabu->acceptRx = true;
}
static void handleRecvAbort( struct Chabu_Data* chabu ){

	uint32 idx = 8;

	// PV protocol version
	uint32 code = UINT32_GET_UNALIGNED_HTON( chabu->recvBuffer.data + idx );
	idx += 4;

	// PN "CHABU"
	uint32 msgLen = UINT32_GET_UNALIGNED_HTON( chabu->recvBuffer.data + idx );
	idx += 4;
	char*  msgStr = (char*)(chabu->recvBuffer.data + idx);
	idx = align4( idx + msgLen);

	// write into buffer to terminate the string
	msgStr[msgLen] = 0;

	chabu->lastErrorCode   = code;
	chabu->lastErrorString = msgStr;

	chabu->userCallback( chabu->userData, chabu, Chabu_Event_Error_AbortRx );

}
static void handleRecvArm( struct Chabu_Data* chabu ){

	Chabu_Assert( Chabu_ErrorCode_ASSERT, chabu->recvBuffer.position == 16 );

	int channelId  = UINT32_GET_UNALIGNED_HTON( chabu->recvBuffer.data + CHABU_ARMPACKET_OFFSET_CHANNEL );
	uint32 arm        = UINT32_GET_UNALIGNED_HTON( chabu->recvBuffer.data + CHABU_ARMPACKET_OFFSET_ARM     );

	Chabu_Assert( Chabu_ErrorCode_ASSERT, channelId < chabu->channelCount );
	struct Chabu_Channel_Data* channel = chabu->channels[channelId];

	if( channel->xmitArm != arm ){
		channel->xmitArm = arm;
		Chabu_Channel_XmitRequestData( channel );
	}

}
static void handleRecvSeq( struct Chabu_Data* chabu ){

	Chabu_Assert( Chabu_ErrorCode_ASSERT, chabu->recvBuffer.position >= 20 );

	int    channelId = UINT32_GET_UNALIGNED_HTON( chabu->recvBuffer.data + CHABU_SEQPACKET_OFFSET_CHANNEL  );
	uint32 seq       = UINT32_GET_UNALIGNED_HTON( chabu->recvBuffer.data + CHABU_SEQPACKET_OFFSET_SEQ      );
	int    dataSize  = UINT32_GET_UNALIGNED_HTON( chabu->recvBuffer.data + CHABU_SEQPACKET_OFFSET_DATASIZE );

	Chabu_Assert( Chabu_ErrorCode_ASSERT, chabu->recvBuffer.position >= 20 + dataSize );
	Chabu_Assert( Chabu_ErrorCode_ASSERT, channelId < chabu->channelCount );

	struct Chabu_Channel_Data* channel = chabu->channels[channelId];

	if( channel->recvSeq != seq ){
		CHABU_DBGPRINTF("Chabu_Channel_handleRecv SEQ not matching %d != %d(expected)", channel->recvSeq, seq);
	}

	//Chabu_Assert( channel->recvSeq == seq );
	channel->recvSeq += dataSize;

	// Queue must have enough space,
	// because only armed payload is transmitted, this is tested in QueueVar_Write
	QueueVar_Write( channel->recvQueue, chabu->recvBuffer.data + 20, dataSize );
	channel->userCallback( channel->userData, channel, Chabu_Channel_Event_DataAvailable );
}

static void handleUnknownPacket( struct Chabu_Data* chabu, uint32 pt ){

	chabu->userCallback( chabu->userData, chabu, Chabu_Event_Error_Protocol_UnknownRx );

}

void Chabu_PutRecvData ( struct Chabu_Data* chabu, struct ByteBuffer_Data* recvData ){

	Chabu_LOCK_DO_LOCK(chabu->lock);

	Chabu_Assert( Chabu_ErrorCode_NOT_ACTIVATED, chabu->activated );
	int idx = 0;

	while( ByteBuffer_hasRemaining(recvData) ){

		// try to fill recvBuffer
		ByteBuffer_xferAllPossible( &chabu->recvBuffer, recvData );

		// if this was PS, prepare full recv and again.
		if( chabu->recvBuffer.position == CHABU_MINIMUM_PACKET_SIZE && chabu->recvBuffer.limit == CHABU_MINIMUM_PACKET_SIZE){
			int ps = UINT32_GET_UNALIGNED_HTON(chabu->recvBuffer.data);

			Chabu_Assert( Chabu_ErrorCode_ASSERT, ps >= CHABU_MINIMUM_PACKET_SIZE && ps <= chabu->recvBuffer.capacity );

			if( chabu->recvBuffer.limit < ps ){
				chabu->recvBuffer.limit = ps;
				continue;
			}
		}

		Chabu_Assert( Chabu_ErrorCode_ASSERT, chabu->recvBuffer.limit >= CHABU_MINIMUM_PACKET_SIZE );

		if( chabu->recvBuffer.position < chabu->recvBuffer.limit ){
			break;
		}

		// now the recv buffer contains a full packet.

		// process packet
		uint32 pt = UINT32_GET_UNALIGNED_HTON(chabu->recvBuffer.data+4);
		switch( pt & 0x00000FF ){
		case CHABU_PACKETTYPE_SETUP : handleRecvSetup (chabu); break;
		case CHABU_PACKETTYPE_ACCEPT: handleRecvAccept(chabu); break;
		case CHABU_PACKETTYPE_ABORT : handleRecvAbort (chabu); break;
		case CHABU_PACKETTYPE_ARM   : handleRecvArm   (chabu); break;
		case CHABU_PACKETTYPE_SEQ   : handleRecvSeq   (chabu); break;
		default              : handleUnknownPacket(chabu, pt); break;
		}

		// prepare for recv new packet
		chabu->recvBuffer.position = 0;
		chabu->recvBuffer.limit    = CHABU_MINIMUM_PACKET_SIZE;


	}
	Chabu_Assert( Chabu_ErrorCode_ASSERT, !ByteBuffer_hasRemaining(recvData)  );
	Chabu_LOCK_DO_UNLOCK(chabu->lock);

}


static void xmitSetup( struct Chabu_Data* chabu ){

	uint32 trgIdx, srcIdx;

	// reserve 4 bytes for length
	trgIdx = 4;

	// packet type
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data +  trgIdx, CHABU_PACKETTYPE_MAGIC | CHABU_PACKETTYPE_SETUP );
	trgIdx += 4;

	// chabu protocol name
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data +  trgIdx, 5 );
	trgIdx += 4;
	chabu->xmitBuffer.data[12] = 'C';
	chabu->xmitBuffer.data[13] = 'H';
	chabu->xmitBuffer.data[14] = 'A';
	chabu->xmitBuffer.data[15] = 'B';
	chabu->xmitBuffer.data[16] = 'U';
	chabu->xmitBuffer.data[17] = 0x00;
	chabu->xmitBuffer.data[18] = 0x00;
	chabu->xmitBuffer.data[19] = 0x00;
	trgIdx += 8;

	// chabu protocol version
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + trgIdx, Chabu_PROTCOL_VERSION );
	trgIdx += 4;

	// receive buffer size
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + trgIdx, chabu->recvBuffer.capacity );
	trgIdx += 4;

	// application version
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + trgIdx, chabu->connectionInfoLocal.applicationVersion );
	trgIdx += 4;

	// Application name
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + trgIdx, chabu->connectionInfoLocal.applicationNameLength );
	trgIdx += 4;

	srcIdx =  0;
	while( srcIdx < chabu->connectionInfoLocal.applicationNameLength ){
		chabu->xmitBuffer.data[trgIdx++] = chabu->connectionInfoLocal.applicationName[ srcIdx++ ];
	}
	while(( trgIdx & 0x03 ) != 0 ){
		chabu->xmitBuffer.data[trgIdx++] = 0;
	}

	// now write the length at the beginning
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + 0, trgIdx );

	chabu->xmitBuffer.limit = trgIdx;
	chabu->xmitBuffer.position = 0;
}

static void xmitAccept( struct Chabu_Data* chabu ){
	// packet size
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + 0, 8 );
	// packet type
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + 4, CHABU_PACKETTYPE_MAGIC | CHABU_PACKETTYPE_ACCEPT );

	chabu->xmitBuffer.limit = 8;
	chabu->xmitBuffer.position = 0;
}

static void xmitChannelArm( struct Chabu_Data* chabu, struct Chabu_Channel_Data* channel ){
	// packet size
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_PACKET_OFFSET_PACKETSIZE, 16 );
	// packet type
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_PACKET_OFFSET_PACKETTYPE, CHABU_PACKETTYPE_MAGIC | CHABU_PACKETTYPE_ARM );
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_ARMPACKET_OFFSET_CHANNEL, channel->channelId );
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_ARMPACKET_OFFSET_ARM, channel->recvArm );
	chabu->xmitBuffer.position =  0;
	chabu->xmitBuffer.limit    = 16;
}

static void handleChannelSeq( struct Chabu_Data* chabu, struct Chabu_Channel_Data* channel ){

	// pls = payload size
	int pls = QueueVar_Available( channel->xmitQueue );
	if( pls == 0 ){
		return;
	}

	if( pls > chabu->xmitMaxPacketSize - CHABU_SEQPACKET_OFFSET_DATADATA ){
		pls = chabu->xmitMaxPacketSize - CHABU_SEQPACKET_OFFSET_DATADATA;
	}
	int remainArm = channel->xmitArm - channel->xmitSeq;
	if( pls > remainArm ){
		pls = remainArm;
	}


	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_PACKET_OFFSET_PACKETTYPE, CHABU_PACKETTYPE_MAGIC | CHABU_PACKETTYPE_SEQ );
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_SEQPACKET_OFFSET_CHANNEL, channel->channelId );
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_SEQPACKET_OFFSET_SEQ, channel->xmitSeq );
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_SEQPACKET_OFFSET_DATASIZE, pls );
	QueueVar_Read( channel->xmitQueue, chabu->xmitBuffer.data + 20, pls );

	channel->xmitSeq += pls;

	while( (pls&3) != 0 ){
		chabu->xmitBuffer.data[ 20 + pls ] = 0;
		pls++;
	}

	int ps = CHABU_SEQPACKET_OFFSET_DATADATA + pls;
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_PACKET_OFFSET_PACKETSIZE, ps );

	chabu->xmitBuffer.position =  0;
	chabu->xmitBuffer.limit    = ps;

}

static void getXmitData(struct Chabu_Data* chabu ){

	if( !chabu->setupTx ){
		chabu->setupTx = true;
		xmitSetup( chabu );
		CHABU_DBGPRINTF("Chabu_GetXmitData startup %d byte supplied, startupRx:%d startupTx:%d", idx - idx2, chabu->startupRx, chabu->startupTx );
		return;
	}
	if( !chabu->setupRx ){
		return;
	}
	if( !chabu->acceptTx ){
		chabu->acceptTx = true;
		xmitAccept( chabu );
		return;
	}

	{
		struct Chabu_Channel_Data* channel = calcNextXmitChannelArm( chabu );
		if( channel != NULL ){
			xmitChannelArm( chabu, channel );
			Chabu_Assert( Chabu_ErrorCode_ASSERT, chabu->xmitBuffer.limit > 0 );
			return;
		}
	}
	{
		struct Chabu_Channel_Data* channel;
		while( (channel = calcNextXmitChannelData( chabu )) != NULL ){

			channel->userCallback( channel->userData, channel, Chabu_Channel_Event_PreTransmit );
			if( QueueVar_Available(channel->xmitQueue) > 0 ){
				handleChannelSeq( chabu, channel );

				CHABU_DBGPRINTF("Chabu_GetXmitData ch[%d] %d byte supplied", channel->channelId, idx - idx2);
				return;
			}
		}
	}

}
/**
 * @return true if the data shall be flushed
 */
bool Chabu_GetXmitData ( struct Chabu_Data* chabu, struct ByteBuffer_Data* xmitData ){

	Chabu_LOCK_DO_LOCK(chabu->lock);

	bool flush = true;
	int loopCheck = 0;

	while( true ){

		loopCheck++;
		Chabu_Assert0( Chabu_ErrorCode_ASSERT, loopCheck < 100 );

		// if xmit buffer is empty, try to fill it
		if( chabu->xmitBuffer.limit == 0 ){

			getXmitData(chabu);

			// if xmit buffer is still empty, go out
			if( chabu->xmitBuffer.limit == 0 ){
				break;
			}
		}

		// copy as much as possible to target
		copyBufferToBuffer( xmitData, &chabu->xmitBuffer);

		// if all is transferred to target, prepare for new data
		if( chabu->xmitBuffer.position == chabu->xmitBuffer.limit ){
			chabu->xmitBuffer.position = 0;
			chabu->xmitBuffer.limit    = 0;
		}

		// if target is full, go out
		if( xmitData->position == xmitData->limit ){
			break;
		}

	}

	Chabu_LOCK_DO_UNLOCK(chabu->lock);
	return flush;
}



void Chabu_Channel_evUserRecvRequest( struct Chabu_Channel_Data* channel ){
	struct Chabu_Data* chabu = channel->chabu;
	UNUSED(chabu);
	Chabu_LOCK_DO_LOCK(chabu->lock);
	channel->recvRequest = true;
	Chabu_LOCK_DO_UNLOCK(chabu->lock);
}

static void Chabu_Channel_XmitRequestArm( struct Chabu_Channel_Data* channel ){
	struct Chabu_Data* chabu = channel->chabu;
	int prio = channel->priority;
	UNUSED(chabu);
	Chabu_LOCK_DO_LOCK(chabu->lock);

	if( !channel->xmitRequestArm ){
		channel->xmitRequestArm = true;

		if( chabu->xmitRequestArmListHead[prio] == NULL ){
			chabu->xmitRequestArmListHead[prio] = channel;
		}
		if( chabu->xmitRequestArmListTail[prio] != NULL ){
			struct Chabu_Channel_Data* prev = chabu->xmitRequestArmListTail[prio];
			prev->xmitRequestArmNext = channel;
		}
		chabu->xmitRequestArmListTail[prio] = channel;
	}



	Chabu_LOCK_DO_UNLOCK(chabu->lock);
}
void Chabu_Channel_XmitRequestData( struct Chabu_Channel_Data* channel ){
	struct Chabu_Data* chabu = channel->chabu;
	int prio = channel->priority;
	UNUSED(chabu);
	Chabu_LOCK_DO_LOCK(chabu->lock);

	if( !channel->xmitRequestData ){
		channel->xmitRequestData = true;

		if( chabu->xmitRequestDataListHead[prio] == NULL ){
			chabu->xmitRequestDataListHead[prio] = channel;
		}
		if( chabu->xmitRequestDataListTail[prio] != NULL ){
			struct Chabu_Channel_Data* prev = chabu->xmitRequestDataListTail[prio];
			prev->xmitRequestDataNext = channel;
		}
		chabu->xmitRequestDataListTail[prio] = channel;
	}

	Chabu_LOCK_DO_UNLOCK(chabu->lock);
}

struct Chabu_Channel_Data* Chabu_GetChannel (struct Chabu_Data* chabu, int channelId ) {
	return chabu->channels[channelId];
}

void* Chabu_Channel_GetUserData ( struct Chabu_Channel_Data* channel ){
	return channel->userData;
}

