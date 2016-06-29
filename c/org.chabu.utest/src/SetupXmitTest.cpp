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

static void configureStdSetup(){
	tdata.chabu = &chabu;
	tdata.userCallback_ChannelGetXmitBuffer = channelGetXmitBuffer;
	tdata.userCallback_ChannelXmitCompleted = channelXmitCompleted;
	tdata.userCallback_ChannelGetRecvBuffer = channelGetRecvBuffer;
	tdata.userCallback_ChannelRecvCompleted = channelRecvCompleted;
	tdata.channelId = 0;
	configureChannels_fake.custom_fake = configureChannels_Cfg1;
}

#define STD_SETUP()
#define ASSERT_NO_ERROR() ASSERT_EQ( Chabu_ErrorCode_OK_NOERROR, Chabu_LastError( &chabu )) << Chabu_LastErrorStr( &chabu )

TEST_F( SetupXmitTest, StdSetup_calls_read ){


	configureStdSetup();

	Chabu_Init(
			&chabu,
			0, APPL_NAME, RPS,
			channels, countof(channels),
			priorities, countof(priorities),
			errorFunction,
			configureChannels,
			networkRegisterWriteRequest,
			networkRecvBuffer,
			networkXmitBuffer,
			&tdata );

	ASSERT_NO_ERROR();
	//ASSERT_EQ( Chabu_ErrorCode_OK_NOERROR, Chabu_LastError( &chabu ));

	Chabu_HandleNetwork( &chabu );



}
