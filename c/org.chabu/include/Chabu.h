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


//#include "ChabuOpts.h"
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

#define Chabu_ABORT_MSG_SIZE_MAX        56
#define Chabu_APPLICATION_NAME_SIZE_MAX 56
#define Chabu_HEADER_SIZE_MAX 10
#define Chabu_HEADER_ARM_SIZE  8
#define Chabu_HEADER_SEQ_SIZE 10

#define Chabu_ProtocolVersion 0x00010001UL


enum Chabu_ErrorCode {
    Chabu_ErrorCode_OK_NOERROR = 0,
    Chabu_ErrorCode_UNKNOWN,
    Chabu_ErrorCode_ASSERT,
    Chabu_ErrorCode_NOT_ACTIVATED,
    Chabu_ErrorCode_IS_ACTIVATED,
    Chabu_ErrorCode_ILLEGAL_ARGUMENT,
    Chabu_ErrorCode_CHABU_IS_NULL,
    Chabu_ErrorCode_CHABU_IS_NOT_INITIALIZED,
    Chabu_ErrorCode_INIT_ERROR_FUNC_NULL,
    Chabu_ErrorCode_INIT_PARAM_APNAME_NULL,
    Chabu_ErrorCode_INIT_PARAM_APNAME_TOO_LONG,
    Chabu_ErrorCode_INIT_PARAM_RPS_RANGE,
    Chabu_ErrorCode_INIT_ACCEPT_FUNC_NULL,
    Chabu_ErrorCode_INIT_CONFIGURE_INVALID_CHANNEL,
    Chabu_ErrorCode_INIT_EVENT_FUNC_NULL,
    Chabu_ErrorCode_INIT_NW_READ_FUNC_NULL,
    Chabu_ErrorCode_INIT_NW_WRITE_FUNC_NULL,
    Chabu_ErrorCode_INIT_PARAM_CHANNELS_NULL,
    Chabu_ErrorCode_INIT_PARAM_CHANNELS_RANGE,
    Chabu_ErrorCode_INIT_PARAM_PRIORITIES_NULL,
    Chabu_ErrorCode_INIT_PARAM_PRIORITIES_RANGE,
    Chabu_ErrorCode_INIT_CHANNEL_FUNCS_NULL,
    Chabu_ErrorCode_INIT_CHANNELS_NOT_CONFIGURED,
    Chabu_ErrorCode_CONFIGURATION_PRIOCOUNT,
    Chabu_ErrorCode_CONFIGURATION_NETWORK,
    Chabu_ErrorCode_CONFIGURATION_CH_ID,
    Chabu_ErrorCode_CONFIGURATION_CH_PRIO,
    Chabu_ErrorCode_CONFIGURATION_CH_USER,
    Chabu_ErrorCode_CONFIGURATION_CH_RECVSZ,
    Chabu_ErrorCode_CONFIGURATION_NO_CHANNELS,
    Chabu_ErrorCode_CONFIGURATION_VALIDATOR,
    Chabu_ErrorCode_SETUP_LOCAL_MAXRECVSIZE,
    Chabu_ErrorCode_SETUP_LOCAL_APPLICATIONNAME,
    Chabu_ErrorCode_SETUP_REMOTE_CHABU_VERSION,
    Chabu_ErrorCode_SETUP_REMOTE_CHABU_NAME,
    Chabu_ErrorCode_SETUP_REMOTE_MAXRECVSIZE,
    Chabu_ErrorCode_RECV_USER_BUFFER_ZERO_LENGTH,
    Chabu_ErrorCode_PROTOCOL_LENGTH,
    Chabu_ErrorCode_PROTOCOL_PCK_TYPE,
    Chabu_ErrorCode_PROTOCOL_ABORT_MSG_LENGTH,
    Chabu_ErrorCode_PROTOCOL_SETUP_TWICE,
    Chabu_ErrorCode_PROTOCOL_ACCEPT_TWICE,
    Chabu_ErrorCode_PROTOCOL_EXPECTED_SETUP,
    Chabu_ErrorCode_PROTOCOL_CHANNEL_RECV_OVERFLOW,
    Chabu_ErrorCode_PROTOCOL_DATA_OVERFLOW,
    Chabu_ErrorCode_PROTOCOL_SEQ_VALUE,
    Chabu_ErrorCode_PROTOCOL_CHANNEL_NOT_EXISTING,
    Chabu_ErrorCode_REMOTE_ABORT,
    Chabu_ErrorCode_APPLICATION_VALIDATOR = 256,
};

enum Chabu_Event {
	Chabu_Event_InitChannels,
	Chabu_Event_NetworkRegisterWriteRequest,
	Chabu_Event_RemotePing,
};

enum Chabu_Channel_Event {
	Chabu_Channel_Event_XmitCompleted,
	Chabu_Channel_Event_RecvCompleted,
	Chabu_Channel_Event_RemoteArm,
	Chabu_Channel_Event_RemoteDavail,
	Chabu_Channel_Event_RemoteReset,
	Chabu_Channel_Event_ResetCompleted
};

struct Chabu_ByteBuffer_Data;
struct Chabu_Channel_Data;
struct Chabu_Data;
struct Chabu_StructInfo;
struct Chabu_ConnectionInfo_Data;

typedef enum Chabu_ErrorCode (CALL_SPEC Chabu_AcceptConnection      )( void* userData, struct Chabu_ConnectionInfo_Data* local, struct Chabu_ConnectionInfo_Data* remote, struct Chabu_ByteBuffer_Data* msg );
typedef void           (CALL_SPEC Chabu_ErrorFunction               )( void* userData, enum Chabu_ErrorCode code, const char* file, int line, const char* msg );
typedef void           (CALL_SPEC Chabu_EventNotification           )( void* userData, enum Chabu_Event event );
typedef void           (CALL_SPEC Chabu_NetworkRecvBuffer           )( void* userData, struct Chabu_ByteBuffer_Data* buffer );
typedef void           (CALL_SPEC Chabu_NetworkXmitBuffer           )( void* userData, struct Chabu_ByteBuffer_Data* buffer );

typedef void           (CALL_SPEC Chabu_ChannelEventNotification)( void* userData, int channelId, enum Chabu_Channel_Event event, int32 param );
typedef struct Chabu_ByteBuffer_Data* (CALL_SPEC Chabu_ChannelGetXmitBuffer)( void* userData, int channelId, int maxSize );
typedef struct Chabu_ByteBuffer_Data* (CALL_SPEC Chabu_ChannelGetRecvBuffer)( void* userData, int channelId, int wantedSize );

struct Chabu_ByteBuffer_Data {
	int position;
	int limit;
	int capacity;
	uint8 * data;
	bool   byteOrderIsBigEndian;
};

struct Chabu_ConnectionInfo_Data {
	const struct Chabu_StructInfo* info;
	bool    hasContent;
	uint32  receiveBufferSize;
	uint32  protocolVersion;
	uint32  receivePacketSize;
	uint32  applicationVersion;
	uint8   applicationNameLength;
	char    applicationName[Chabu_APPLICATION_NAME_SIZE_MAX];
};

struct Chabu_Channel_Data;

struct Chabu_Priority_Data {
	struct Chabu_StructInfo* info;
	struct Chabu_Channel_Data* firstChannelForPriority;
};

struct Chabu_Channel_Data {
	const struct Chabu_StructInfo* info;
	struct Chabu_Data* chabu;
	char*              instanceName;
	int                channelId;
	int                priority;

	Chabu_ChannelEventNotification * userCallback_ChannelEventNotification;
	Chabu_ChannelGetXmitBuffer * userCallback_ChannelGetXmitBuffer;
	Chabu_ChannelGetRecvBuffer * userCallback_ChannelGetRecvBuffer;
	void                       * userData;

	///struct QueueVar* xmitQueue;
	uint32           xmitSeq;
	uint32           xmitArm;

	bool             xmitRequestArm;
	bool             xmitRequestData;

	//struct QueueVar* recvQueue;
	uint32           recvSeq;
	uint32           recvArm;
	bool             recvRequest;

	struct Chabu_Channel_Data*  xmitRequestArmNext;
	struct Chabu_Channel_Data*  xmitRequestDataNext;
};

/**
 * Indicates the state this is in sending progress.
 */
enum Chabu_XmitState {
	Chabu_XmitState_Setup,
	Chabu_XmitState_Accept,
	Chabu_XmitState_Abort,
	Chabu_XmitState_Seq,
	Chabu_XmitState_Arm,
	Chabu_XmitState_Idle,
};

/**
 * Indicates the state that must follow next
 */
enum Chabu_RecvState {
	Chabu_RecvState_Setup,
	Chabu_RecvState_Accept,
	Chabu_RecvState_Ready,
	Chabu_RecvState_SeqPayload,
};

struct Chabu_Data {

	const struct Chabu_StructInfo* info;

	struct Chabu_ConnectionInfo_Data connectionInfoLocal;
	struct Chabu_ConnectionInfo_Data connectionInfoRemote;

#ifdef Chabu_USE_LOCK
	Chabu_LOCK_TYPE    lock;
#endif

	Chabu_ErrorFunction               * userCallback_ErrorFunction;
	Chabu_AcceptConnection            * userCallback_AcceptConnection;
	Chabu_EventNotification           * userCallback_EventNotification;
	Chabu_NetworkRecvBuffer           * userCallback_NetworkRecvBuffer;
	Chabu_NetworkXmitBuffer           * userCallback_NetworkXmitBuffer;
	void                              * userData;
	enum Chabu_ErrorCode         lastError;

	struct Chabu_Channel_Data*   channels;
	int                          channelCount;

	struct Chabu_Priority_Data*  priorities;
	int                          priorityCount;
	char                         errorMessage[200];

	struct {
		enum Chabu_XmitState         state;
		uint8                        memory[0x100];
		struct Chabu_ByteBuffer_Data buffer;
	} xmit;

	struct {
		enum Chabu_RecvState         state;
		uint8                        memory[0x100];
		struct Chabu_ByteBuffer_Data buffer;

		struct Chabu_Channel_Data*    seqChannel;
		int                           seqRemainingPayload;
		int                           seqRemainingPadding;
		struct Chabu_ByteBuffer_Data* seqBufferUser;
		struct Chabu_ByteBuffer_Data  seqBuffer;
	} recv;

	int                          receivePacketSize;


};

LIBRARY_API void Chabu_Init(
		struct Chabu_Data* chabu,

		int           applicationVersion,
		const char*   applicationName,
		int           receivePacketSize,

		struct Chabu_Channel_Data*  channels,
		int                         channelCount,

		struct Chabu_Priority_Data* priorities,
		int                         priorityCount,

		Chabu_ErrorFunction                 * userCallback_ErrorFunction,
		Chabu_AcceptConnection              * userCallback_AcceptConnection,
		Chabu_EventNotification             * userCallback_EventNotification,
		Chabu_NetworkRecvBuffer             * userCallback_NetworkRecvBuffer,
		Chabu_NetworkXmitBuffer             * userCallback_NetworkXmitBuffer,
		void * userData );

/**
 * Calls are only allowed from within the Chabu userCallback on event Chabu_Event_InitChannels
 */
LIBRARY_API void Chabu_ConfigureChannel (
		struct Chabu_Data* chabu,
		int channelId,
		int priority,
		Chabu_ChannelEventNotification * userCallback_ChannelEvent,
		Chabu_ChannelGetXmitBuffer     * userCallback_ChannelGetXmitBuffer,
		Chabu_ChannelGetRecvBuffer     * userCallback_ChannelGetRecvBuffer,
		void * userData );

LIBRARY_API enum Chabu_ErrorCode  Chabu_LastError( struct Chabu_Data* chabu );
LIBRARY_API const char*  Chabu_LastErrorStr( struct Chabu_Data* chabu );

LIBRARY_API const char*  Chabu_ErrorCodeStr( enum Chabu_ErrorCode e );
LIBRARY_API const char*  Chabu_XmitStateStr( enum Chabu_XmitState v );
LIBRARY_API const char*  Chabu_RecvStateStr( enum Chabu_RecvState v );
LIBRARY_API const char*  Chabu_Channel_EventStr( enum Chabu_Channel_Event v );

/////////////////////////////////////////////////////////
// Network <-> Chabu

LIBRARY_API void Chabu_HandleNetwork ( struct Chabu_Data* chabu );

/////////////////////////////////////////////////////////
// Chabu Channel <-> Application

LIBRARY_API void  Chabu_Channel_StartReset( struct Chabu_Data* chabu, int channelId );

LIBRARY_API void  Chabu_Channel_SetXmitLimit( struct Chabu_Data* chabu, int channelId, int64 limit );
LIBRARY_API void  Chabu_Channel_AddXmitLimit( struct Chabu_Data* chabu, int channelId, int added );
LIBRARY_API int64 Chabu_Channel_GetXmitLimit( struct Chabu_Data* chabu, int channelId );
LIBRARY_API int64 Chabu_Channel_GetXmitPosition( struct Chabu_Data* chabu, int channelId );
LIBRARY_API int   Chabu_Channel_GetXmitRemaining( struct Chabu_Data* chabu, int channelId );

LIBRARY_API void  Chabu_Channel_SetRecvLimit( struct Chabu_Data* chabu, int channelId, int64 limit );
LIBRARY_API void  Chabu_Channel_AddRecvLimit( struct Chabu_Data* chabu, int channelId, int added );
LIBRARY_API int64 Chabu_Channel_GetRecvLimit( struct Chabu_Data* chabu, int channelId );
LIBRARY_API int64 Chabu_Channel_GetRecvPosition( struct Chabu_Data* chabu, int channelId );
LIBRARY_API int   Chabu_Channel_GetRecvRemaining( struct Chabu_Data* chabu, int channelId );



/////////////////////////////////////////////////////////
// Chabu Byte Buffer


static inline void Chabu_ByteBuffer_Init( struct Chabu_ByteBuffer_Data* data, uint8* memory, int size ){
	data->capacity = size;
	data->limit    = size;
	data->position = 0;
	data->data     = memory;
	data->byteOrderIsBigEndian = true;
}

static inline void Chabu_ByteBuffer_clear(struct Chabu_ByteBuffer_Data* data){
	data->limit    = data->capacity;
	data->position = 0;
}

static inline void Chabu_ByteBuffer_flip(struct Chabu_ByteBuffer_Data* data){
	data->limit    = data->position;
	data->position = 0;
}

static inline bool Chabu_ByteBuffer_hasRemaining(struct Chabu_ByteBuffer_Data* data){
	return data->position < data->limit;
}

int32 Chabu_ByteBuffer_getIntAt_BE(struct Chabu_ByteBuffer_Data* data, int pos );
int32 Chabu_ByteBuffer_getInt_BE(struct Chabu_ByteBuffer_Data* data );
int32 Chabu_ByteBuffer_getString(struct Chabu_ByteBuffer_Data* data, char* buffer, int bufferSize );

int  Chabu_ByteBuffer_xferAllPossible(struct Chabu_ByteBuffer_Data* trg, struct Chabu_ByteBuffer_Data* src);

static __inline__ int  Chabu_ByteBuffer_remaining(struct Chabu_ByteBuffer_Data* data){
	return data->limit - data->position;
}

static __inline__ void Chabu_ByteBuffer_putByte(struct Chabu_ByteBuffer_Data* data, uint8 value){
	data->data[ data->position ] = value;
	data->position++;
}

void Chabu_ByteBuffer_putStringFromBuffer(struct Chabu_ByteBuffer_Data* data, struct Chabu_ByteBuffer_Data* stringBuffer);

static __inline__ void Chabu_ByteBuffer_putIntBe(struct Chabu_ByteBuffer_Data* data, int32 value){
	data->data[ data->position++ ] = value >> 24;
	data->data[ data->position++ ] = value >> 16;
	data->data[ data->position++ ] = value >>  8;
	data->data[ data->position++ ] = value >>  0;
}

void Chabu_ByteBuffer_putString(struct Chabu_ByteBuffer_Data* data, const char* const value);

static __inline__ uint8 Chabu_ByteBuffer_get(struct Chabu_ByteBuffer_Data* data){
	uint8 res = data->data[ data->position ];
	data->position++;
	return res;
}

void Chabu_ByteBuffer_compact(struct Chabu_ByteBuffer_Data* data);



#ifdef __cplusplus
}
#endif

#endif /* CHABU_SRC_CHABU_H_ */
