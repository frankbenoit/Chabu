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
    Chabu_ErrorCode_CHABU_IS_NULL,
    Chabu_ErrorCode_CHABU_IS_NOT_INITIALIZED,
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

	Chabu_Event_InitChannels  = 0,
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

struct Chabu_Channel_Data;
struct Chabu_Data;


typedef void           (CALL_SPEC Chabu_ErrorFunction               )( void* userData, enum Chabu_ErrorCode code, const char* file, int line, const char* fmt, ... );
typedef void           (CALL_SPEC Chabu_ConfigureChannels           )( void* userData );
typedef void           (CALL_SPEC Chabu_NetworkRegisterWriteRequest )( void* userData );
typedef int            (CALL_SPEC Chabu_NetworkRecvBuffer           )( void* userData, struct Buffer* buffer );
typedef int            (CALL_SPEC Chabu_NetworkXmitBuffer           )( void* userData, struct Buffer* buffer );

typedef struct Buffer* (CALL_SPEC Chabu_ChannelGetXmitBuffer)( void* userData );
typedef void           (CALL_SPEC Chabu_ChannelXmitCompleted)( void* userData );
typedef struct Buffer* (CALL_SPEC Chabu_ChannelGetRecvBuffer)( void* userData );
typedef void           (CALL_SPEC Chabu_ChannelRecvCompleted)( void* userData );

struct Chabu_ConnectionInfo_Data {
	uint32  receiveBufferSize;
	uint32  applicationVersion;
	uint8   applicationNameLength;
	char    applicationName[Chabu_APPLICATION_NAME_SIZE_MAX];
};
struct Chabu_Channel_Data;

struct Chabu_Priority_Data {
	struct Chabu_Channel_Data* firstChannelForPriority;
};

struct Chabu_Channel_Data {

	struct Chabu_Data* chabu;
	char*              instanceName;
	int                channelId;
	int                priority;

	Chabu_ChannelGetXmitBuffer * userCallback_ChannelGetXmitBuffer;
	Chabu_ChannelXmitCompleted * userCallback_ChannelXmitCompleted;
	Chabu_ChannelGetRecvBuffer * userCallback_ChannelGetRecvBuffer;
	Chabu_ChannelRecvCompleted * userCallback_ChannelRecvCompleted;
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

	Chabu_ErrorFunction * errorFunction;

	struct Chabu_ConnectionInfo_Data connectionInfoLocal;
	struct Chabu_ConnectionInfo_Data connectionInfoRemote;

#ifdef Chabu_USE_LOCK
	Chabu_LOCK_TYPE    lock;
#endif

	Chabu_ErrorFunction               * userCallback_ErrorFunction;
	Chabu_NetworkRegisterWriteRequest * userCallback_NetworkRegisterWriteRequest;
	Chabu_NetworkRecvBuffer           * userCallback_NetworkRecvBuffer;
	Chabu_NetworkXmitBuffer           * userCallback_NetworkXmitBuffer;
	void                              * userData;

	struct Chabu_Channel_Data**  channels;
	int                          channelCount;

	struct Chabu_Priority_Data** priorities;
	int                          priorityCount;
};

LIBRARY_API extern void Chabu_Init(
		struct Chabu_Data* chabu,

		int           applicationVersion,
		const char*   applicationName,

		struct Chabu_Channel_Data*  channels,
		int                         channelCount,

		struct Chabu_Priority_Data* priorities,
		int                         priorityCount,

		Chabu_ErrorFunction               * userCallback_ErrorFunction,
		Chabu_ConfigureChannels           * userCallback_ConfigureChannels,
		Chabu_NetworkRegisterWriteRequest * userCallback_NetworkRegisterWriteRequest,
		Chabu_NetworkRecvBuffer           * userCallback_NetworkRecvBuffer,
		Chabu_NetworkXmitBuffer           * userCallback_NetworkXmitBuffer,
		void * userData );

/**
 * Calls are only allowed from within the Chabu userCallback on event Chabu_Event_InitChannels
 */
LIBRARY_API extern void Chabu_ConfigureChannel (
		struct Chabu_Data* chabu,
		int channelId,
		int priority,
		Chabu_ChannelGetXmitBuffer * userCallback_ChannelGetXmitBuffer,
		Chabu_ChannelXmitCompleted * userCallback_ChannelXmitCompleted,
		Chabu_ChannelGetRecvBuffer * userCallback_ChannelGetRecvBuffer,
		Chabu_ChannelRecvCompleted * userCallback_ChannelRecvCompleted,
		void * userData );

LIBRARY_API extern enum Chabu_ErrorCode  Chabu_LastError( struct Chabu_Data* chabu );

/////////////////////////////////////////////////////////
// Network <-> Chabu

LIBRARY_API extern void Chabu_HandleNetwork ( struct Chabu_Data* chabu );

/////////////////////////////////////////////////////////
// Chabu Channel <-> Application

LIBRARY_API extern void  Chabu_Channel_SetXmitLimit( struct Chabu_Data* chabu, int channelId, int64 limit );
LIBRARY_API extern void  Chabu_Channel_AddXmitLimit( struct Chabu_Data* chabu, int channelId, int added );
LIBRARY_API extern int64 Chabu_Channel_GetXmitLimit( struct Chabu_Data* chabu, int channelId );
LIBRARY_API extern int64 Chabu_Channel_GetXmitPosition( struct Chabu_Data* chabu, int channelId );
LIBRARY_API extern int   Chabu_Channel_GetXmitRemaining( struct Chabu_Data* chabu, int channelId );

LIBRARY_API extern void  Chabu_Channel_SetRecvLimit( struct Chabu_Data* chabu, int channelId, int64 limit );
LIBRARY_API extern void  Chabu_Channel_AddRecvLimit( struct Chabu_Data* chabu, int channelId, int added );
LIBRARY_API extern int64 Chabu_Channel_GetRecvLimit( struct Chabu_Data* chabu, int channelId );
LIBRARY_API extern int64 Chabu_Channel_GetRecvPosition( struct Chabu_Data* chabu, int channelId );
LIBRARY_API extern int   Chabu_Channel_GetRecvRemaining( struct Chabu_Data* chabu, int channelId );

#ifdef __cplusplus
}
#endif

#endif /* CHABU_SRC_CHABU_H_ */
