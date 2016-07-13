/*
 * SetupXmitTest.cpp
 *
 *  Created on: 29.06.2016
 *      Author: Frank
 */

#include "gtest/gtest.h"
#include "FakeFunctions.h"
#include <Chabu.h>
#include <string>
#include "Utils.h"
using std::string;

static void SetUp(){
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

static void networkRecvBufferImpl( void* userData, struct Chabu_ByteBuffer_Data* buffer ){
	Chabu_ByteBuffer_xferAllPossible( buffer, &tdata.recvBuffer );
}
static void networkXmitBufferImpl( void* userData, struct Chabu_ByteBuffer_Data* buffer ){
	Chabu_ByteBuffer_xferAllPossible( &tdata.xmitBuffer, buffer );
}

static void configureStdSetup(){
	SetUp();
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

#define ASSERT_NO_ERROR() ASSERT_EQ( Chabu_ErrorCode_OK_NOERROR, Chabu_LastError( &chabu )) << Chabu_LastErrorStr( &chabu )

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

	ASSERT_NO_ERROR();

}


TEST( SetupXmitTest, sendsSetup_callsNetworkRecvAndXmit ){
	setup1Ch();
	//ASSERT_EQ( Chabu_ErrorCode_OK_NOERROR, Chabu_LastError( &chabu ));

	Chabu_HandleNetwork( &chabu );

	EXPECT_LE( 1u, networkRecvBuffer_fake.call_count );
	EXPECT_LE( 1u, networkXmitBuffer_fake.call_count );

}

TEST( SetupXmitTest, sendsSetup_hasCorrectLength ){

	setup1Ch();
	Chabu_HandleNetwork( &chabu );
	EXPECT_EQ( 0x28, tdata.xmitBuffer.position );

}

TEST( SetupXmitTest, sendsSetup_withCorrectContent ){

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

TEST( SetupXmitTest, sendsSetup_cannotFullySend_registersWriteRequest ){

	setup1Ch();
	tdata.xmitBuffer.limit = 20;
	Chabu_HandleNetwork( &chabu );

	// expect 2 calls, one for init channels, then the netw write request
	EXPECT_EQ( 2u, eventNotification_fake.call_count );
	EXPECT_EQ( Chabu_Event_NetworkRegisterWriteRequest, eventNotification_fake.arg1_history[1] );

}

TEST( SetupXmitTest, sendsSetup_cannotFullySend_completesOnNextRound ){

	setup1Ch();
	tdata.xmitBuffer.limit = 20;
	Chabu_HandleNetwork( &chabu );
	tdata.xmitBuffer.limit = 50;
	Chabu_HandleNetwork( &chabu );

	EXPECT_EQ( 0x28, tdata.xmitBuffer.position );

}

TEST( SetupXmitTest, sendsSetup_thenWaitsRemoteSetup ){

	setup1Ch();
	Chabu_HandleNetwork( &chabu );
	EXPECT_EQ( Chabu_XmitState_Setup, chabu.xmit.state );

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

TEST( SetupRecvTest, receiveGood_byFirstSendingThenRecv_ConsumedAllSetup ){
	setup1Ch();
	Chabu_HandleNetwork( &chabu );

	prepareRecvSetup();
	Chabu_HandleNetwork( &chabu );

	EXPECT_FALSE( Chabu_ByteBuffer_hasRemaining( &tdata.recvBuffer ) );

}

TEST( SetupRecvTest, receiveGood_byFirstSendingThenRecv_ChangedToIdleState ){
	setup1Ch();
	Chabu_HandleNetwork( &chabu );

	prepareRecvSetup();
	Chabu_HandleNetwork( &chabu );
	EXPECT_EQ( Chabu_XmitState_Idle, chabu.xmit.state ) << ":: " << Chabu_XmitStateStr( chabu.xmit.state );

}

static void allowNoXmit(){
	tdata.xmitBuffer.limit = 0;
}

static void allowToXmit( int size ){
	tdata.xmitBuffer.limit += size;
}

TEST( SetupRecvTest, receiveGood_byFirstRecvThenSending_ChangedToIdleState ){
	setup1Ch();

	allowNoXmit();
	prepareRecvSetup();

	Chabu_HandleNetwork( &chabu );

	EXPECT_FALSE( Chabu_ByteBuffer_hasRemaining( &tdata.recvBuffer ) );

	allowToXmit( 50 );
	Chabu_HandleNetwork( &chabu );

	EXPECT_EQ( Chabu_XmitState_Idle, chabu.xmit.state );

}

TEST( SetupRecvTest, recvSetupAndAskUserToAccept_IsCallingUser ){
	setup1Ch();

	prepareRecvSetup();
	Chabu_HandleNetwork( &chabu );

	EXPECT_EQ( 1u, acceptConnection_fake.call_count );
}

TEST( SetupRecvTest, recvSetupAndAskUserToAccept_UserRejects_AbortIsSend ){
	setup1Ch();

	acceptConnection_fake.return_val = Chabu_ErrorCode_UNKNOWN;

	prepareRecvSetup();
	Chabu_HandleNetwork( &chabu );

	EXPECT_EQ( Chabu_XmitState_Abort, chabu.xmit.state );
}

// DISABLED_

TEST( AbortXmitTest, abortOnUserReject_AbortHasRightFormat ){
	setup1Ch();

	acceptConnection_fake.return_val = Chabu_ErrorCode_UNKNOWN;

	Chabu_HandleNetwork( &chabu );
	Chabu_ByteBuffer_clear( &tdata.xmitBuffer );

	prepareRecvSetup();
	Chabu_HandleNetwork( &chabu );

	EXPECT_EQ( Chabu_XmitState_Abort, chabu.xmit.state );
	Chabu_ByteBuffer_flip( &tdata.xmitBuffer );
	int packetLength = Chabu_ByteBuffer_getInt_BE( &tdata.xmitBuffer );
	EXPECT_TRUE( packetLength >= 16 ) << "packetLength: " << packetLength;
	EXPECT_TRUE( packetLength <= 64 ) << "packetLength: " << packetLength;
	int packetType   = Chabu_ByteBuffer_getInt_BE( &tdata.xmitBuffer );

	EXPECT_EQ( 0x777700D2, packetType );
	int error = Chabu_ByteBuffer_getInt_BE( &tdata.xmitBuffer );
	EXPECT_NE( 0, error );
	int stringLen = Chabu_ByteBuffer_getInt_BE( &tdata.xmitBuffer );
	int padding = packetLength - 16 - stringLen;
	EXPECT_TRUE( padding >= 0 ) << "value: " << padding;
	EXPECT_TRUE( padding < 4 ) << "value: " << padding;

}

TEST( SetupRecvTest, DISABLED_receiveWrongChabuVersion ){
	setup1Ch();
}

TEST( SetupRecvTest, DISABLED_receiveWrongChabuName ){
	setup1Ch();
}

TEST( SetupRecvTest, DISABLED_receiveWrongApplVersion ){
	setup1Ch();
}

TEST( SetupRecvTest, DISABLED_receiveWrongApplName ){
	setup1Ch();
}


