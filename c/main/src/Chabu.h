/*
 * Chabu.h
 *
 *  Created on: 11.12.2014
 *      Author: Frank
 */

#ifndef CHABU_SRC_CHABU_H_
#define CHABU_SRC_CHABU_H_

#include "ChabuOpts.h"

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

#define Chabu_APPLICATION_NAME_SIZE_MAX 256
#define Chabu_HEADER_SIZE_MAX 10
#define Chabu_HEADER_ARM_SIZE  8
#define Chabu_HEADER_SEQ_SIZE 10

enum Chabu_Event {
	Chabu_Event_DataAvailable,
	Chabu_Event_CanTransmit,
	Chabu_Event_Connecting,
};

enum Chabu_Channel_Event {
	Chabu_Channel_Event_DataAvailable,
	Chabu_Channel_Event_PreTransmit,
	Chabu_Channel_Event_Transmitted,
};

struct Chabu_Channel_Data;
struct Chabu_Data;

typedef void (TChabu_ChannelUserCallback)( void* userData, struct Chabu_Channel_Data* chabuData, enum Chabu_Channel_Event event );
typedef void (TChabu_UserCallback       )( void* userData, struct Chabu_Data*         chabuData, enum Chabu_Event         event );

struct Chabu_ConnectionInfo_Data {
	int     index;
	int     length;
	uint8   chabuProtocolVersion;
	bool    byteOrderBigEndian;
	union {
		struct {
			uint16  maxReceivePayloadSize;
			uint16  receiveChannelCount;
			uint32  applicationVersion;
			uint8   applicationNameLength;
			char    applicationName[Chabu_APPLICATION_NAME_SIZE_MAX];
		} v1;
	};
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
	bool             xmitRequest;

	bool             recvArmShouldBeXmit;

	struct QueueVar* recvQueue;
	uint32           recvSeq;
	uint32           recvArm;
	bool             recvRequest;

};

struct Chabu_Data {

	struct Chabu_ConnectionInfo_Data connectionInfoLocal;
	struct Chabu_ConnectionInfo_Data connectionInfoRemote;
	char   applicationNameRemote[Chabu_APPLICATION_NAME_SIZE_MAX];

#ifdef Chabu_USE_LOCK
	Chabu_LOCK_TYPE    lock;
#endif

	TChabu_UserCallback * userCallback;
	void                * userData;

	struct Chabu_Channel_Data* channels[Chabu_CHANNEL_COUNT_MAX];
	int                        channelCount;

	int    xmitChannelIdx;
	int    recvChannelIdx;

	uint8              recvHeaderBuffer[Chabu_HEADER_SIZE_MAX];
	/// Until which idx the data is until now consumed.
	int                recvLastIndex;
	/// Until which idx the data shall be consumed.
	int                recvLastLength;

	uint8              xmitHeaderBuffer[Chabu_HEADER_SIZE_MAX];
	int                xmitLastIndex;
	int                xmitLastLength;

	bool startupRx; // TODO: true before what is initialized?
	bool startupTx; // TODO: true before what is initialized?
	bool activated; // true when initialization (TODO: of chabu protocol?) is completed

	struct Chabu_Channel_Data* recvContinueChannel;

	struct Chabu_Channel_Data* xmitContinueChannel;
	uint16                     xmitMaxPayloadSize;

};

extern void Chabu_Init(
		struct Chabu_Data* data,
		int    applicationVersion,
		char*  applicationName,
		int    maxReceivePayloadSize,
		TChabu_UserCallback * userCallback,
		void * userData );

extern void Chabu_Init_AddChannel (
		struct Chabu_Data* data,
		struct Chabu_Channel_Data* channel,
		TChabu_ChannelUserCallback * userCallback, void * userData,
		int priority,
		struct QueueVar* recvQueue,
		struct QueueVar* xmitQueue );

extern void Chabu_Init_Complete ( struct Chabu_Data* data );
/////////////////////////////////////////////////////////
// Network <-> Chabu

// RX
extern void Chabu_PutRecvData ( struct Chabu_Data* data, void* recvData, int length );

// TX
extern bool Chabu_GetXmitData ( struct Chabu_Data* data, void* xmitData, int* xmitLength, int maxLength );


/////////////////////////////////////////////////////////
// Chabu Channel <-> Application
extern void Chabu_Channel_evUserRecvRequest( struct Chabu_Channel_Data* channel );
extern void Chabu_Channel_evUserXmitRequest( struct Chabu_Channel_Data* channel );



#endif /* CHABU_SRC_CHABU_H_ */
