//============================================================================
// Name        : utest.cpp
// Author      : 
// Version     :
// Copyright   : Your copyright notice
// Description : Hello World in C++, Ansi-style
//============================================================================

#include "gtest/gtest.h"
#include <fcntl.h>

int main(int argc, char **argv) {
  ::testing::InitGoogleTest(&argc, argv);
  return RUN_ALL_TESTS();
}



#include <Chabu.h>

class ChabuTest : public ::testing::Test {

};

#define APPL_VERSION 0x1234
#define APPL_NAME    "ABC"

/*

typedef struct Buffer* (CALL_SPEC Chabu_ChannelGetXmitBuffer)( void* userData );
typedef void           (CALL_SPEC Chabu_ChannelXmitCompleted)( void* userData );
typedef struct Buffer* (CALL_SPEC Chabu_ChannelGetRecvBuffer)( void* userData );
typedef void           (CALL_SPEC Chabu_ChannelRecvCompleted)( void* userData );
*/

void  CALL_SPEC errorFunction               ( void* userData, enum Chabu_ErrorCode code, const char* file, int line, const char* fmt, ... ){

}
void  CALL_SPEC configureChannels           ( void* userData ){

}
void  CALL_SPEC networkRegisterWriteRequest ( void* userData ){

}
int   CALL_SPEC networkRecvBuffer           ( void* userData, struct Buffer* buffer ){
	return 0;
}
int   CALL_SPEC networkXmitBuffer           ( void* userData, struct Buffer* buffer ){
	return 0;
}


TEST( ChabuInit, LastError_set_for_chabu_NULL ){
	EXPECT_EQ( Chabu_ErrorCode_CHABU_IS_NULL, Chabu_LastError( NULL ));
}

TEST( ChabuInit, LastError_set_for_chabu_uninitialized ){
	struct Chabu_Data chabu;
	memset( &chabu, 0, sizeof(chabu));
	EXPECT_EQ( Chabu_ErrorCode_CHABU_IS_NOT_INITIALIZED, Chabu_LastError( &chabu ));
}

TEST( ChabuInit, LastError_set_for_chabu_uninitialized ){
	struct Chabu_Data chabu;
	memset( &chabu, 0, sizeof(chabu));
	EXPECT_EQ( Chabu_ErrorCode_CHABU_IS_NOT_INITIALIZED, Chabu_LastError( &chabu ));
}


TEST( ChabuTest, AssertIsCalled ){

	struct Chabu_Data chabu;
	struct Chabu_Channel_Data channels[3];
	struct Chabu_Priority_Data priorities[3];

	Chabu_Init(
			&chabu,
			APPL_VERSION, APPL_NAME,
			channels, countof(channels),
			priorities, countof(priorities),
			errorFunction, configureChannels, networkRegisterWriteRequest, networkRecvBuffer, networkXmitBuffer,
			NULL );

}


