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
	Chabu_Channel_Event_DataAvailable,
	Chabu_Channel_Event_PreTransmit,
	Chabu_Channel_Event_Transmitted,
};

struct Chabu_Channel_Data;
struct Chabu_Data;

struct Chabu_Buffer {
	int  position;
	int  limit;
	int  capacity;
	uint8 * data;
};

typedef void (TChabu_UserCallback       )( void* userData, struct Chabu_Data*         chabuData, enum Chabu_Event         event );
typedef void (TChabu_ChannelUserCallback)( void* userData, struct Chabu_Channel_Data* chabuData, enum Chabu_Channel_Event event );

struct Chabu_ConnectionInfo_Data {
	uint16  receiveBufferSize;
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

#ifdef Chabu_USE_LOCK
	Chabu_LOCK_TYPE    lock;
#endif

	TChabu_UserCallback * userCallback;
	void                * userData;

	struct Chabu_Channel_Data* channels[Chabu_CHANNEL_COUNT_MAX];
	int                        channelCount;

	int    xmitChannelIdx;

	struct Chabu_Buffer recvBuffer;

	struct Chabu_Buffer xmitBuffer;
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

extern void Chabu_Init(
		struct Chabu_Data* data,

		int     applicationVersion,
		char*   applicationName,

		uint8*  recvBufferMemory,
		int recvBufferSize,

		uint8*  xmitBufferMemory,
		int xmitBufferSize,

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

//extern char* Chabu_GetLastErrorString( struct Chabu_Data* data );
//extern int   Chabu_GetLastErrorCode( struct Chabu_Data* data );
/////////////////////////////////////////////////////////
// Network <-> Chabu

/**
 * Called by the network connection to transfer received data into chabu and distribute it towards the channels.
 */
extern void Chabu_PutRecvData ( struct Chabu_Data* data, void* recvData, int length );

/**
 * Called by the network connection to retrieve data from chabu.
 */
extern bool Chabu_GetXmitData ( struct Chabu_Data* data, void* xmitData, int* xmitLength, int maxLength );


/////////////////////////////////////////////////////////
// Chabu Channel <-> Application
extern void Chabu_Channel_evUserRecvRequest( struct Chabu_Channel_Data* channel );
extern void Chabu_Channel_evUserXmitRequest( struct Chabu_Channel_Data* channel );



#endif /* CHABU_SRC_CHABU_H_ */
