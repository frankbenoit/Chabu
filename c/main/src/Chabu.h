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

#ifndef CHABU_SRC_CHABU_H_
#define CHABU_SRC_CHABU_H_

#ifdef __cplusplus
extern "C" {
#endif


#include "ChabuOpts.h"
#include "Common.h"
#include "ByteBuffer.h"

#ifdef Chabu_USE_LOCK

# ifndef Chabu_LOCK_DO_LOCK
#  error "define Chabu_LOCK_DO_LOCK in ChabuOpts.h"
# endif
# ifndef Chabu_LOCK_DO_UNLOCK
#  error "define Chabu_LOCK_DO_UNLOCK in ChabuOpts.h"
# endif

#else
# undef Chabu_LOCK_DO_LOCK
# define Chabu_LOCK_DO_LOCK(_var)

# undef Chabu_LOCK_DO_UNLOCK
# define Chabu_LOCK_DO_UNLOCK(_var)

#endif

#ifndef Chabu_CHANNEL_COUNT_MAX
#define Chabu_CHANNEL_COUNT_MAX 64
#endif

#ifndef Chabu_PRIORITY_COUNT_MAX
#define Chabu_PRIORITY_COUNT_MAX 20
#endif

#define Chabu_APPLICATION_NAME_SIZE_MAX 56
#define Chabu_HEADER_SIZE_MAX 10
#define Chabu_HEADER_ARM_SIZE  8
#define Chabu_HEADER_SEQ_SIZE 10


enum Chabu_ErrorCode {
    Chabu_ErrorCode_OK_NOERROR = 0,
    Chabu_ErrorCode_UNKNOWN = 1,
    Chabu_ErrorCode_ASSERT = 2,
    Chabu_ErrorCode_NOT_ACTIVATED = 3,
    Chabu_ErrorCode_IS_ACTIVATED = 4,
    Chabu_ErrorCode_ILLEGAL_ARGUMENT = 5,
    Chabu_ErrorCode_CONFIGURATION_PRIOCOUNT = 11,
    Chabu_ErrorCode_CONFIGURATION_NETWORK = 12,
    Chabu_ErrorCode_CONFIGURATION_CH_ID = 13,
    Chabu_ErrorCode_CONFIGURATION_CH_PRIO = 14,
    Chabu_ErrorCode_CONFIGURATION_CH_USER = 15,
    Chabu_ErrorCode_CONFIGURATION_CH_RECVSZ = 16,
    Chabu_ErrorCode_CONFIGURATION_NO_CHANNELS = 17,
    Chabu_ErrorCode_CONFIGURATION_VALIDATOR = 18,
    Chabu_ErrorCode_SETUP_LOCAL_MAXRECVSIZE = 21,
    Chabu_ErrorCode_SETUP_LOCAL_APPLICATIONNAME = 23,
    Chabu_ErrorCode_SETUP_REMOTE_CHABU_VERSION = 31,
    Chabu_ErrorCode_SETUP_REMOTE_CHABU_NAME = 31,
    Chabu_ErrorCode_SETUP_REMOTE_MAXRECVSIZE = 32,
    Chabu_ErrorCode_PROTOCOL_LENGTH = 50,
    Chabu_ErrorCode_PROTOCOL_PCK_TYPE = 51,
    Chabu_ErrorCode_PROTOCOL_ABORT_MSG_LENGTH = 52,
    Chabu_ErrorCode_PROTOCOL_SETUP_TWICE = 53,
    Chabu_ErrorCode_PROTOCOL_ACCEPT_TWICE = 54,
    Chabu_ErrorCode_PROTOCOL_EXPECTED_SETUP = 55,
    Chabu_ErrorCode_PROTOCOL_CHANNEL_RECV_OVERFLOW = 56,
    Chabu_ErrorCode_PROTOCOL_DATA_OVERFLOW = 57,
    Chabu_ErrorCode_REMOTE_ABORT = 100,
    Chabu_ErrorCode_APPLICATION_VALIDATOR = 256,
};

enum Chabu_Event {

	Chabu_Event_DataAvailable = 1,
	Chabu_Event_CanTransmit   = 2,
	Chabu_Event_Connecting    = 3,

	Chabu_Event_Error         = 0x1000,
	Chabu_Event_Error_AbortRx,

	Chabu_Event_Error_Protocol = 0x1100,
	Chabu_Event_Error_Protocol_UnknownRx,

	Chabu_Event_Error_RxSetup_PN = 0x1200,
	Chabu_Event_Error_RxSetup_PV,
	Chabu_Event_Error_RxSetup_RS,
	Chabu_Event_Error_RxSetup_AV,
	Chabu_Event_Error_RxSetup_AN,
	Chabu_Event_Error_RxSetup_AN_Length,


};

enum Chabu_Channel_Event {
	Chabu_Channel_Event_Activated     = 1,
	Chabu_Channel_Event_DataAvailable = 2,
	Chabu_Channel_Event_PreTransmit   = 3,
	Chabu_Channel_Event_Transmitted   = 4,
};

struct Chabu_Channel_Data;
struct Chabu_Data;


typedef void (CALL_SPEC TChabu_AssertFunction     )( enum Chabu_ErrorCode code, void* userData, struct Chabu_Data*         chabuData, const char* file, int line, const char* fmt, ... );
typedef void (CALL_SPEC TChabu_UserCallback       )( void* userData, struct Chabu_Data*         chabuData, enum Chabu_Event         event );
typedef void (CALL_SPEC TChabu_ChannelUserCallback)( void* userData, struct Chabu_Channel_Data* chabuData, enum Chabu_Channel_Event event );

struct Chabu_ConnectionInfo_Data {
	uint32  receiveBufferSize;
	uint32  applicationVersion;
	uint8   applicationNameLength;
	char    applicationName[Chabu_APPLICATION_NAME_SIZE_MAX];
};

struct Chabu_Channel_Data {

	struct Chabu_Data* chabu;
	char*              instanceName;
	int                channelId;
	int                priority;

	TChabu_ChannelUserCallback * userCallback;
	void                       * userData;

	struct QueueVar* xmitQueue;
	uint32           xmitSeq;
	uint32           xmitArm;

	bool             xmitRequestArm;
	bool             xmitRequestData;

	struct QueueVar* recvQueue;
	uint32           recvSeq;
	uint32           recvArm;
	bool             recvRequest;

	struct Chabu_Channel_Data*  xmitRequestArmNext;
	struct Chabu_Channel_Data*  xmitRequestDataNext;
};

struct Chabu_Data {

	TChabu_AssertFunction * assertFunction;

	struct Chabu_ConnectionInfo_Data connectionInfoLocal;
	struct Chabu_ConnectionInfo_Data connectionInfoRemote;

#ifdef Chabu_USE_LOCK
	Chabu_LOCK_TYPE    lock;
#endif

	TChabu_UserCallback * userCallback;
	void                * userData;

	struct Chabu_Channel_Data* channels[Chabu_CHANNEL_COUNT_MAX];
	int                        channelCount;
	int                        priorityCount;

	int    xmitChannelIdx;

	struct Chabu_Channel_Data*  xmitRequestArmListHead[Chabu_PRIORITY_COUNT_MAX];
	struct Chabu_Channel_Data*  xmitRequestArmListTail[Chabu_PRIORITY_COUNT_MAX];
	struct Chabu_Channel_Data*  xmitRequestDataListHead[Chabu_PRIORITY_COUNT_MAX];
	struct Chabu_Channel_Data*  xmitRequestDataListTail[Chabu_PRIORITY_COUNT_MAX];

	struct ByteBuffer_Data recvBuffer;

	struct ByteBuffer_Data xmitBuffer;
	int                 xmitMaxPacketSize;

	bool activated; // true when initialization is completed
	bool setupRx;   // reveived the setup packet
	bool setupTx;   // put the setup packet into xmit buffer
	bool acceptRx;  // received the accept packet
	bool acceptTx;  // put the accept packet into the xmit buffer

	bool xmitAbort; // an abort packet shall be send on next possibility

	int   lastErrorCode;
	char* lastErrorString;

};

LIBRARY_API extern void Chabu_Init(
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

		TChabu_AssertFunction assertFunction
		);

LIBRARY_API extern void Chabu_Init_AddChannel (
		struct Chabu_Data* chabu,
		int channelId,
		struct Chabu_Channel_Data* channel,
		TChabu_ChannelUserCallback * userCallback, void * userData,
		int priority,
		struct QueueVar* recvQueue,
		struct QueueVar* xmitQueue );

LIBRARY_API extern void Chabu_Init_Complete ( struct Chabu_Data* chabu );

LIBRARY_API extern struct Chabu_Channel_Data* Chabu_GetChannel (
		struct Chabu_Data* chabu,
		int channelId );

LIBRARY_API extern void* Chabu_Channel_GetUserData ( struct Chabu_Channel_Data* channel );


//extern char* Chabu_GetLastErrorString( struct Chabu_Data* data );
//extern int   Chabu_GetLastErrorCode( struct Chabu_Data* data );
/////////////////////////////////////////////////////////
// Network <-> Chabu

/**
 * Called by the network connection to transfer received data into chabu and distribute it towards the channels.
 */
LIBRARY_API extern void Chabu_PutRecvData ( struct Chabu_Data* chabu, struct ByteBuffer_Data* recvData );

/**
 * Called by the network connection to retrieve data from chabu.
 */
LIBRARY_API extern bool Chabu_GetXmitData ( struct Chabu_Data* chabu, struct ByteBuffer_Data* xmitData );


/////////////////////////////////////////////////////////
// Chabu Channel <-> Application
LIBRARY_API extern void Chabu_Channel_evUserRecvRequest( struct Chabu_Channel_Data* channel );
LIBRARY_API extern void Chabu_Channel_XmitRequestData( struct Chabu_Channel_Data* channel );

#ifdef __cplusplus
}
#endif

#endif /* CHABU_SRC_CHABU_H_ */
