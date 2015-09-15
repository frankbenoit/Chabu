#ifndef TESTCHANNELUSER_H
#define TESTCHANNELUSER_H

#include <Common.h>
#include <ByteBuffer.h>

struct TestChannelUser_Data {
	struct ByteBuffer_Data rx;
	uint8 rxQueueBuffer[1000];

	struct ByteBuffer_Data tx;
	uint8 txQueueBuffer[1000];
	
    struct Chabu_Channel_Data* channel;
    bool consumeRxInProgress;
	int channelId;

};

extern void TestChannelUser_SetChannel(struct TestChannelUser_Data* data, struct Chabu_Channel_Data* channel);
extern void TestChannelUser_Init( struct TestChannelUser_Data* data, int channelId );
extern void TestChannelUser_ConsumeRxData(struct TestChannelUser_Data* data, struct ByteBuffer_Data* bb );
extern void TestChannelUser_AddTxData(struct TestChannelUser_Data* data, struct ByteBuffer_Data* bb );
extern void TestChannelUser_RecvEvent(struct TestChannelUser_Data* data, struct Chabu_Channel_Data* channel);
extern bool TestChannelUser_XmitEvent(struct TestChannelUser_Data* data, struct Chabu_Channel_Data* channel);
#endif