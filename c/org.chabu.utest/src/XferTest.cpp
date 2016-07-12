/*
 * XmitterTest.cpp
 *
 *  Created on: 12.07.2016
 *      Author: Frank
 */

#include "gtest/gtest.h"
#include "FakeFunctions.h"
#include <Chabu.h>
#include <string>
#include "Utils.h"
using std::string;

void SetUp(){
	FakeFunctions_ResetAll();
}

#define  RPS         0x200
#define APPL_VERSION 0x1234
#define APPL_NAME    "ABC"

struct TestData {
	struct Chabu_Data* chabu;
	int channelId;
	Chabu_ChannelEventNotification * userCallback_ChannelEventNotification;
	Chabu_ChannelGetXmitBuffer * userCallback_ChannelGetXmitBuffer;
	Chabu_ChannelGetRecvBuffer * userCallback_ChannelGetRecvBuffer;


	uint8 memRx[1000];
	struct Chabu_ByteBuffer_Data recvBuffer;
	uint8 memTx[1000];
	struct Chabu_ByteBuffer_Data xmitBuffer;
	uint8 memTst[1000];
	struct Chabu_ByteBuffer_Data testBuffer;
};

static struct TestData tdata;
static struct Chabu_Data chabu;
static struct Chabu_Channel_Data channels[1];
static struct Chabu_Priority_Data priorities[1];

static void eventNotification_Cfg1( void* userData, enum Chabu_Event event ){
	struct TestData* data = (struct TestData*)userData;
	struct Chabu_Data* chabu = data->chabu;
	Chabu_ConfigureChannel(chabu, data->channelId, 0,
			data->userCallback_ChannelEventNotification,
			data->userCallback_ChannelGetXmitBuffer,
			data->userCallback_ChannelGetRecvBuffer, NULL );

}

static int networkRecvBufferImpl( void* userData, struct Chabu_ByteBuffer_Data* buffer ){
	return Chabu_ByteBuffer_xferAllPossible( buffer, &tdata.recvBuffer );
}
static int networkXmitBufferImpl( void* userData, struct Chabu_ByteBuffer_Data* buffer ){
	return Chabu_ByteBuffer_xferAllPossible( &tdata.xmitBuffer, buffer );
}

static void configureStdSetup(){
	FakeFunctions_ResetAll();
	tdata.chabu = &chabu;
	tdata.userCallback_ChannelEventNotification = channelEventNotification;
	tdata.userCallback_ChannelGetXmitBuffer = channelGetXmitBuffer;
	tdata.userCallback_ChannelGetRecvBuffer = channelGetRecvBuffer;
	tdata.channelId = 0;
	eventNotification_fake.custom_fake = eventNotification_Cfg1;

	Chabu_ByteBuffer_Init( &tdata.xmitBuffer, tdata.memTx, sizeof(tdata.memTx) );
	tdata.xmitBuffer.limit = tdata.xmitBuffer.capacity;

	Chabu_ByteBuffer_Init( &tdata.recvBuffer, tdata.memRx, sizeof(tdata.memRx) );
	tdata.recvBuffer.limit = tdata.recvBuffer.capacity;

	Chabu_ByteBuffer_Init( &tdata.testBuffer, tdata.memTst, sizeof(tdata.memTst) );
	tdata.recvBuffer.limit = 0;

	networkRecvBuffer_fake.custom_fake = networkRecvBufferImpl;
	networkXmitBuffer_fake.custom_fake = networkXmitBufferImpl;

	acceptConnection_fake.return_val = Chabu_ErrorCode_OK_NOERROR;
}
static void prepareRecvSetup(){
	Chabu_ByteBuffer_compact( &tdata.recvBuffer );
	Chabu_ByteBuffer_AppendHex( &tdata.recvBuffer, string("00 00 00 28 77 77 00 F0 "));
	Chabu_ByteBuffer_AppendHex( &tdata.recvBuffer, string("00 00 00 05 43 48 41 42 55 00 00 00 "));
	Chabu_ByteBuffer_putIntBe( &tdata.recvBuffer, Chabu_ProtocolVersion );
	Chabu_ByteBuffer_putIntBe( &tdata.recvBuffer, RPS );
	Chabu_ByteBuffer_putIntBe( &tdata.recvBuffer, APPL_VERSION);
	Chabu_ByteBuffer_AppendHex( &tdata.recvBuffer, string("00 00 00 03 41 42 43 00 "));

	Chabu_ByteBuffer_flip( &tdata.recvBuffer );

}

#define ASSERT_NO_ERROR() ASSERT_EQ( Chabu_ErrorCode_OK_NOERROR, Chabu_LastError( &chabu )) << Chabu_LastErrorStr( &chabu )

static void allowNoXmit(){
	tdata.xmitBuffer.limit = tdata.xmitBuffer.position;
}

static void allowToXmit( int size ){
	tdata.xmitBuffer.limit += size;
}


static void setup1Ch(){

	configureStdSetup();

	Chabu_Init(
			&chabu,
			APPL_VERSION, APPL_NAME, RPS,
			channels, countof(channels),
			priorities, countof(priorities),
			errorFunction,
			acceptConnection,
			eventNotification,
			networkRecvBuffer,
			networkXmitBuffer,
			&tdata );

	Chabu_HandleNetwork( &chabu );
	allowNoXmit();
	allowToXmit(8);
	prepareRecvSetup();
	ASSERT_NO_ERROR();
	Chabu_HandleNetwork( &chabu );

}

TEST( XmitterTest, DISABLED_receiveWrongApplName ){
	setup1Ch();
	allowToXmit(16);
	Chabu_HandleNetwork( &chabu );

}



