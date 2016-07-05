/*
 * SetupXmitTest.cpp
 *
 *  Created on: 29.06.2016
 *      Author: Frank
 */

#include "SetupXmitTest.hpp"
#include "gtest/gtest.h"
#include "FakeFunctions.h"
#include <Chabu.h>
#include <string>
#include "Utils.h"
using std::string;

void SetupXmitTest::SetUp(){
	FakeFunctions_ResetAll();

}

#define  RPS         0x200
#define APPL_VERSION 0x1234
#define APPL_NAME    "ABC"

struct TestData {
	struct Chabu_Data* chabu;
	int channelId;
	Chabu_ChannelGetXmitBuffer * userCallback_ChannelGetXmitBuffer;
	Chabu_ChannelXmitCompleted * userCallback_ChannelXmitCompleted;
	Chabu_ChannelGetRecvBuffer * userCallback_ChannelGetRecvBuffer;
	Chabu_ChannelRecvCompleted * userCallback_ChannelRecvCompleted;


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

static void configureChannels_Cfg1( void* userData ){
	struct TestData* data = (struct TestData*)userData;
	struct Chabu_Data* chabu = data->chabu;
	Chabu_ConfigureChannel(chabu, data->channelId, 0,
			data->userCallback_ChannelGetXmitBuffer,
			data->userCallback_ChannelXmitCompleted,
			data->userCallback_ChannelGetRecvBuffer,
			data->userCallback_ChannelRecvCompleted, NULL );

}

static int networkRecvBufferImpl( void* userData, struct Chabu_ByteBuffer_Data* buffer ){
	return Chabu_ByteBuffer_xferAllPossible( buffer, &tdata.recvBuffer );
}
static int networkXmitBufferImpl( void* userData, struct Chabu_ByteBuffer_Data* buffer ){
	return Chabu_ByteBuffer_xferAllPossible( &tdata.xmitBuffer, buffer );
}

static void configureStdSetup(){
	tdata.chabu = &chabu;
	tdata.userCallback_ChannelGetXmitBuffer = channelGetXmitBuffer;
	tdata.userCallback_ChannelXmitCompleted = channelXmitCompleted;
	tdata.userCallback_ChannelGetRecvBuffer = channelGetRecvBuffer;
	tdata.userCallback_ChannelRecvCompleted = channelRecvCompleted;
	tdata.channelId = 0;
	configureChannels_fake.custom_fake = configureChannels_Cfg1;

	Chabu_ByteBuffer_Init( &tdata.xmitBuffer, tdata.memTx, sizeof(tdata.memTx) );
	tdata.xmitBuffer.limit = tdata.xmitBuffer.capacity;

	Chabu_ByteBuffer_Init( &tdata.recvBuffer, tdata.memRx, sizeof(tdata.memRx) );
	tdata.recvBuffer.limit = tdata.recvBuffer.capacity;

	Chabu_ByteBuffer_Init( &tdata.testBuffer, tdata.memTst, sizeof(tdata.memTst) );
	tdata.recvBuffer.limit = tdata.testBuffer.capacity;

	networkRecvBuffer_fake.custom_fake = networkRecvBufferImpl;
	networkXmitBuffer_fake.custom_fake = networkXmitBufferImpl;
}

#define ASSERT_NO_ERROR() ASSERT_EQ( Chabu_ErrorCode_OK_NOERROR, Chabu_LastError( &chabu )) << Chabu_LastErrorStr( &chabu )

static void setup1Ch(){

	configureStdSetup();

	Chabu_Init(
			&chabu,
			APPL_VERSION, APPL_NAME, RPS,
			channels, countof(channels),
			priorities, countof(priorities),
			errorFunction,
			configureChannels,
			networkRegisterWriteRequest,
			networkRecvBuffer,
			networkXmitBuffer,
			&tdata );

	ASSERT_NO_ERROR();

}


TEST_F( SetupXmitTest, sendsSetup_callsNetworkRecvAndXmit ){
	setup1Ch();
	//ASSERT_EQ( Chabu_ErrorCode_OK_NOERROR, Chabu_LastError( &chabu ));

	Chabu_HandleNetwork( &chabu );

	EXPECT_LE( 1u, networkRecvBuffer_fake.call_count );
	EXPECT_LE( 1u, networkXmitBuffer_fake.call_count );

}

TEST_F( SetupXmitTest, sendsSetup_hasCorrectLength ){

	setup1Ch();
	Chabu_HandleNetwork( &chabu );
	EXPECT_EQ( 0x28, tdata.xmitBuffer.position );

}

TEST_F( SetupXmitTest, sendsSetup_withCorrectContent ){

	setup1Ch();
	Chabu_HandleNetwork( &chabu );
	Chabu_ByteBuffer_AppendHex( &tdata.testBuffer, string("00 00 00 28 77 77 00 F0 "));
	Chabu_ByteBuffer_AppendHex( &tdata.testBuffer, string("00 00 00 05 43 48 41 42 55 00 00 00 "));
	Chabu_ByteBuffer_putIntBe( &tdata.testBuffer, Chabu_ProtocolVersion );
	Chabu_ByteBuffer_putIntBe( &tdata.testBuffer, RPS );
	Chabu_ByteBuffer_putIntBe( &tdata.testBuffer, APPL_VERSION);
	Chabu_ByteBuffer_AppendHex( &tdata.testBuffer, string("00 00 00 03 41 42 43 00 "));

	Chabu_ByteBuffer_flip( &tdata.testBuffer );
	Chabu_ByteBuffer_flip( &tdata.xmitBuffer );
	VerifyContent( &tdata.testBuffer, &tdata.xmitBuffer );

}

TEST_F( SetupXmitTest, sendsSetup_cannotFullySend_registersWriteRequest ){

	setup1Ch();
	tdata.xmitBuffer.limit = 20;
	Chabu_HandleNetwork( &chabu );

	EXPECT_EQ( 1u, networkRegisterWriteRequest_fake.call_count );

}

TEST_F( SetupXmitTest, sendsSetup_cannotFullySend_completesOnNextRound ){

	setup1Ch();
	tdata.xmitBuffer.limit = 20;
	Chabu_HandleNetwork( &chabu );
	tdata.xmitBuffer.limit = 50;
	Chabu_HandleNetwork( &chabu );

	EXPECT_EQ( 0x28, tdata.xmitBuffer.position );

}





