/*
 * Chabu.c
 *
 *  Created on: 11.12.2014
 *      Author: Frank
 */


#include <string.h>
#include <stdint.h>
#include "Common.h"
#include "Chabu.h"
#include "QueueVar.h"

#define Chabu_PROTCOL_VERSION 1
#define PACKETFLAG_ARM  0x0001
#define PACKETFLAG_SEQ  0x0002

//#define CHABU_DBGPRINTF(...) dbg_printf(__VA_ARGS__)
//#define CHABU_DBGMEMORY(...) dbg_memory(__VA_ARGS__)
#define CHABU_DBGPRINTF(...)
#define CHABU_DBGMEMORY(...)


void Chabu_Init(
		struct Chabu_Data* data,
		int     applicationVersion,
		char*   applicationName,
		int     maxReceivePayloadSize,
		TChabu_UserCallback * userCallback,
		void * userData ){

	data->channelCount = 0;
	data->activated = false;

	data->connectionInfoLocal.index  = 0;
	data->connectionInfoLocal.length = 11;
	data->connectionInfoLocal.chabuProtocolVersion  = Chabu_PROTCOL_VERSION;
	data->connectionInfoLocal.byteOrderBigEndian    = true;
	data->connectionInfoLocal.v1.maxReceivePayloadSize = maxReceivePayloadSize;
	data->connectionInfoLocal.v1.receiveChannelCount   = 0;
	data->connectionInfoLocal.v1.applicationVersion    = applicationVersion;
	data->connectionInfoLocal.v1.applicationNameLength = Chabu_strnlen(applicationName, Chabu_APPLICATION_NAME_SIZE_MAX);
	strncpy( data->connectionInfoLocal.v1.applicationName, applicationName, Chabu_APPLICATION_NAME_SIZE_MAX );

	data->connectionInfoRemote.index  = 0;
	data->connectionInfoRemote.length = 11;
	memset( data->connectionInfoRemote.v1.applicationName, 0, Chabu_APPLICATION_NAME_SIZE_MAX );

#ifdef Chabu_USE_LOCK
	Chabu_LOCK_CREATE(data->lock);
#endif

	data->xmitChannelIdx = 0;
	data->recvChannelIdx = 0;

	data->startupRx = true;
	data->startupTx = true;

	data->userCallback = userCallback;
	data->userData     = userData;

	data->recvContinueChannel = NULL;
	data->xmitContinueChannel = NULL;

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
	data->xmitLastIndex  = 0;
	data->xmitLastLength = 0;

	channel->recvArmShouldBeXmit = true;

	channel->recvQueue      = recvQueue;
	channel->recvArm        = 0;
	channel->recvSeq        = 0;
	channel->recvRequest    = false;
	data->recvLastIndex  = 0;
	data->recvLastLength = 0;

	channel->recvArm        += QueueVar_Free( recvQueue );

	QueueVar_SetCallbackConsumed( recvQueue, &Chabu_Channel_QueueConsumed, channel );
	QueueVar_SetCallbackSupplied( xmitQueue, &Chabu_Channel_QueueSupplied, channel );

	data->channelCount++;
}

void Chabu_Init_Complete ( struct Chabu_Data* data ){
	data->activated = true;
	data->connectionInfoLocal.v1.receiveChannelCount = data->channelCount;
}

static bool calcNextXmitChannel( struct Chabu_Data* data ) {
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
		return true;
	}

	return false;
}


static int recvProtocolParameterSpecification( struct Chabu_Data* data, void* recvData, int length ){

	int res = 0;
	if(( data->connectionInfoRemote.index == 0) && ( res+1 <= length )){
		data->connectionInfoRemote.chabuProtocolVersion = *(uint8*)recvData;
		data->connectionInfoRemote.index++;
		res++;
		Chabu_Assert( data->connectionInfoRemote.chabuProtocolVersion == Chabu_PROTCOL_VERSION );
	}
	if(( data->connectionInfoRemote.index == 1) && ( res+1 <= length )){
		data->connectionInfoRemote.byteOrderBigEndian = ((*(uint8*)recvData) != 0 );
		data->connectionInfoRemote.index++;
		res++;
	}
	if( data->connectionInfoRemote.index >= 2 ){
		while( true ){
			if( res+1 > length || data->connectionInfoRemote.index >= data->connectionInfoRemote.length ){
				break;
			}
			uint32 value = *(uint8*)( recvData + data->connectionInfoRemote.index );
			switch( data->connectionInfoRemote.index ){
			case  2: data->connectionInfoRemote.v1.maxReceivePayloadSize |= value <<  8; break;
			case  3: data->connectionInfoRemote.v1.maxReceivePayloadSize |= value <<  0;
				data->xmitMaxPayloadSize = data->connectionInfoRemote.v1.maxReceivePayloadSize;
				//dbg_printf("Chabu: xmitMaxPayloadSize %d", data->xmitMaxPayloadSize );
				break;
			case  4: data->connectionInfoRemote.v1.receiveChannelCount   |= value <<  8;
				break;
			case  5: data->connectionInfoRemote.v1.receiveChannelCount   |= value <<  0;
				//dbg_printf("Chabu: data->connectionInfoRemote.v1.receiveCannelCount %d", data->connectionInfoRemote.v1.receiveCannelCount );
				break;
			case  6: data->connectionInfoRemote.v1.applicationVersion    |= value << 24;
				break;
			case  7: data->connectionInfoRemote.v1.applicationVersion    |= value << 16;
				break;
			case  8: data->connectionInfoRemote.v1.applicationVersion    |= value <<  8;
				break;
			case  9: data->connectionInfoRemote.v1.applicationVersion    |= value <<  0;
				//dbg_printf("Chabu: data->connectionInfoRemote.v1.applicationVersion 0x%X", data->connectionInfoRemote.v1.applicationVersion );
				break;
			case 10:
				data->connectionInfoRemote.v1.applicationNameLength = value;
				data->connectionInfoRemote.length = 11 + data->connectionInfoRemote.v1.applicationNameLength;
				break;
			default: {
					int nameIdx = data->connectionInfoRemote.index -11;
					if( nameIdx < data->connectionInfoRemote.v1.applicationNameLength ){
						data->connectionInfoRemote.v1.applicationName[nameIdx] = (char)value;
					}
				}
				break;
			}
			data->connectionInfoRemote.index++;
			res++;
			if( data->connectionInfoRemote.index >= data->connectionInfoRemote.length ){
				//dbg_printf("Chabu: data->connectionInfoRemote.v1.applicationName %s", data->connectionInfoRemote.v1.applicationName );
				data->startupRx = false;
			}
		}
	}
	return res;
}

static int xmitProtocolParameterSpecification( struct Chabu_Data* data, uint8* xmitData, int length ){

	int res = 0;
	if(( data->connectionInfoLocal.index == 0) && ( res+1 <= length )){
		xmitData[ res ] = data->connectionInfoLocal.chabuProtocolVersion;
		data->connectionInfoLocal.index++;
		res++;
		Chabu_Assert( data->connectionInfoLocal.chabuProtocolVersion == Chabu_PROTCOL_VERSION );
	}
	if(( data->connectionInfoLocal.index == 1) && ( res+1 <= length )){
		xmitData[ res ] = data->connectionInfoLocal.byteOrderBigEndian;
		data->connectionInfoLocal.index++;
		res++;
	}
	if( data->connectionInfoLocal.index >= 2 ){
		while( true ){
			if( res+1 > length ){
				break;
			}
			switch( data->connectionInfoLocal.index ){
			case  2: xmitData[ res ] = data->connectionInfoLocal.v1.maxReceivePayloadSize >>  8; break;
			case  3: xmitData[ res ] = data->connectionInfoLocal.v1.maxReceivePayloadSize >>  0; break;
			case  4: xmitData[ res ] = data->connectionInfoLocal.v1.receiveChannelCount   >>  8; break;
			case  5: xmitData[ res ] = data->connectionInfoLocal.v1.receiveChannelCount   >>  0; break;
			case  6: xmitData[ res ] = data->connectionInfoLocal.v1.applicationVersion    >> 24; break;
			case  7: xmitData[ res ] = data->connectionInfoLocal.v1.applicationVersion    >> 16; break;
			case  8: xmitData[ res ] = data->connectionInfoLocal.v1.applicationVersion    >>  8; break;
			case  9: xmitData[ res ] = data->connectionInfoLocal.v1.applicationVersion    >>  0; break;
			case 10: xmitData[ res ] = data->connectionInfoLocal.v1.applicationNameLength; break;
			default:
			{
				int nameIdx = data->connectionInfoLocal.index -11;
				if( nameIdx < data->connectionInfoLocal.v1.applicationNameLength ){
					xmitData[ res ] = data->connectionInfoLocal.v1.applicationName[nameIdx];
				}
				if( nameIdx >= data->connectionInfoLocal.v1.applicationNameLength ){
					data->startupTx = false;
					return res;
				}
			}
			}
			data->connectionInfoLocal.index++;
			res++;
		}
	}
	return res;
}


/**
 * Consume the provided data, even if the packet is not yet completed. Avoids payload copying.
 *
 * If the data is the complete packet, it is processed and data is transfered into the recv queue.
 * Otherwise the header is stored into a buffer and when the header is completed it is used from that
 * buffer.
 *
 * When the data is not giving a full packet, channel->chabu->recvContinueChannel is set to this
 * channel. Then the immediate next data must be delivered again to this channel.
 *
 * @Return: count of consumed bytes.
 *
 */
static int Chabu_Channel_handleRecv( struct Chabu_Data* chabu, struct Chabu_Channel_Data* channel, void* recvData, int length ){
	Chabu_Assert( length > 0 );
	int res = 0;

	// when there is no began header and the header can be read completely -> process
	// when there is no begun header and the header cannot be red completely -> store header
	// when there is a begun header -> store header

	if( chabu->recvLastIndex == 0 ){
		if( length >= Chabu_HEADER_ARM_SIZE ){
			// ok, can process ArmPacket completely
			int pkf = UINT16_GET_UNALIGNED_HTON( recvData + 2 ) & 0xFFFF;
			if( pkf == PACKETFLAG_ARM ){
				uint32 arm = UINT32_GET_UNALIGNED_HTON( recvData + 4 );
				if( channel->xmitArm != arm ){
					channel->xmitArm = arm;
					Chabu_Channel_evUserXmitRequest( channel );
				}
				return Chabu_HEADER_ARM_SIZE;
			}
			else if( pkf == PACKETFLAG_SEQ ){
				if( length <= Chabu_HEADER_SEQ_SIZE ){
					Common_MemCopy( chabu->recvHeaderBuffer, Chabu_HEADER_SEQ_SIZE, 0, recvData, length, 0, length );
					chabu->recvLastIndex = length;
					chabu->recvContinueChannel = channel;
					return length;
				}
				uint32 seq = UINT32_GET_UNALIGNED_HTON( recvData+4 );
				//pay load size
				int    pls = UINT16_GET_UNALIGNED_HTON( recvData+8 ) & 0xFFFF;

				if( channel->recvSeq != seq ){
					CHABU_DBGPRINTF("Chabu_Channel_handleRecv SEQ not matching %d != %d(expected)", channel->recvSeq, seq);
				}
				//Chabu_Assert( channel->recvSeq == seq );
				channel->recvSeq += pls;

				if( Chabu_HEADER_SEQ_SIZE+pls > length ){
					// packet not yet completely available
					// header must be stored
					Common_MemCopy( chabu->recvHeaderBuffer, Chabu_HEADER_SIZE_MAX, 0, recvData, length, 0, Chabu_HEADER_SEQ_SIZE );

					// Queue must have enough space,
					// because only armed payload is transmitted
					QueueVar_Write( channel->recvQueue, recvData+10, length - 10 );

					// continue with this channel with following data
					chabu->recvLastIndex = length;
					chabu->recvLastLength = Chabu_HEADER_SEQ_SIZE + pls;
					channel->chabu->recvContinueChannel = channel;

					// all input data was taken
					res = length;
				}
				else {
					// ok, full packet

					// Queue must have enough space,
					// because only armed payload is transmitted
					QueueVar_Write( channel->recvQueue, recvData+Chabu_HEADER_SEQ_SIZE, pls );
					chabu->recvLastIndex = 0;
					// only this packet size data was taken
					res = Chabu_HEADER_SEQ_SIZE + pls;
				}


				channel->userCallback( channel->userData, channel, Chabu_Channel_Event_DataAvailable );

				return res;

			}
			else {
				Chabu_Assert(false);
				return 0;
			}
		}
		else {
			//length < Chabu_HEADER_ARM_SIZE
			Common_MemCopy( chabu->recvHeaderBuffer, Chabu_HEADER_SIZE_MAX, 0, recvData, Chabu_HEADER_SIZE_MAX, 0, length );
			chabu->recvLastIndex = length;
			chabu->recvContinueChannel = channel;
			return length;
		}
	}

	Chabu_Assert(res==0);

	// there was already a started packet
	// now fill the header buffer
	if( chabu->recvLastIndex < 0 ){
		Chabu_dbg_printf("channel->channelId %d, channel->recvLastIndex %d, channel->recvLastLength %d", channel->channelId, chabu->recvLastIndex, chabu->recvLastLength );
	}
	Chabu_Assert( chabu->recvLastIndex >= 0 );

	//fill with ARM HEADER
	if( chabu->recvLastIndex < Chabu_HEADER_ARM_SIZE ){
		int sz = Chabu_HEADER_ARM_SIZE - chabu->recvLastIndex;
		if( sz > length ){
			sz = length;
		}
		Chabu_Assert( sz >= 0 );

		Common_MemCopy( chabu->recvHeaderBuffer, Chabu_HEADER_ARM_SIZE, chabu->recvLastIndex, recvData, length, 0, sz );
		chabu->recvLastIndex += sz;

		// the copied data is consumed
		res += sz;

		if( chabu->recvLastIndex < Chabu_HEADER_ARM_SIZE ){
			// still smaller 8, so no further processing
			channel->chabu->recvContinueChannel = channel;
			Chabu_Assert( sz > 0 );
			return res;
		}
	}

	// when here, the recv bytes must have at least 8
	int pkf = UINT16_GET_UNALIGNED_HTON( chabu->recvHeaderBuffer + 2 ) & 0xFFFF;
	if( pkf == PACKETFLAG_ARM ){
		uint32 arm = UINT32_GET_UNALIGNED_HTON( chabu->recvHeaderBuffer + 4 );
		if( channel->xmitArm != arm ){
			channel->xmitArm = arm;
			Chabu_Channel_evUserXmitRequest( channel );
		}

		// packet completed -> reset header buffer
		chabu->recvLastIndex = 0;

		// consumed up to the 8th byte
		return res;
	}

	//from here it must be a seq.
	Chabu_Assert( pkf == PACKETFLAG_SEQ );

	//fill up from ARM HEADER up to SEQ_HEADER (+2 Byte for the pls)
	if( chabu->recvLastIndex < Chabu_HEADER_SEQ_SIZE ){
		int sz = Chabu_HEADER_SEQ_SIZE - chabu->recvLastIndex;
		if( sz > length ){
			sz = length;
		}
		Chabu_Assert( sz >= 0 );

		Common_MemCopy( chabu->recvHeaderBuffer, Chabu_HEADER_SEQ_SIZE, chabu->recvLastIndex, recvData, length, res, sz );
		chabu->recvLastIndex += sz;

		// the copied data is consumed
		res += sz;

		if( chabu->recvLastIndex < Chabu_HEADER_SEQ_SIZE ){
			// still smaller 10, so no further processing
			channel->chabu->recvContinueChannel = channel;
			Chabu_Assert( sz > 0 );


			return res;
		}
	}

	// SEQ Header is available
	if( chabu->recvLastIndex == Chabu_HEADER_SEQ_SIZE ){

		uint32 seq = UINT32_GET_UNALIGNED_HTON( chabu->recvHeaderBuffer+4 );
		int    pls = UINT16_GET_UNALIGNED_HTON( chabu->recvHeaderBuffer+8 ) & 0xFFFF;

		Chabu_Assert( channel->recvSeq == seq );
		channel->recvSeq += pls;

		chabu->recvLastLength = Chabu_HEADER_SEQ_SIZE + pls;
	}

	{
		int remainingSrc = length - res;
		int remainingTrg = chabu->recvLastLength - chabu->recvLastIndex;
		if( remainingTrg > remainingSrc ){
			// packet not yet completely available

			// Queue must have enough space,
			// because only armed payload is transmitted
			Chabu_Assert( remainingSrc > 0 );
			QueueVar_Write( channel->recvQueue, recvData+res, remainingSrc );

			// continue with this channel with following data
			chabu->recvLastIndex += remainingSrc;
			channel->chabu->recvContinueChannel = channel;

			// increase consumed amount by the copied payload size
			res += remainingSrc;
		}
		else {
			// ok, full packet

			// Queue must have enough space,
			// because only armed payload is transmitted
			Chabu_Assert( remainingTrg > 0 );
			QueueVar_Write( channel->recvQueue, recvData+res, remainingTrg );

			// packet completed -> reset header buffer
			chabu->recvLastIndex = 0;
			chabu->recvLastLength = 0;

			// increase consumed amount by the copied payload size
			res += remainingTrg;
		}

		channel->userCallback( channel->userData, channel, Chabu_Channel_Event_DataAvailable );
	}
	return res;
}

void Chabu_PutRecvData ( struct Chabu_Data* data, void* recvData, int length ){

	Chabu_LOCK_DO_LOCK(data->lock);

	int idx = 0;

	while( idx < length ){

		if( data->startupRx ){
			Chabu_Assert( data->activated );
			idx += recvProtocolParameterSpecification( data, recvData + idx, length - idx );

			CHABU_DBGPRINTF( "Chabu_PutRecvData recv protocol spec %d bytes consumed, startupRx:%d startupTx:%d", (idx - idx2), data->startupRx, data->startupTx );
			continue;
		}

		//data from last loop still pending
		if( data->recvContinueChannel != NULL ){
			//still continue with last channel
			struct Chabu_Channel_Data* channel = data->recvContinueChannel;
			data->recvContinueChannel = NULL;
			CHABU_DBGPRINTF( "Chabu_PutRecvData recv continue" );
			//copy the received data to the defined buffers
			idx += Chabu_Channel_handleRecv( data, channel, recvData + idx, length - idx );

			continue;
		}
		//check new data -> 2 Options: ARM or SEQ

		// ARM packet: CC CC 00 01 AA AA AA AA
		// CC = channel (2 Bytes)
		// AA = ARM position

		// SEQ packet: CC CC 00 02 SS SS SS SS PP PP [ PP count of bytes ]
		// CC = channel (2 Bytes)
		// AA = ARM position
		{
			int channelId;

			{
				// when the last byte of a recv portion contains the first byte of the channel id,
				// it must be stored here and reused in the next call
				int remaining = length - idx;
				if( remaining == 1 ){
					data->recvHeaderBuffer[0] = ((uint8*)recvData)[idx];
					data->recvLastIndex = 1;
					idx++;
					continue;
				}

				// continue if previous only 1 Byte was available.
				if( data->recvLastIndex == 1 ){
					data->recvHeaderBuffer[1] = ((uint8*)recvData)[idx];
					data->recvLastIndex = 2;
					idx++;
					channelId = UINT16_GET_UNALIGNED_HTON( data->recvHeaderBuffer );
				}
				else {
					// normal processing of the channel ID
					Assert( data->recvLastIndex == 0 );
					channelId = UINT16_GET_UNALIGNED_HTON( recvData + idx );

					//TODO: if idx +=2
					//--> note assert in Chabu_Channel_handleRecv because:
					//length == idx
				}
			}


			if( channelId >= data->channelCount ){

				Chabu_dbg_printf( "idx %d, unexpected channelId 0x%x, flag 0x%x, seq/arm 0x%x, last_packet_pay_load_size: 0x%x",
						idx,
						channelId,
						UINT16_GET_UNALIGNED_HTON( recvData + idx+2) ,
						UINT32_GET_UNALIGNED_HTON( recvData + idx+4) , last_packet_pay_load_size );
				Chabu_dbg_memory("recvData", recvData, length );
			}

			Assert( channelId < data->channelCount );
			CHABU_DBGPRINTF( "Chabu_PutRecvData recv new" );

			idx += Chabu_Channel_handleRecv( data, data->channels[ channelId ], recvData + idx, length - idx );
		}
	}
	Chabu_Assert( idx == length );
	Chabu_LOCK_DO_UNLOCK(data->lock);

}

static int Chabu_Channel_handleXmit( struct Chabu_Data* chabu, struct Chabu_Channel_Data* channel, uint8* xmitData, int maxLength ){

	channel->userCallback( channel->userData, channel, Chabu_Channel_Event_PreTransmit );

	// Is no transfer in progress?
	if( chabu->xmitLastIndex == 0 && chabu->xmitLastLength == 0 ){

		// Generate the headers
		if( channel->recvArmShouldBeXmit ){
			channel->recvArmShouldBeXmit = false;
			UINT16_PUT_UNALIGNED_HTON( chabu->xmitHeaderBuffer + 0, channel->channelId );
			UINT16_PUT_UNALIGNED_HTON( chabu->xmitHeaderBuffer + 2, PACKETFLAG_ARM );
			UINT32_PUT_UNALIGNED_HTON( chabu->xmitHeaderBuffer + 4, channel->recvArm );
			chabu->xmitLastLength = Chabu_HEADER_ARM_SIZE;
		}
		else {
			// pls = payload size
			int pls = QueueVar_Available( channel->xmitQueue );

			if( pls > channel->chabu->xmitMaxPayloadSize ){
				pls = channel->chabu->xmitMaxPayloadSize;
			}
			int remainArm = channel->xmitArm - channel->xmitSeq;
			if( pls > remainArm ){
				pls = remainArm;
			}

			Assert( pls >= 0 ); // negative shall not occur
			if( pls > 0 ){
				UINT16_PUT_UNALIGNED_HTON( chabu->xmitHeaderBuffer + 0, channel->channelId );
				UINT16_PUT_UNALIGNED_HTON( chabu->xmitHeaderBuffer + 2, PACKETFLAG_SEQ );
				UINT32_PUT_UNALIGNED_HTON( chabu->xmitHeaderBuffer + 4, channel->xmitSeq );
				UINT16_PUT_UNALIGNED_HTON( chabu->xmitHeaderBuffer + 8, pls );
				chabu->xmitLastLength = pls + Chabu_HEADER_SEQ_SIZE;
				channel->xmitSeq += pls;
			}
		}

	}

	// if xfer in progress
	int idx = 0;
	if( chabu->xmitLastLength != 0 ){

		// block header
		if(( chabu->xmitLastIndex < Chabu_HEADER_SIZE_MAX ) && ( chabu->xmitLastIndex < chabu->xmitLastLength ) && ( idx < maxLength )){

			int remainingSrc = chabu->xmitLastLength - chabu->xmitLastIndex;
			if( remainingSrc > Chabu_HEADER_SIZE_MAX ){
				remainingSrc = Chabu_HEADER_SIZE_MAX;
			}
			int remainingTrg = maxLength - idx;
			int copySz = ( remainingTrg < remainingSrc ) ? remainingTrg : remainingSrc;
			Assert( copySz >= 0 );

			memcpy( xmitData + idx, chabu->xmitHeaderBuffer + chabu->xmitLastIndex, copySz );
			chabu->xmitLastIndex += copySz;
			idx += copySz;
		}

		// block payload
		if(( chabu->xmitLastIndex >= Chabu_HEADER_SIZE_MAX )
				&& ( chabu->xmitLastIndex < chabu->xmitLastLength )
				&& ( idx < maxLength ) ){

			int remainingSrc = chabu->xmitLastLength - chabu->xmitLastIndex;
			int remainingTrg = maxLength - idx;

			int copySz = ( remainingTrg < remainingSrc ) ? remainingTrg : remainingSrc;

			int size = QueueVar_Available( channel->xmitQueue );

			// is implicitly asserted in QueueVar_Read
			Assert( ( copySz >= 0 ) && (copySz <= size)  );
			QueueVar_Read( channel->xmitQueue, xmitData + idx, copySz );

			chabu->xmitLastIndex += copySz;
			idx += copySz;

		}

		// block completed
		if( chabu->xmitLastIndex >= chabu->xmitLastLength ){
			Assert( chabu->xmitLastIndex == chabu->xmitLastLength );
			chabu->xmitLastIndex  = 0;
			chabu->xmitLastLength = 0;
			channel->userCallback( channel->userData, channel, Chabu_Channel_Event_Transmitted );
		}
		else {
			// block to be continued
			// ensure, the next xmit data is requested from the same channel.
			chabu->xmitContinueChannel = channel;
		}
	}
	if(( channel->recvArmShouldBeXmit ) || (( QueueVar_Available( channel->xmitQueue ) > 0 ) && ( channel->xmitArm != channel->xmitSeq ))){
		channel->xmitRequest = true;
	}

	Assert( idx >= 0 && idx <= maxLength );
	return idx;
}

/**
 * @return true if the data shall be flushed
 */
bool Chabu_GetXmitData ( struct Chabu_Data* data, void* xmitData, int* xmitLength, int maxLength ){
	Chabu_LOCK_DO_LOCK(data->lock);
	bool flush = true;
	int idx = 0;
	int oldIdx = -1;
	int loopCheck = 0;
	while( idx < maxLength && oldIdx != idx){
		oldIdx = idx;
		loopCheck++;
		Chabu_Assert( loopCheck < 100 );

		if( data->startupTx ){
			idx += xmitProtocolParameterSpecification( data, (uint8*)(xmitData+idx), maxLength-idx);
			CHABU_DBGPRINTF("Chabu_GetXmitData startup %d byte supplied, startupRx:%d startupTx:%d", idx - idx2, data->startupRx, data->startupTx );
			continue;
		}
		if( data->startupRx ){
			break;
		}

		{
			//if last xmit was not finished: load channel with last channel
			struct Chabu_Channel_Data* channel = data->xmitContinueChannel;
			data->xmitContinueChannel = NULL;

			//last xmit was completed, get next channel
			if( channel == NULL ){
				if( calcNextXmitChannel( data ) ){
					channel = data->channels[ data->xmitChannelIdx ];
				}
			}
			if( channel != NULL ){
				idx += Chabu_Channel_handleXmit( data, channel, xmitData + idx, maxLength - idx );
				CHABU_DBGPRINTF("Chabu_GetXmitData ch[%d] %d byte supplied", channel->channelId, idx - idx2);
			}
		}
	}
	*xmitLength = idx;

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


