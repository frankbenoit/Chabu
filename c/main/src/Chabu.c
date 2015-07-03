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



void Chabu_Init(
		struct Chabu_Data* data,

		int     applicationVersion,
		char*   applicationName,

		uint8*  recvBufferMemory,
		int recvBufferSize,

		uint8*  xmitBufferMemory,
		int xmitBufferSize,

		TChabu_UserCallback * userCallback,
		void * userData ){

	Chabu_AssertPrintf( (((int)recvBufferMemory) & 0x3) == 0, "recvBufferMemory is not 4-byte aligned" );
	Chabu_AssertPrintf( (((int)xmitBufferMemory) & 0x3) == 0, "xmitBufferMemory is not 4-byte aligned" );
	Chabu_AssertPrintf( recvBufferMemory != NULL, "recvBufferMemory is NULL" );
	Chabu_AssertPrintf( recvBufferSize < 0x100, "recvBufferSize shall be bigger than 0x100, but is %d", recvBufferSize );
	Chabu_AssertPrintf( xmitBufferMemory != NULL, "xmitBufferMemory is NULL" );
	Chabu_AssertPrintf( xmitBufferSize < 0x100, "xmitBufferSize shall be bigger than 0x100, but is %d", xmitBufferSize );

	data->channelCount = 0;
	data->activated = false;

	data->connectionInfoLocal.receiveBufferSize = recvBufferSize;
	data->connectionInfoLocal.applicationVersion    = applicationVersion;
	data->connectionInfoLocal.applicationNameLength = Chabu_strnlen(applicationName, Chabu_APPLICATION_NAME_SIZE_MAX);
	strncpy( data->connectionInfoLocal.applicationName, applicationName, Chabu_APPLICATION_NAME_SIZE_MAX );

	memset( data->connectionInfoRemote.applicationName, 0, Chabu_APPLICATION_NAME_SIZE_MAX );

	data->recvBuffer.data     = recvBufferMemory;
	data->recvBuffer.capacity = recvBufferSize;
	data->recvBuffer.limit    = CHABU_MINIMUM_PACKET_SIZE;
	data->recvBuffer.position = 0;

	data->xmitBuffer.data     = xmitBufferMemory;
	data->xmitBuffer.capacity = xmitBufferSize;
	data->xmitBuffer.limit    = 0;
	data->xmitBuffer.position = 0;
	data->xmitMaxPacketSize   = 0x100;

#ifdef Chabu_USE_LOCK
	Chabu_LOCK_CREATE(data->lock);
#endif

	data->xmitChannelIdx = 0;

	data->setupRx   = false;
	data->setupTx   = false;
	data->acceptRx  = false;
	data->acceptRx  = false;
	data->xmitAbort = false;

	data->userCallback = userCallback;
	data->userData     = userData;

	int i;
	for( i = 0; i < Chabu_CHANNEL_COUNT_MAX; i++ ){
		data->channels[i] = NULL;
	}
}

static void Chabu_Channel_QueueSupplied( void* ctx, struct QueueVar* queue, int amount ){
	UNUSED(queue);
	UNUSED(amount);
	struct Chabu_Channel_Data* channel = (struct Chabu_Channel_Data*)ctx;
	channel->xmitRequest = true;
}
static void Chabu_Channel_QueueConsumed( void* ctx, struct QueueVar* queue, int amount ){
	UNUSED(queue);
	struct Chabu_Channel_Data* channel = (struct Chabu_Channel_Data*)ctx;
	channel->recvArm += amount;
	channel->recvRequest = true;

	// update the ARM -> send it
	channel->recvArmShouldBeXmit = true;
	channel->xmitRequest         = true;
}
void Chabu_Init_AddChannel (
		struct Chabu_Data* data,
		struct Chabu_Channel_Data* channel,
		TChabu_ChannelUserCallback * userCallback, void * userData,
		int priority,
		struct QueueVar* recvQueue,
		struct QueueVar* xmitQueue ){

	data->channels[ data->channelCount ] = channel;

	channel->chabu        = data;
	channel->channelId    = data->channelCount;
	channel->userCallback = userCallback;
	channel->userData     = userData;
	channel->priority     = priority;

	channel->xmitQueue      = xmitQueue;
	channel->xmitArm        = 0;
	channel->xmitSeq        = 0;
	channel->xmitRequest    = true;

	channel->recvArmShouldBeXmit = true;

	channel->recvQueue      = recvQueue;
	channel->recvArm        = 0;
	channel->recvSeq        = 0;
	channel->recvRequest    = false;

	channel->recvArm        += QueueVar_Free( recvQueue );

	QueueVar_SetCallbackConsumed( recvQueue, &Chabu_Channel_QueueConsumed, channel );
	QueueVar_SetCallbackSupplied( xmitQueue, &Chabu_Channel_QueueSupplied, channel );

	data->channelCount++;
}

void Chabu_Init_Complete ( struct Chabu_Data* data ){
	data->activated = true;
}

static struct Chabu_Channel_Data * calcNextXmitChannel( struct Chabu_Data* data ) {
	int priority = INT32_MIN;
	struct Chabu_Channel_Data * foundChannel = NULL;
	int chIdx = ( data->xmitChannelIdx +1 );
	if( chIdx >= data->channelCount ){
		chIdx = 0;
	}
	int i;
	for( i = 0; i < data->channelCount; i++ ){
		struct Chabu_Channel_Data * channel = data->channels[chIdx];
		Chabu_Assert( channel != NULL );
		if( channel->xmitRequest && priority <= channel->priority ){
			foundChannel = channel;
			priority = channel->priority;
		}
		chIdx++;
		if( chIdx >= data->channelCount ){
			chIdx = 0;
		}
	}

	if( foundChannel ){
		foundChannel->xmitRequest = false;
		data->xmitChannelIdx = foundChannel->channelId;
		return foundChannel;
	}

	return NULL;
}


/**
 * Try to copy as much as possible into the buffer.
 * The amount of bytes copied is returned.
 */
static int copyMemoryToBuffer( struct Chabu_Buffer* trgBuf, void* srcData, int srcLength ){
	int cpySz = trgBuf->limit - trgBuf->position;
	if( cpySz > srcLength){
		cpySz = srcLength;
	}
	memcpy( trgBuf->data + trgBuf->position, srcData, cpySz );
	trgBuf->position += cpySz;
	return cpySz;
}
static int copyBufferToBuffer( struct Chabu_Buffer* trgBuf, struct Chabu_Buffer* srcBuf ){
	int cpySzTrg = trgBuf->limit - trgBuf->position;
	int cpySzSrc = srcBuf->limit - srcBuf->position;
	int cpySz = ( cpySzTrg < cpySzSrc ) ? cpySzTrg : cpySzSrc;
	memcpy( trgBuf->data + trgBuf->position, srcBuf->data + srcBuf->position, cpySz );
	trgBuf->position += cpySz;
	srcBuf->position += cpySz;
	return cpySz;
}


static void handleRecvSetupProblem( struct Chabu_Data* data, enum Chabu_Event ev ){

}
static void handleRecvSetup( struct Chabu_Data* data ){
	if( !data->setupRx ){
		data->setupRx = true;

		// PS
		// PT
		uint32 idx = 8;

		// PN "CHABU"
		uint32 pnLen = UINT32_GET_UNALIGNED_HTON( data->recvBuffer.data + idx );
		idx += 4;
		char*  pnStr = (char*)(data->recvBuffer.data + idx);
		idx = align4( idx + pnLen);

		if( pnLen != 5 ) {
			handleRecvSetupProblem( data, Chabu_Event_Error_RxSetup_PN );
			return;
		}
		if( strncmp( "CHABU", pnStr, 5 ) != 0 ){
			handleRecvSetupProblem( data, Chabu_Event_Error_RxSetup_PN );
			return;
		}

		// PV protocol version
		uint32 pv = UINT32_GET_UNALIGNED_HTON( data->recvBuffer.data + idx );
		idx += 4;

		if( UINT32_HI(pv) != UINT32_HI(Chabu_PROTCOL_VERSION) ) {
			handleRecvSetupProblem( data, Chabu_Event_Error_RxSetup_PV );
			return;
		}

		// RS receive buffer size
		uint32 rs = UINT32_GET_UNALIGNED_HTON( data->recvBuffer.data + idx );
		idx += 4;
		if( rs < CHABU_MINIMUM_PACKET_SIZE ) {
			handleRecvSetupProblem( data, Chabu_Event_Error_RxSetup_RS );
			return;
		}

		// AV application version
		uint32 av = UINT32_GET_UNALIGNED_HTON( data->recvBuffer.data + idx );
		idx += 4;

		// AN application name
		uint32 anLen = UINT32_GET_UNALIGNED_HTON( data->recvBuffer.data + idx );
		idx += 4;
		char*  anStr = (char*)(data->recvBuffer.data + idx);
		idx = align4( idx + pnLen);

		if( anLen < CHABU_MINIMUM_PACKET_SIZE ) {
			handleRecvSetupProblem( data, Chabu_Event_Error_RxSetup_AN_Length );
			return;
		}
		if( anLen > Chabu_APPLICATION_NAME_SIZE_MAX ){
			anLen = Chabu_APPLICATION_NAME_SIZE_MAX;
		}
		memcpy( data->connectionInfoRemote.applicationName, anStr, anLen );
		data->connectionInfoRemote.applicationName[anLen] = 0;
		data->connectionInfoRemote.applicationNameLength = (uint8)anLen;

		data->connectionInfoRemote.applicationVersion = av;

		data->userCallback( data->userData, data, Chabu_Event_Connecting );

		CHABU_DBGPRINTF( "Chabu_PutRecvData recv protocol spec %d bytes consumed, startupRx:%d startupTx:%d", (idx - idx2), data->startupRx, data->startupTx );
		return;
	}


}
static void handleRecvAccept( struct Chabu_Data* data ){
	data->acceptRx = true;
}
static void handleRecvAbort( struct Chabu_Data* data ){

	uint32 idx = 8;

	// PV protocol version
	uint32 code = UINT32_GET_UNALIGNED_HTON( data->recvBuffer.data + idx );
	idx += 4;

	// PN "CHABU"
	uint32 msgLen = UINT32_GET_UNALIGNED_HTON( data->recvBuffer.data + idx );
	idx += 4;
	char*  msgStr = (char*)(data->recvBuffer.data + idx);
	idx = align4( idx + msgLen);

	// write into buffer to terminate the string
	msgStr[msgLen] = 0;

	data->lastErrorCode   = code;
	data->lastErrorString = msgStr;

	data->userCallback( data->userData, data, Chabu_Event_Error_AbortRx );

}
static void handleRecvArm( struct Chabu_Data* data ){

	Chabu_Assert( data->recvBuffer.position == 16 );

	int channelId  = UINT32_GET_UNALIGNED_HTON( data->recvBuffer.data + CHABU_ARMPACKET_OFFSET_CHANNEL );
	uint32 arm        = UINT32_GET_UNALIGNED_HTON( data->recvBuffer.data + CHABU_ARMPACKET_OFFSET_ARM     );

	Chabu_Assert( channelId < data->channelCount );
	struct Chabu_Channel_Data* channel = data->channels[channelId];

	if( channel->xmitArm != arm ){
		channel->xmitArm = arm;
		Chabu_Channel_evUserXmitRequest( channel );
	}

}
static void handleRecvSeq( struct Chabu_Data* data ){

	Chabu_Assert( data->recvBuffer.position >= 20 );

	int    channelId = UINT32_GET_UNALIGNED_HTON( data->recvBuffer.data + CHABU_SEQPACKET_OFFSET_CHANNEL  );
	uint32 seq       = UINT32_GET_UNALIGNED_HTON( data->recvBuffer.data + CHABU_SEQPACKET_OFFSET_SEQ      );
	int    dataSize  = UINT32_GET_UNALIGNED_HTON( data->recvBuffer.data + CHABU_SEQPACKET_OFFSET_DATASIZE );

	Chabu_Assert( data->recvBuffer.position >= 20 + dataSize );
	Chabu_Assert( channelId < data->channelCount );

	struct Chabu_Channel_Data* channel = data->channels[channelId];

	if( channel->recvSeq != seq ){
		CHABU_DBGPRINTF("Chabu_Channel_handleRecv SEQ not matching %d != %d(expected)", channel->recvSeq, seq);
	}

	//Chabu_Assert( channel->recvSeq == seq );
	channel->recvSeq += dataSize;

	// Queue must have enough space,
	// because only armed payload is transmitted, this is tested in QueueVar_Write
	QueueVar_Write( channel->recvQueue, data->recvBuffer.data + 20, dataSize );
}

static void handleUnknownPacket( struct Chabu_Data* data, uint32 pt ){

	data->userCallback( data->userData, data, Chabu_Event_Error_Protocol_UnknownRx );

}

void Chabu_PutRecvData ( struct Chabu_Data* data, void* recvData, int length ){

	Chabu_LOCK_DO_LOCK(data->lock);

	Chabu_Assert( data->activated );
	int idx = 0;

	while( idx < length ){

		// try to fill recvBuffer
		idx += copyMemoryToBuffer( &data->recvBuffer, ((char*)recvData) + idx, length - idx );

		// if this was PS, prepare full recv and again.
		if( data->recvBuffer.position == CHABU_MINIMUM_PACKET_SIZE && data->recvBuffer.limit == CHABU_MINIMUM_PACKET_SIZE){
			int ps = UINT32_GET_UNALIGNED_HTON(data->recvBuffer.data);

			Chabu_Assert( ps >= CHABU_MINIMUM_PACKET_SIZE && ps <= data->recvBuffer.capacity );

			if( data->recvBuffer.limit < ps ){
				data->recvBuffer.limit = ps;
				continue;
			}
		}

		Chabu_Assert( data->recvBuffer.limit >= CHABU_MINIMUM_PACKET_SIZE );

		if( data->recvBuffer.position < data->recvBuffer.limit ){
			break;
		}

		// now the recv buffer contains a full packet.

		// process packet
		uint32 pt = UINT32_GET_UNALIGNED_HTON(data->recvBuffer.data+4);
		switch( pt & 0x00000FF ){
		case CHABU_PACKETTYPE_SETUP : handleRecvSetup (data); break;
		case CHABU_PACKETTYPE_ACCEPT: handleRecvAccept(data); break;
		case CHABU_PACKETTYPE_ABORT : handleRecvAbort (data); break;
		case CHABU_PACKETTYPE_ARM   : handleRecvArm   (data); break;
		case CHABU_PACKETTYPE_SEQ   : handleRecvSeq   (data); break;
		default              : handleUnknownPacket(data, pt); break;
		}

		// prepare for recv new packet
		data->recvBuffer.position = 0;
		data->recvBuffer.limit    = CHABU_MINIMUM_PACKET_SIZE;


	}
	Chabu_Assert( idx == length );
	Chabu_LOCK_DO_UNLOCK(data->lock);

}


static void xmitSetup( struct Chabu_Data* data ){

	uint32 trgIdx, srcIdx;

	// packet type
	UINT32_PUT_UNALIGNED_HTON( data->xmitBuffer.data +  4, CHABU_PACKETTYPE_MAGIC | CHABU_PACKETTYPE_SETUP );

	// protocol name
	UINT32_PUT_UNALIGNED_HTON( data->xmitBuffer.data +  8, 5 );
	data->xmitBuffer.data[12] = 'C';
	data->xmitBuffer.data[13] = 'H';
	data->xmitBuffer.data[14] = 'A';
	data->xmitBuffer.data[15] = 'B';
	data->xmitBuffer.data[16] = 'U';
	data->xmitBuffer.data[17] = 0x00;
	data->xmitBuffer.data[18] = 0x00;
	data->xmitBuffer.data[19] = 0x00;
	UINT32_PUT_UNALIGNED_HTON( data->xmitBuffer.data + 20, Chabu_PROTCOL_VERSION );
	UINT32_PUT_UNALIGNED_HTON( data->xmitBuffer.data + 24, data->recvBuffer.capacity );
	UINT32_PUT_UNALIGNED_HTON( data->xmitBuffer.data + 28, data->connectionInfoLocal.applicationNameLength );

	trgIdx = 28;
	srcIdx =  0;
	while( srcIdx < data->connectionInfoLocal.applicationNameLength ){
		data->xmitBuffer.data[trgIdx++] = data->connectionInfoLocal.applicationName[ srcIdx++ ];
	}
	while(( trgIdx & 0x03 ) != 0 ){
		data->xmitBuffer.data[trgIdx++] = 0;
	}
	UINT32_PUT_UNALIGNED_HTON( data->xmitBuffer.data + 0, trgIdx );

	data->xmitBuffer.limit = trgIdx;
	data->xmitBuffer.position = 0;
}

static void xmitAccept( struct Chabu_Data* data ){
	// packet size
	UINT32_PUT_UNALIGNED_HTON( data->xmitBuffer.data + 0, 8 );
	// packet type
	UINT32_PUT_UNALIGNED_HTON( data->xmitBuffer.data + 4, CHABU_PACKETTYPE_MAGIC | CHABU_PACKETTYPE_ACCEPT );
}

static void xmitChannelArm( struct Chabu_Data* chabu, struct Chabu_Channel_Data* channel ){
	// packet size
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_PACKET_OFFSET_PACKETSIZE, 16 );
	// packet type
	UINT16_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_PACKET_OFFSET_PACKETTYPE, CHABU_PACKETTYPE_MAGIC | CHABU_PACKETTYPE_ARM );
	UINT16_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_ARMPACKET_OFFSET_CHANNEL, channel->channelId );
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_ARMPACKET_OFFSET_ARM, channel->recvArm );
	chabu->xmitBuffer.position =  0;
	chabu->xmitBuffer.limit    = 16;
}

static void handleChannelSeq( struct Chabu_Data* chabu, struct Chabu_Channel_Data* channel ){

	// pls = payload size
	int pls = QueueVar_Available( channel->xmitQueue );

	if( pls > chabu->xmitMaxPacketSize - CHABU_SEQPACKET_OFFSET_DATADATA ){
		pls = chabu->xmitMaxPacketSize - CHABU_SEQPACKET_OFFSET_DATADATA;
	}
	int remainArm = channel->xmitArm - channel->xmitSeq;
	if( pls > remainArm ){
		pls = remainArm;
	}

	int ps = CHABU_SEQPACKET_OFFSET_DATADATA + pls;

	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_PACKET_OFFSET_PACKETSIZE, ps );
	UINT16_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_PACKET_OFFSET_PACKETTYPE, CHABU_PACKETTYPE_MAGIC | CHABU_PACKETTYPE_SEQ );
	UINT16_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_SEQPACKET_OFFSET_CHANNEL, channel->channelId );
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_SEQPACKET_OFFSET_SEQ, channel->xmitSeq );
	UINT32_PUT_UNALIGNED_HTON( chabu->xmitBuffer.data + CHABU_SEQPACKET_OFFSET_DATASIZE, pls );
	QueueVar_Read( channel->xmitQueue, chabu->xmitBuffer.data + 20, pls );
	chabu->xmitBuffer.position =  0;
	chabu->xmitBuffer.limit    = ps;

}

static void getXmitData(struct Chabu_Data* data ){

	if( !data->setupTx ){
		data->setupTx = true;
		xmitSetup( data );
		CHABU_DBGPRINTF("Chabu_GetXmitData startup %d byte supplied, startupRx:%d startupTx:%d", idx - idx2, data->startupRx, data->startupTx );
		return;
	}
	if( !data->setupRx ){
		return;
	}
	if( !data->acceptTx ){
		data->acceptTx = true;
		xmitAccept( data );
		return;
	}

	struct Chabu_Channel_Data* channel = calcNextXmitChannel( data );
	if( channel != NULL ){

		channel->userCallback( channel->userData, channel, Chabu_Channel_Event_PreTransmit );

		if( channel->recvArmShouldBeXmit ){
			channel->recvArmShouldBeXmit = false;
			xmitChannelArm( data, channel );
			return;
		}

		handleChannelSeq( data, channel );

		CHABU_DBGPRINTF("Chabu_GetXmitData ch[%d] %d byte supplied", channel->channelId, idx - idx2);
	}

}
/**
 * @return true if the data shall be flushed
 */
bool Chabu_GetXmitData_Buffer ( struct Chabu_Data* data, struct Chabu_Buffer* xmitData ){

	Chabu_LOCK_DO_LOCK(data->lock);

	bool flush = true;
	int loopCheck = 0;

	while( true ){

		loopCheck++;
		Chabu_Assert( loopCheck < 100 );

		// if xmit buffer is empty, try to fill it
		if( data->xmitBuffer.limit == 0 ){

			getXmitData(data);

			// if xmit buffer is still empty, go out
			if( data->xmitBuffer.limit == 0 ){
				break;
			}
		}

		// copy as much as possible to target
		copyBufferToBuffer( xmitData, &data->xmitBuffer);

		// if all is transferred to target, prepare for new data
		if( data->xmitBuffer.position == data->xmitBuffer.limit ){
			data->xmitBuffer.position = 0;
			data->xmitBuffer.limit    = 0;
		}

		// if target is full, go out
		if( xmitData->position == xmitData->limit ){
			break;
		}

	}

	Chabu_LOCK_DO_UNLOCK(data->lock);
	return flush;
}



void Chabu_Channel_evUserRecvRequest( struct Chabu_Channel_Data* channel ){
	struct Chabu_Data* chabu = channel->chabu;
	UNUSED(chabu);
	Chabu_LOCK_DO_LOCK(chabu->lock);
	channel->recvRequest = true;
	Chabu_LOCK_DO_UNLOCK(chabu->lock);
}
void Chabu_Channel_evUserXmitRequest( struct Chabu_Channel_Data* channel ){
	struct Chabu_Data* chabu = channel->chabu;
	UNUSED(chabu);
	Chabu_LOCK_DO_LOCK(chabu->lock);
	channel->xmitRequest = true;
	Chabu_LOCK_DO_UNLOCK(chabu->lock);
}


