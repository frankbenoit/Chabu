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
#include <assert.h>

#define MIN(a,b) (((a)<(b))?(a):(b))
#define MAX(a,b) (((a)>(b))?(a):(b))

struct Chabu_StructInfo {
	const char* name;
};

const struct Chabu_StructInfo structInfo_chabu    = { "chabu" };
const struct Chabu_StructInfo structInfo_channel  = { "channel" };
const struct Chabu_StructInfo structInfo_priority = { "priority" };

static void handleReads( struct Chabu_Data* chabu );
static void handleWrites( struct Chabu_Data* chabu );
static bool writePendingXmitData( struct Chabu_Data* chabu );
static bool prepareNextXmitState(struct Chabu_Data* chabu);
static enum Chabu_ErrorCode checkAcceptance( struct Chabu_Data* chabu, struct Chabu_ByteBuffer_Data* msgBuffer );
static void xmitGetUserData(struct Chabu_Data* chabu);
static void ensureNetworkWriteRequest(struct Chabu_Data* chabu);

LIBRARY_API void Chabu_HandleNetwork ( struct Chabu_Data* chabu ){
	handleReads( chabu );
	handleWrites( chabu );
}

static void ensureNetworkWriteRequest(struct Chabu_Data* chabu){
	chabu->user.eventNotification( chabu->user.data, Chabu_Event_NetworkRegisterWriteRequest );
}
static void handleReads( struct Chabu_Data* chabu ){

	while( true ){

		struct Chabu_ByteBuffer_Data * rb = &chabu->recv.buffer;

		if( chabu->recv.state != Chabu_RecvState_SeqPayload ){
			if( rb->position < 8 ){
				rb->limit = 8;
				chabu->user.networkRecvBuffer( chabu->user.data, &chabu->recv.buffer );
			}
			if( rb->position < 8 ){
				break;
			}

			int packetLength = Chabu_ByteBuffer_getIntAt_BE( rb, 0 );
			int packetId     = Chabu_ByteBuffer_getIntAt_BE( rb, 4 );
			uint8 packetType = UINT32_B0( packetId );
			bool isSeq = ( packetType == PacketType_SEQ );
			if( rb->position == 8 ){
				rb->limit = isSeq ? 20 : packetLength;
			}
			if( Chabu_ByteBuffer_hasRemaining(rb) ){
				chabu->user.networkRecvBuffer( chabu->user.data, &chabu->recv.buffer );
				if( Chabu_ByteBuffer_hasRemaining(rb) ){
					break;
				}
			}

			Chabu_ByteBuffer_flip( rb );
			rb->position = 8; // skip packetLength+id
			if( packetType == PacketType_Setup ){
				char protocolName[10];
				Chabu_ByteBuffer_getString( rb, protocolName, sizeof(protocolName));
				struct Chabu_ConnectionInfo_Data * ci = &chabu->connectionInfoRemote;

				ci->protocolVersion       = Chabu_ByteBuffer_getInt_BE( rb );
				ci->receivePacketSize     = Chabu_ByteBuffer_getInt_BE( rb );
				ci->applicationVersion    = Chabu_ByteBuffer_getInt_BE( rb );
				ci->applicationNameLength = (uint8) Chabu_ByteBuffer_getString( rb, ci->applicationName, sizeof(ci->applicationName));
				ci->hasContent = true;

				chabu->recv.state = Chabu_RecvState_Accept;
			}
			else if( packetType == PacketType_Accept ){
				chabu->recv.state = Chabu_RecvState_Ready;
			}
			else if( packetType == PacketType_ARM ){
				chabu->recv.state = Chabu_RecvState_Ready;
				int channelId   = Chabu_ByteBuffer_getInt_BE( rb );
				int arm         = Chabu_ByteBuffer_getInt_BE( rb );

				if( channelId < 0 || channelId >= chabu->channelCount ){
					Chabu_ReportError( chabu, Chabu_ErrorCode_PROTOCOL_CHANNEL_NOT_EXISTING, __FILE__, __LINE__,
							"ch[%d] does not exist. %d available channels", channelId, chabu->channelCount );
					return;
				}

				struct Chabu_Channel_Data* ch = &chabu->channels[ channelId ];
				if( ch->xmit.arm != arm ){
					ch->xmit.arm = arm;
					if(  ch->xmit.limit != ch->xmit.seq ){
						Chabu_Priority_SetRequestData( chabu, ch );
						ensureNetworkWriteRequest(chabu);
					}
				}
			}
			else if( packetType == PacketType_PING ){
				int payloadSize = Chabu_ByteBuffer_getInt_BE( rb );

				chabu->recvPing.pingData.position = 0;
				chabu->recvPing.pingData.limit    = payloadSize;
				chabu->recvPing.pingData.capacity = payloadSize;
				chabu->recvPing.pingData.data = &rb->data[ rb->position ];

				chabu->user.eventNotification( chabu->user.data, Chabu_Event_RemotePing );

				chabu->recvPing.pingData.position = 0;
				chabu->recvPing.pingData.limit    = 0;
				chabu->recvPing.pingData.capacity = 0;
				chabu->recvPing.pingData.data = NULL;
				chabu->recvPing.request = true;
				chabu->recv.state = Chabu_RecvState_Ready;
			}
			else if( packetType == PacketType_PONG ){
				int payloadSize = Chabu_ByteBuffer_getInt_BE( rb );
				if( chabu->xmitPing.pongData ){
					Chabu_ByteBuffer_xferWithMax( rb, chabu->xmitPing.pongData, payloadSize );
				}
				chabu->user.eventNotification( chabu->user.data, Chabu_Event_PingCompleted );
				chabu->recv.state = Chabu_RecvState_Ready;
			}
			else if( packetType == PacketType_SEQ ){

				int channelId   = Chabu_ByteBuffer_getInt_BE( rb );
				int seq         = Chabu_ByteBuffer_getInt_BE( rb );
				int payloadSize = Chabu_ByteBuffer_getInt_BE( rb );

				if( channelId < 0 || channelId >= chabu->channelCount ){
					Chabu_ReportError( chabu, Chabu_ErrorCode_PROTOCOL_CHANNEL_NOT_EXISTING, __FILE__, __LINE__,
							"ch[%d] does not exist. %d available channels", channelId, chabu->channelCount );
					return;
				}

				struct Chabu_Channel_Data* ch = &chabu->channels[ channelId ];
				if( seq != ch->recv.seq ){
					Chabu_ReportError( chabu, Chabu_ErrorCode_PROTOCOL_SEQ_VALUE, __FILE__, __LINE__,
							"ch[%d] received wrong SEQ index: local:%d, received:%d", ch->channelId, ch->recv.seq, seq );
					return;
				}

				int payloadSizeAligned = Common_AlignUp4(payloadSize);

				chabu->recv.seqChannel = ch;
				chabu->recv.state = Chabu_RecvState_SeqPayload;
				chabu->recv.seqRemainingPayload = payloadSize;
				chabu->recv.seqRemainingPadding = payloadSizeAligned - payloadSize;

				assert( chabu->recv.seqRemainingPayload >= 0 );

			}
			else {
				Chabu_ReportError( chabu, Chabu_ErrorCode_PROTOCOL_PCK_TYPE, __FILE__, __LINE__, "unknown packet type: 0x%X", packetId );
				return;
			}
			rb->position = 0;
			rb->limit = 8;
		}
		if( chabu->recv.state == Chabu_RecvState_SeqPayload ) {
			if( chabu->recv.seqRemainingPayload > 0 ){

				struct Chabu_Channel_Data* ch = chabu->recv.seqChannel;
				if( chabu->recv.seqBufferUser == NULL ){
					chabu->recv.seqBufferUser =
							ch->user.channelGetRecvBuffer(
									ch->user.data,
									ch->channelId,
									chabu->recv.seqRemainingPayload );
					assert( ch );
					assert( chabu->recv.seqBufferUser );
					if( !Chabu_ByteBuffer_hasRemaining(chabu->recv.seqBufferUser) ){
						Chabu_ReportError( chabu, Chabu_ErrorCode_RECV_USER_BUFFER_ZERO_LENGTH, __FILE__, __LINE__, "ch[%d] use gave buffer with zero length", ch->channelId );
						return;
					}
					chabu->recv.seqBuffer = *chabu->recv.seqBufferUser;

					int spaceFree = Chabu_ByteBuffer_remaining(&chabu->recv.seqBuffer);
					int spaceBiggerThanRequested = spaceFree - chabu->recv.seqRemainingPayload;
					if( spaceBiggerThanRequested > 0 ){
						chabu->recv.seqBuffer.limit -= spaceBiggerThanRequested;
					}
					assert( Chabu_ByteBuffer_hasRemaining(&chabu->recv.seqBuffer) );
				}

				{
					int remaining = Chabu_ByteBuffer_remaining(&chabu->recv.seqBuffer);
					chabu->user.networkRecvBuffer( chabu->user.data, &chabu->recv.seqBuffer );
					int remainingNew = Chabu_ByteBuffer_remaining(&chabu->recv.seqBuffer);
					int readSize = remaining - remainingNew;
					if( readSize == 0 ){
						break; // could not read
					}
					chabu->recv.seqRemainingPayload -= readSize;
					assert( chabu->recv.seqRemainingPayload >= 0 );
					if( remainingNew == 0 ){
						chabu->recv.seqBufferUser->position = chabu->recv.seqBuffer.position;
						ch->user.channelEventNotification(
								ch->user.data,
								ch->channelId,
								Chabu_Channel_Event_RecvCompleted, 0 );
						chabu->recv.seqBufferUser = NULL;
					}
				}
			}
			if(( chabu->recv.seqRemainingPayload == 0 ) && ( chabu->recv.seqRemainingPadding > 0 )){
				rb->position = 0;
				rb->limit = chabu->recv.seqRemainingPadding;
				chabu->user.networkRecvBuffer( chabu->user.data, &chabu->recv.buffer );
				chabu->recv.seqRemainingPadding -= rb->position;
				if( rb->position == 0 ){
					break; // could not read
				}
			}
			if(( chabu->recv.seqRemainingPayload == 0 ) && ( chabu->recv.seqRemainingPadding == 0 )){
				chabu->recv.state = Chabu_RecvState_Ready;
				rb->position = 0;
			}
		}
	}
}

static bool hasPendingXmitData( struct Chabu_Data* chabu ){
	return Chabu_ByteBuffer_hasRemaining( &chabu->xmit.buffer );
}
static bool writePendingXmitData( struct Chabu_Data* chabu ){

	if( Chabu_ByteBuffer_hasRemaining( &chabu->xmit.buffer )){

		chabu->user.networkXmitBuffer( chabu->user.data, &chabu->xmit.buffer );

		if( hasPendingXmitData( chabu )){
			return false;
		}
	}

	if( chabu->xmit.state == Chabu_XmitState_SeqPayload ){
		struct Chabu_Channel_Data* ch = chabu->xmit.seqChannel;
		assert( ch );
		while( chabu->xmit.seqRemainingPayload ){

			xmitGetUserData(chabu);

			struct Chabu_ByteBuffer_Data* buf = &chabu->xmit.seqBuffer;
			int availBefore = Chabu_ByteBuffer_remaining( buf );
			chabu->user.networkXmitBuffer( chabu->user.data, buf );
			int availAfter = Chabu_ByteBuffer_remaining( buf );
			int written = availBefore - availAfter;
			if( written == 0 ){
				break;
			}
			chabu->xmit.seqRemainingPayload -= written;
			assert( chabu->xmit.seqRemainingPayload >= 0 );

			if( !Chabu_ByteBuffer_hasRemaining( buf ) ){
				ch->user.channelEventNotification( ch->user.data, ch->channelId, Chabu_Channel_Event_XmitCompleted, 0 );
			}

		}
		if(( chabu->xmit.seqRemainingPayload == 0 ) && ( chabu->xmit.seqRemainingPadding > 0)){
			struct Chabu_ByteBuffer_Data* buf = &chabu->xmit.buffer;
			buf->position = 0;
			buf->limit = chabu->xmit.seqRemainingPadding;
			chabu->user.networkXmitBuffer( chabu->user.data, buf );
			chabu->xmit.seqRemainingPadding -= buf->position;
		}
		if(( chabu->xmit.seqRemainingPayload == 0 ) && ( chabu->xmit.seqRemainingPadding == 0)){
			chabu->xmit.state = Chabu_XmitState_Idle;
		}

		if( chabu->xmit.seqRemainingPayload ){
			return false;
		}
	}


	return true;
}
static void createAbortPacket(struct Chabu_Data* chabu, enum Chabu_ErrorCode error, struct Chabu_ByteBuffer_Data * msgBuffer ){

	int msgLength = Chabu_ByteBuffer_remaining(msgBuffer);
	if( msgLength > Chabu_ABORT_MSG_SIZE_MAX ){
		Chabu_ReportError( chabu, Chabu_ErrorCode_ASSERT, __FILE__, __LINE__, "abort packet called with too long message" );
		msgBuffer->limit -= ( msgLength - Chabu_ABORT_MSG_SIZE_MAX );
	}
	int packetLength = 16 + Common_AlignUp4(msgLength);
	if( Chabu_ByteBuffer_remaining( &chabu->xmit.buffer ) < packetLength ) {
		Chabu_ReportError( chabu, Chabu_ErrorCode_ASSERT, __FILE__, __LINE__, "abort packet too big for xmit buffer" );
		return;
	}
	Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, packetLength );
	Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, PACKET_MAGIC | PacketType_Abort );
	Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, error );
	Chabu_ByteBuffer_putStringFromBuffer( &chabu->xmit.buffer, msgBuffer );
	Chabu_ByteBuffer_flip( &chabu->xmit.buffer );
}

static void xmitGetUserData(struct Chabu_Data* chabu){

	struct Chabu_Channel_Data* ch = chabu->xmit.seqChannel;
	if( chabu->xmit.seqBufferUser == NULL ){
		chabu->xmit.seqBufferUser =
				ch->user.channelGetXmitBuffer(
						ch->user.data,
						ch->channelId,
						chabu->xmit.seqRemainingPayload );

		int userBufferAvailable = Chabu_ByteBuffer_remaining( chabu->xmit.seqBufferUser );
		if(( chabu->xmit.seqBufferUser == NULL ) || ( userBufferAvailable == 0 )) {
			Chabu_ReportError( chabu, Chabu_ErrorCode_XMIT_USER_BUFFER_ZERO_LENGTH, __FILE__, __LINE__, "xmit buffer is NULL or has no data" );
			return;
		}
		assert( chabu->xmit.seqBufferUser );
		chabu->xmit.seqBuffer = *chabu->xmit.seqBufferUser;
		int bufferTooLong = userBufferAvailable - chabu->xmit.seqRemainingPayload;
		if( bufferTooLong > 0 ){
			chabu->xmit.seqBuffer.limit -= bufferTooLong;
		}
	}
}
static bool prepareNextXmitState(struct Chabu_Data* chabu){

	switch( chabu->xmit.state ){
	case Chabu_XmitState_Setup:
		if( chabu->connectionInfoRemote.hasContent ){

			uint8 msgMemory[Chabu_ABORT_MSG_SIZE_MAX];
			struct Chabu_ByteBuffer_Data msgBuffer;
			Chabu_ByteBuffer_Init( &msgBuffer, msgMemory, sizeof(msgMemory));

			enum Chabu_ErrorCode error = checkAcceptance( chabu, &msgBuffer );
			if( error == Chabu_ErrorCode_OK_NOERROR ){

				Chabu_ByteBuffer_clear( &chabu->xmit.buffer );
				Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, 8 );
				Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, PACKET_MAGIC | PacketType_Accept );
				Chabu_ByteBuffer_flip( &chabu->xmit.buffer );

				chabu->xmit.state = Chabu_XmitState_Accept;
			}
			else {
				Chabu_ByteBuffer_flip( &msgBuffer );

				Chabu_ByteBuffer_clear( &chabu->xmit.buffer );
				createAbortPacket( chabu, error, &msgBuffer );
				chabu->xmit.state = Chabu_XmitState_Abort;
			}
			return true;
		}
		break;
	case Chabu_XmitState_Accept:
	case Chabu_XmitState_SeqPayload:
	case Chabu_XmitState_Ping:
	case Chabu_XmitState_Pong:
		chabu->xmit.state = Chabu_XmitState_Idle;
		break;
	case Chabu_XmitState_Seq:
		chabu->xmit.state = Chabu_XmitState_SeqPayload;
		return true;
	default:
		break;
	}


	if( chabu->xmit.state == Chabu_XmitState_Idle ){

		if( chabu->recvPing.request ){
			chabu->recvPing.request = false;
			int xmitSize = Chabu_ByteBuffer_remaining(&chabu->recvPing.pongData);
			if( xmitSize > Chabu_PongPayloadMax ){
				xmitSize = Chabu_PongPayloadMax;
			}
			int xmitSizeAligned = Common_AlignUp4(xmitSize);
			Chabu_ByteBuffer_clear( &chabu->xmit.buffer );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, 12+xmitSizeAligned );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, PACKET_MAGIC | PacketType_PONG );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, xmitSize );
			if( xmitSize ){
				Chabu_ByteBuffer_xferWithMax( &chabu->xmit.buffer, &chabu->recvPing.pongData, Chabu_PongPayloadMax );
				Chabu_ByteBuffer_putPadding( &chabu->xmit.buffer, xmitSizeAligned-xmitSize);
			}
			Chabu_ByteBuffer_flip( &chabu->xmit.buffer );

			chabu->xmit.state = Chabu_XmitState_Pong;
			return true;
		}

		if( chabu->xmitPing.request ){
			chabu->xmitPing.request = false;
			int xmitSize = 0;
			if( chabu->xmitPing.pingData ){
				xmitSize = Chabu_ByteBuffer_remaining(chabu->xmitPing.pingData);
			}
			if( xmitSize > Chabu_PingPayloadMax ){
				xmitSize = Chabu_PingPayloadMax;
			}
			int xmitSizeAligned = Common_AlignUp4(xmitSize);
			Chabu_ByteBuffer_clear( &chabu->xmit.buffer );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, 12+xmitSizeAligned );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, PACKET_MAGIC | PacketType_PING );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, xmitSize );
			if( xmitSize ){
				Chabu_ByteBuffer_xferWithMax( &chabu->xmit.buffer, chabu->xmitPing.pingData, Chabu_PingPayloadMax );
				Chabu_ByteBuffer_putPadding( &chabu->xmit.buffer, xmitSizeAligned-xmitSize);
			}
			Chabu_ByteBuffer_flip( &chabu->xmit.buffer );

			chabu->xmit.state = Chabu_XmitState_Ping;
			return true;
		}

		struct Chabu_Channel_Data* ch = NULL;
		ch = Chabu_Priority_PopNextRequestCtrl( chabu );
		if( ch != NULL ){
			if( ch->xmit.requestCtrl_Reset ){
				ch->xmit.requestCtrl_Reset = false;
				assert( false );
			}
			else if( ch->xmit.requestCtrl_Davail ){
				ch->xmit.requestCtrl_Davail = false;
				assert( false );
			}
			else if( ch->xmit.requestCtrl_Arm ){
				ch->xmit.requestCtrl_Arm = false;

				Chabu_ByteBuffer_clear( &chabu->xmit.buffer );
				Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, 16 );
				Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, PACKET_MAGIC | PacketType_ARM );
				Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, ch->channelId );
				Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, ch->recv.arm );
				Chabu_ByteBuffer_flip( &chabu->xmit.buffer );

				chabu->xmit.state = Chabu_XmitState_Arm;
				return true;
			}
			else {
				assert( false );
			}
		}

		ch = Chabu_Priority_PopNextRequestData( chabu );
		if( ch != NULL ){
			int maxXmitSize = chabu->receivePacketSize - SEQ_HEADER_SZ;
			uint64 xmitAvailable = ch->xmit.limit - ch->xmit.seq;
			uint64 xmitArmed     = ch->xmit.arm   - ch->xmit.seq;
			uint64 xmitSize = maxXmitSize;
			if( xmitSize > xmitAvailable ){
				xmitSize = xmitAvailable;
			}
			if( xmitSize > xmitArmed ){
				xmitSize = xmitArmed;
			}
			int xmitSizeAligned = Common_AlignUp4(xmitSize);
			Chabu_ByteBuffer_clear( &chabu->xmit.buffer );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, SEQ_HEADER_SZ+xmitSizeAligned );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, PACKET_MAGIC | PacketType_SEQ );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, ch->channelId );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, (uint32)ch->xmit.seq );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, (uint32)xmitSize );
			Chabu_ByteBuffer_flip( &chabu->xmit.buffer );

			chabu->xmit.seqChannel = ch;
			chabu->xmit.seqRemainingPayload = (int) xmitSize;
			chabu->xmit.seqRemainingPadding = (int)( xmitSizeAligned - xmitSize );
			chabu->xmit.seqBufferUser = NULL;
			chabu->xmit.state = Chabu_XmitState_Seq;
			return true;
		}
	}

	return false;
}

static enum Chabu_ErrorCode checkAcceptance( struct Chabu_Data* chabu, struct Chabu_ByteBuffer_Data* msgBuffer ){
	return chabu->user.acceptConnection( chabu->user.data, &chabu->connectionInfoLocal, &chabu->connectionInfoRemote, msgBuffer );
}

static void handleWrites( struct Chabu_Data* chabu ){

	while( true ){
		bool allDataXmitted = writePendingXmitData( chabu );
		if( !allDataXmitted ){
			ensureNetworkWriteRequest(chabu);
			return;
		}

		bool createdData = prepareNextXmitState( chabu );
		if( !createdData ){
			return;
		}
	}

}
void LIBRARY_API Chabu_StartPing ( struct Chabu_Data* chabu, struct Chabu_ByteBuffer_Data* pingData, struct Chabu_ByteBuffer_Data* pongData ){
	if( chabu->xmitPing.inProgress ){
		Chabu_ReportError( chabu, Chabu_ErrorCode_PING_IN_PROGRESS, __FILE__, __LINE__,
				"ping in progress" );
		return;

	}
	chabu->xmitPing.pingData = pingData;
	chabu->xmitPing.pongData = pongData;
	chabu->xmitPing.request = true;
	chabu->xmitPing.inProgress = true;

	ensureNetworkWriteRequest(chabu);
}

void  Chabu_SetPongData (struct Chabu_Data* chabu, struct Chabu_ByteBuffer_Data* pongData ){
	chabu->recvPing.pongData = *pongData;
}
