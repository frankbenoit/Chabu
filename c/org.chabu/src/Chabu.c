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

struct Chabu_StructInfo {
	const char* name;
};

const struct Chabu_StructInfo structInfo_chabu   = { "chabu" };
const struct Chabu_StructInfo structInfo_channel = { "channel" };

static void handleReads( struct Chabu_Data* chabu );
static void handleWrites( struct Chabu_Data* chabu );
static bool writePendingXmitData( struct Chabu_Data* chabu );
static bool prepareNextXmitState(struct Chabu_Data* chabu);
static enum Chabu_ErrorCode checkAcceptance( struct Chabu_Data* chabu, struct Chabu_ByteBuffer_Data* msgBuffer );
static struct Chabu_Channel_Data* popNextArmRequest( struct Chabu_Data* chabu );

LIBRARY_API extern void Chabu_HandleNetwork ( struct Chabu_Data* chabu ){
	handleReads( chabu );
	handleWrites( chabu );
}

static void handleReads( struct Chabu_Data* chabu ){

	while( true ){

		struct Chabu_ByteBuffer_Data * rb = &chabu->recv.buffer;

		if( chabu->recv.state != Chabu_RecvState_SeqPayload ){
			if( rb->position < 8 ){
				rb->limit = 8;
				chabu->userCallback_NetworkRecvBuffer( chabu->userData, &chabu->recv.buffer );
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
				chabu->userCallback_NetworkRecvBuffer( chabu->userData, &chabu->recv.buffer );
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
				ci->applicationNameLength = Chabu_ByteBuffer_getString( rb, ci->applicationName, sizeof(ci->applicationName));
				ci->hasContent = true;

				chabu->recv.state = Chabu_RecvState_Accept;
			}
			else if( packetType == PacketType_Accept ){
				chabu->recv.state = Chabu_RecvState_Ready;
			}
//			else if( packetType == PacketType_ARM ){
//				chabu->recv.state = Chabu_RecvState_Ready;
//			}
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
				if( seq != ch->recvSeq ){
					Chabu_ReportError( chabu, Chabu_ErrorCode_PROTOCOL_SEQ_VALUE, __FILE__, __LINE__,
							"ch[%d] received wrong SEQ index: local:%d, received:%d", ch->channelId, ch->recvSeq, seq );
					return;
				}

				int payloadSizeAligned = Common_AlignUp4(payloadSize);

				chabu->recv.seqChannel = ch;
				chabu->recv.state = Chabu_RecvState_SeqPayload;
				chabu->recv.seqRemainingPayload = payloadSize;
				chabu->recv.seqRemainingPadding = payloadSizeAligned - payloadSize;

				assert( chabu->recv.seqRemainingPayload >= 0 );

			}
			rb->position = 0;
			rb->limit = 8;
		}
		if( chabu->recv.state == Chabu_RecvState_SeqPayload ) {
			if( chabu->recv.seqRemainingPayload > 0 ){

				struct Chabu_Channel_Data* ch = chabu->recv.seqChannel;
				if( chabu->recv.seqBufferUser == NULL ){
					chabu->recv.seqBufferUser =
							ch->userCallback_ChannelGetRecvBuffer(
									ch->userData,
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
					chabu->userCallback_NetworkRecvBuffer( chabu->userData, &chabu->recv.seqBuffer );
					int remainingNew = Chabu_ByteBuffer_remaining(&chabu->recv.seqBuffer);
					int readSize = remaining - remainingNew;
					if( readSize == 0 ){
						break;
					}
					chabu->recv.seqRemainingPayload -= readSize;
					assert( chabu->recv.seqRemainingPayload >= 0 );
					if( remainingNew == 0 ){
						chabu->recv.seqBufferUser->position = chabu->recv.seqBuffer.position;
						ch->userCallback_ChannelEventNotification(
								ch->userData,
								ch->channelId,
								Chabu_Channel_Event_RecvCompleted, 0 );
						chabu->recv.seqBufferUser = NULL;
					}
				}
			}
			if(( chabu->recv.seqRemainingPayload == 0 ) && ( chabu->recv.seqRemainingPadding > 0 )){
				rb->position = 0;
				rb->limit = chabu->recv.seqRemainingPadding;
				chabu->userCallback_NetworkRecvBuffer( chabu->userData, &chabu->recv.buffer );
				chabu->recv.seqRemainingPadding -= rb->position;
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

		chabu->userCallback_NetworkXmitBuffer( chabu->userData, &chabu->xmit.buffer );

		if( hasPendingXmitData( chabu )){
			chabu->userCallback_EventNotification( chabu->userData, Chabu_Event_NetworkRegisterWriteRequest );
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
static bool prepareNextXmitState(struct Chabu_Data* chabu){
	if( chabu->xmit.state == Chabu_XmitState_Setup ){
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
	}
	else if( chabu->xmit.state == Chabu_XmitState_Accept ){
		chabu->xmit.state = Chabu_XmitState_Idle;
	}


	if( chabu->xmit.state == Chabu_XmitState_Idle ){
		struct Chabu_Channel_Data* ch = popNextArmRequest( chabu );
		if( ch != NULL ){
			Chabu_ByteBuffer_clear( &chabu->xmit.buffer );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, 16 );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, PACKET_MAGIC | PacketType_ARM );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, ch->channelId );
			Chabu_ByteBuffer_putIntBe( &chabu->xmit.buffer, ch->recvArm );
			Chabu_ByteBuffer_flip( &chabu->xmit.buffer );

			chabu->xmit.state = Chabu_XmitState_Arm;
			return true;
		}
	}

	return false;
}

static struct Chabu_Channel_Data* popNextArmRequest( struct Chabu_Data* chabu ){
	int chIdx = 0;
	for( ; chIdx < chabu->channelCount; chIdx ++ ){
		struct Chabu_Channel_Data* ch = &chabu->channels[chIdx];
		if( ch->xmitRequestArm ){
			ch->xmitRequestArm = false;
			return ch;
		}
	}
	return NULL;
}
static enum Chabu_ErrorCode checkAcceptance( struct Chabu_Data* chabu, struct Chabu_ByteBuffer_Data* msgBuffer ){
	return chabu->userCallback_AcceptConnection( chabu->userData, &chabu->connectionInfoLocal, &chabu->connectionInfoRemote, msgBuffer );
}

static void handleWrites( struct Chabu_Data* chabu ){

	while( true ){
		bool allDataXmitted = writePendingXmitData( chabu );
		if( !allDataXmitted ){
			return;
		}

		bool createdData = prepareNextXmitState( chabu );
		if( !createdData ){
			return;
		}
	}

}
