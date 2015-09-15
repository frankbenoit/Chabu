
#ifndef __TRACE_RUNNER
#define __TRACE_RUNNER

#include <Common.h>
#include <Chabu.h>
#include <jansson.h>

#include "ByteBuffer.h"

struct TraceRunner_Data {

	const char* line;
	int ln;
	//BufferedReader br;
	struct Chabu_Data* chabu;
	
	int blockLineNum;

	struct ByteBuffer_Data bb;
	struct ByteBuffer_Data txBuf;
	uint8 bbMem[10000];
	uint8 txBufMem[10000];
};

void TraceRunner_Init(struct TraceRunner_Data* data, struct Chabu_Data* chabu) ;
#define TraceRunner_Ensure(_c,_f,...) TraceRunner_EnsureImpl(__FILE__,__LINE__,(_c),(_f),  __VA_ARGS__)
void TraceRunner_EnsureImpl( const char* file, long line, bool test, const char* fmt, ... );
//void TraceRunner_Run( struct TraceRunner_Data* data, struct Chabu_Data* givenChabu, Stream stream ) ;
void TraceRunner_WireRxAutoLength(struct TraceRunner_Data* data, const char* hexData) ;
void TraceRunner_WireRxHex(struct TraceRunner_Data* data, const char* hexData) ;
void TraceRunner_WireTxAutoLength(struct TraceRunner_Data* data, const char* hexData) ;
void TraceRunner_WireTxHex(struct TraceRunner_Data* data, const char* hexData) ;
void TraceRunner_WireTxHexMore(struct TraceRunner_Data* data,  int more, const char* hexData) ;
void TraceRunner_WireTx(struct TraceRunner_Data* data,  json_t* param_, struct ByteBuffer_Data* bb) ;
void TraceRunner_ChannelToApplHex(struct TraceRunner_Data* data, int channelId, const char* hexData) ;
void TraceRunner_ApplToChannelHex(struct TraceRunner_Data* data, int channelId, const char* hexData) ;
//static void TraceRunner_TestFile(const char* str)  ;
//static void TraceRunner_TestText(const char* str)  ;
//static void TraceRunner_TestText(struct Chabu_Data* givenChabu, const char* str)  ;
//static TraceRunner TraceRunner_Test(struct Chabu_Data* chabu) ;


#endif