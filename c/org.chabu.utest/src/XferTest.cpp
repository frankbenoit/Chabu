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
	int recvTestNumber;
	uint8 memTx[1000];
	struct Chabu_ByteBuffer_Data xmitBuffer;
	int xmitTestNumber;
	uint8 memTst[1000];
	struct Chabu_ByteBuffer_Data testBuffer;
};

static struct TestData tdata;
static struct Chabu_Data chabu;
static struct Chabu_Channel_Data channels[1];
static struct Chabu_Priority_Data priorities[1];

static void eventNotification_Cfg1( void* userData, enum Chabu_Event event ){
	if( event == Chabu_Event_InitChannels ){
		struct TestData* data = (struct TestData*)userData;
		struct Chabu_Data* chabu = data->chabu;
		Chabu_ConfigureChannel(chabu, data->channelId, 0,
				data->userCallback_ChannelEventNotification,
				data->userCallback_ChannelGetXmitBuffer,
				data->userCallback_ChannelGetRecvBuffer, NULL );
	}

}

static void networkRecvBufferImpl( void* userData, struct Chabu_ByteBuffer_Data* buffer ){
	Chabu_ByteBuffer_xferAllPossible( buffer, &tdata.recvBuffer );
}
static void networkXmitBufferImpl( void* userData, struct Chabu_ByteBuffer_Data* buffer ){
	Chabu_ByteBuffer_xferAllPossible( &tdata.xmitBuffer, buffer );
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
	tdata.xmitTestNumber = 0;

	Chabu_ByteBuffer_Init( &tdata.recvBuffer, tdata.memRx, sizeof(tdata.memRx) );
	tdata.recvBuffer.limit = tdata.recvBuffer.capacity;
	tdata.recvTestNumber = 0;

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
static void prepareRecvAccept(){
	Chabu_ByteBuffer_compact( &tdata.recvBuffer );
	Chabu_ByteBuffer_AppendHex( &tdata.recvBuffer, string("00 00 00 08 77 77 00 E1 "));
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
	prepareRecvAccept();
	ASSERT_NO_ERROR();
	Chabu_HandleNetwork( &chabu );

}

TEST( XferTest, startup_statesAreGood ){
	setup1Ch();

	EXPECT_EQ( Chabu_XmitState_Idle, chabu.xmit.state ) << ":: " << Chabu_XmitStateStr(chabu.xmit.state);
	EXPECT_EQ( Chabu_RecvState_Ready, chabu.recv.state ) << ":: " << Chabu_RecvStateStr(chabu.recv.state);
}

static void xmitAllowAll(){
	Chabu_ByteBuffer_clear( &tdata.xmitBuffer );
}
static void doIo(){
	Chabu_HandleNetwork( &chabu );
	Chabu_ByteBuffer_flip( &tdata.xmitBuffer );
}
static int xmitSize(){
	return Chabu_ByteBuffer_remaining( &tdata.xmitBuffer );
}

TEST( XferTest, notReadyForRecv_noArmXmitted ){
	setup1Ch();

	xmitAllowAll();
	doIo();
	EXPECT_EQ( 0, xmitSize() );
}

TEST( XferTest, readyForRecv_armXmitted ){
	setup1Ch();

	Chabu_Channel_AddRecvLimit( &chabu, 0, 22 );
	xmitAllowAll();
	doIo();
	EXPECT_EQ( 16, xmitSize() );
	EXPECT_EQ( 16, xmitSize() );
}

TEST( XferTest, readyForRecv_armXmittedWithRightFormat ){
	setup1Ch();

	Chabu_Channel_AddRecvLimit( &chabu, 0, 22 );
	xmitAllowAll();
	doIo();
	EXPECT_EQ( 16, xmitSize() );
	EXPECT_EQ( 16, Chabu_ByteBuffer_getInt_BE( &tdata.xmitBuffer) );
	EXPECT_EQ( 0x777700C3, Chabu_ByteBuffer_getInt_BE( &tdata.xmitBuffer) );
	EXPECT_EQ( 0, Chabu_ByteBuffer_getInt_BE( &tdata.xmitBuffer) ) << "--  channel ID";
	EXPECT_EQ( 22, Chabu_ByteBuffer_getInt_BE( &tdata.xmitBuffer) ) << "--  ARM";
}


TEST( XferTest, readyForRecv_writeRequestIsFired ){
	setup1Ch();

	RESET_FAKE(eventNotification);

	Chabu_Channel_AddRecvLimit( &chabu, 0, 22 );

	EXPECT_EQ( 1u, eventNotification_fake.call_count );
	EXPECT_EQ( Chabu_Event_NetworkRegisterWriteRequest, eventNotification_fake.arg1_val );

}

static void prepareSeqPacket( int channelId, int seq, int size){
	int alignedSize = Common_AlignUp4( size );
	int packetSize = alignedSize + 20;
	Chabu_ByteBuffer_compact( &tdata.recvBuffer );
	Chabu_ByteBuffer_putIntBe( &tdata.recvBuffer, packetSize );
	Chabu_ByteBuffer_AppendHex( &tdata.recvBuffer, string("77 77 00 B4 "));
	Chabu_ByteBuffer_putIntBe( &tdata.recvBuffer, channelId );
	Chabu_ByteBuffer_putIntBe( &tdata.recvBuffer, seq );
	Chabu_ByteBuffer_putIntBe( &tdata.recvBuffer, size );
	for( int i = 0; i < alignedSize; i += 4 ){
		Chabu_ByteBuffer_putIntBe( &tdata.recvBuffer, -1 );
	}
	Chabu_ByteBuffer_flip( &tdata.recvBuffer );
}

static void prepareArmPacket( int channelId, int arm){
	Chabu_ByteBuffer_compact( &tdata.recvBuffer );
	Chabu_ByteBuffer_putIntBe( &tdata.recvBuffer, 16 );
	Chabu_ByteBuffer_AppendHex( &tdata.recvBuffer, string("77 77 00 C3 "));
	Chabu_ByteBuffer_putIntBe( &tdata.recvBuffer, channelId );
	Chabu_ByteBuffer_putIntBe( &tdata.recvBuffer, arm );
	Chabu_ByteBuffer_flip( &tdata.recvBuffer );
}

static void channelAddRecv( int channel, int amount ){
	Chabu_Channel_AddRecvLimit( &chabu, channel, amount );
	xmitAllowAll();
	doIo();
}

TEST( XferTest, recvSeq_singleBigUserBuffer ){
	setup1Ch();

	channelAddRecv( 0, 22 );
	prepareSeqPacket(0, 0, 22);

	EXPECT_EQ( 0u, channelEventNotification_fake.call_count );
	EXPECT_EQ( 0u, channelGetRecvBuffer_fake.call_count );

	channelGetRecvBuffer_fake.return_val = &tdata.testBuffer;
	tdata.testBuffer.position = 0;
	tdata.testBuffer.limit = tdata.testBuffer.capacity;

	doIo();

	EXPECT_EQ( 22, tdata.testBuffer.position );
	EXPECT_EQ( 1u, channelEventNotification_fake.call_count );
	EXPECT_EQ( 0, channelEventNotification_fake.arg1_val );
	EXPECT_EQ( Chabu_Channel_Event_RecvCompleted, channelEventNotification_fake.arg2_val );
}

TEST( XferTest, recvSeq_multipleUserBuffers ){
	setup1Ch();

	channelAddRecv( 0, 22 );
	prepareSeqPacket(0, 0, 22);


	struct Chabu_ByteBuffer_Data* recvBufferPtr[3];
	struct Chabu_ByteBuffer_Data recvBuffer[3];
	Chabu_ByteBuffer_Init( &recvBuffer[0], tdata.memRx, sizeof(tdata.memRx) );
	Chabu_ByteBuffer_Init( &recvBuffer[1], tdata.memRx, sizeof(tdata.memRx) );
	Chabu_ByteBuffer_Init( &recvBuffer[2], tdata.memRx, sizeof(tdata.memRx) );

	recvBufferPtr[0] = &recvBuffer[0];
	recvBufferPtr[1] = &recvBuffer[1];
	recvBufferPtr[2] = &recvBuffer[2];
	channelGetRecvBuffer_fake.return_val_seq = recvBufferPtr;

	recvBuffer[0].position =  0; recvBuffer[0].limit =  8;
	recvBuffer[1].position =  8; recvBuffer[1].limit = 16;
	recvBuffer[2].position = 16; recvBuffer[2].limit = 22;

	channelGetRecvBuffer_fake.return_val_seq_len = 3;


	doIo();

	EXPECT_EQ(  8, recvBuffer[0].position );
	EXPECT_EQ( 16, recvBuffer[1].position );
	EXPECT_EQ( 22, recvBuffer[2].position );
	EXPECT_EQ( 3u, channelEventNotification_fake.call_count );
	EXPECT_EQ( 0, channelEventNotification_fake.arg1_val );
	EXPECT_EQ( Chabu_Channel_Event_RecvCompleted, channelEventNotification_fake.arg2_val );
}

#define VERIFY_ERROR_INFO( code, msg ) \
	do{\
		ASSERT_EQ( 1u, errorFunction_fake.call_count );\
		EXPECT_EQ( code, errorFunction_fake.arg1_val ) <<  Chabu_LastErrorStr( &chabu );\
		EXPECT_TRUE( strstr( errorFunction_fake.arg4_val, msg) != NULL ) << errorFunction_fake.arg4_val;\
	}while(false)


TEST( XferTest, recvSeq_userBufferSizeZero_errorUserBufferZeroLength ){
	setup1Ch();

	channelAddRecv( 0, 22 );
	prepareSeqPacket(0, 0, 22);

	EXPECT_EQ( 0u, channelEventNotification_fake.call_count );
	EXPECT_EQ( 0u, channelGetRecvBuffer_fake.call_count );

	channelGetRecvBuffer_fake.return_val = &tdata.testBuffer;
	tdata.testBuffer.position = 0;
	tdata.testBuffer.limit = 0;

	doIo();
	VERIFY_ERROR_INFO( Chabu_ErrorCode_RECV_USER_BUFFER_ZERO_LENGTH, "zero length");

}

TEST( XferTest, recvSeqWithWrongSeq_errorGenerated ){
	setup1Ch();

	channelAddRecv( 0, 22 );
	prepareSeqPacket(0, 0, 4);
	prepareSeqPacket(0, 11, 4);

	EXPECT_EQ( 0u, channelEventNotification_fake.call_count );
	EXPECT_EQ( 0u, channelGetRecvBuffer_fake.call_count );

	channelGetRecvBuffer_fake.return_val = &tdata.testBuffer;
	tdata.testBuffer.position = 0;
	tdata.testBuffer.limit = tdata.testBuffer.capacity;

	doIo();

	VERIFY_ERROR_INFO( Chabu_ErrorCode_PROTOCOL_SEQ_VALUE, "wrong SEQ");
}

TEST( XferTest, recvSeqWithNotExistingChannel_errorGenerated ){
	setup1Ch();

	channelAddRecv( 0, 22 );
	prepareSeqPacket(1, 0, 4);

	EXPECT_EQ( 0u, channelEventNotification_fake.call_count );
	EXPECT_EQ( 0u, channelGetRecvBuffer_fake.call_count );

	channelGetRecvBuffer_fake.return_val = &tdata.testBuffer;
	tdata.testBuffer.position = 0;
	tdata.testBuffer.limit = tdata.testBuffer.capacity;

	doIo();

	VERIFY_ERROR_INFO( Chabu_ErrorCode_PROTOCOL_CHANNEL_NOT_EXISTING, "ch[1] does not exist");
}

TEST( XferTest, recvMultipleSeq_assembleCorrectly ){
	setup1Ch();

	channelAddRecv( 0, 22 );
	prepareSeqPacket(0, 0, 2);
	prepareSeqPacket(0, 0, 12);
	prepareSeqPacket(0, 0, 2);
	prepareSeqPacket(0, 0, 1);
	prepareSeqPacket(0, 0, 3);
	prepareSeqPacket(0, 0, 2);

	channelGetRecvBuffer_fake.return_val = &tdata.testBuffer;
	tdata.testBuffer.position = 0;
	tdata.testBuffer.limit = tdata.testBuffer.capacity;

	doIo();

	EXPECT_EQ( 22, tdata.testBuffer.position );
	EXPECT_EQ(6u, channelEventNotification_fake.call_count );
	EXPECT_EQ( 0, channelEventNotification_fake.arg1_val );
	EXPECT_EQ( Chabu_Channel_Event_RecvCompleted, channelEventNotification_fake.arg2_val );
}

TEST( XferTest, receiveArm_valueIsStored ){

	setup1Ch();

	prepareArmPacket(0, 22);

	doIo();

	EXPECT_EQ( 22u, tdata.chabu->channels[0].xmitArm );
}

TEST( XferTest, startXmit_callsChannelGetXmitBuffer ){
	setup1Ch();

	prepareArmPacket( 0, 200 );
	doIo();

	RESET_FAKE(eventNotification);
	Chabu_Channel_AddXmitLimit( tdata.chabu, 0, 22 );

	EXPECT_EQ( 1u, eventNotification_fake.call_count );
	EXPECT_EQ( Chabu_Event_NetworkRegisterWriteRequest, eventNotification_fake.arg1_val );

	channelGetXmitBuffer_fake.return_val = &tdata.testBuffer;
	tdata.testBuffer.position = 0;
	tdata.testBuffer.limit = tdata.testBuffer.capacity;

	doIo();

	EXPECT_EQ( 1u, channelGetXmitBuffer_fake.call_count );
}


TEST( XferTest, DISABLED_receiveArm_wrongChannel_generatedError ){

}


TEST( XferTest, recvSeq_splitBetweenHeaderAndPayload ){
	setup1Ch();

	channelAddRecv( 0, 22 );
	prepareSeqPacket(0, 0, 5);
	// second packet, to ensure follow up is working
	prepareSeqPacket(0, 0, 5);

	channelGetRecvBuffer_fake.return_val = &tdata.testBuffer;
	tdata.testBuffer.position = 0;
	tdata.testBuffer.limit = tdata.testBuffer.capacity;

	ASSERT_EQ(  0, tdata.recvBuffer.position );
	ASSERT_EQ( 2*28, tdata.recvBuffer.limit );

	tdata.recvBuffer.limit = 20;
	doIo();
	tdata.recvBuffer.limit = 2*28;
	doIo();

	ASSERT_EQ( 10, tdata.testBuffer.position );
	ASSERT_NO_ERROR();

}
TEST( XferTest, recvSeq_splitInPayload ){
	setup1Ch();

	channelAddRecv( 0, 22 );
	prepareSeqPacket(0, 0, 5);
	// second packet, to ensure follow up is working
	prepareSeqPacket(0, 0, 5);

	channelGetRecvBuffer_fake.return_val = &tdata.testBuffer;
	tdata.testBuffer.position = 0;
	tdata.testBuffer.limit = tdata.testBuffer.capacity;

	ASSERT_EQ(  0, tdata.recvBuffer.position );
	ASSERT_EQ( 2*28, tdata.recvBuffer.limit );

	tdata.recvBuffer.limit = 22;
	doIo();
	tdata.recvBuffer.limit = 2*28;
	doIo();

	ASSERT_EQ( 10, tdata.testBuffer.position );
	ASSERT_NO_ERROR();
}
TEST( XferTest, recvSeq_splitBetweenPayloadAndPadding ){
	setup1Ch();

	channelAddRecv( 0, 22 );
	prepareSeqPacket(0, 0, 5);
	// second packet, to ensure follow up is working
	prepareSeqPacket(0, 0, 5);

	channelGetRecvBuffer_fake.return_val = &tdata.testBuffer;
	tdata.testBuffer.position = 0;
	tdata.testBuffer.limit = tdata.testBuffer.capacity;

	ASSERT_EQ(  0, tdata.recvBuffer.position );
	ASSERT_EQ( 2*28, tdata.recvBuffer.limit );

	tdata.recvBuffer.limit = 25;
	doIo();
	tdata.recvBuffer.limit = 2*28;
	doIo();

	ASSERT_EQ( 10, tdata.testBuffer.position );
	ASSERT_NO_ERROR();
}
TEST( XferTest, recvSeq_splitInPadding ){
	setup1Ch();

	channelAddRecv( 0, 22 );
	prepareSeqPacket(0, 0, 5);
	// second packet, to ensure follow up is working
	prepareSeqPacket(0, 0, 5);

	channelGetRecvBuffer_fake.return_val = &tdata.testBuffer;
	tdata.testBuffer.position = 0;
	tdata.testBuffer.limit = tdata.testBuffer.capacity;

	ASSERT_EQ(  0, tdata.recvBuffer.position );
	ASSERT_EQ( 2*28, tdata.recvBuffer.limit );

	tdata.recvBuffer.limit = 26;
	doIo();
	tdata.recvBuffer.limit = 2*28;
	doIo();

	ASSERT_EQ( 10, tdata.testBuffer.position );
	ASSERT_NO_ERROR();
}


TEST( XferTest, DISABLED_xmitSeq_splitBetweenHeaderAndPayload ){

}
TEST( XferTest, DISABLED_xmitSeq_splitInPayload ){

}
TEST( XferTest, DISABLED_xmitSeq_splitBetweenPayloadAndPadding ){

}
TEST( XferTest, DISABLED_xmitSeq_splitInPadding ){

}


TEST( PriotityTest, DISABLED_higherPrioGetsPrecedence ){

}

TEST( PriotityTest, DISABLED_samePrioIsRoundRobin ){

}

TEST( PriotityTest, DISABLED_lowerPrioGetsCalledWhenHigherLevelCompletedOneRound ){

}


