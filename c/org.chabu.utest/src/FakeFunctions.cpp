/*
 * FakeFunctions.cpp
 *
 *  Created on: 29.06.2016
 *      Author: Frank
 */

#include "FakeFunctions.h"
//DECLARE_FAKE_VOID_FUNC0

DEFINE_FFF_GLOBALS;

DEFINE_FAKE_VALUE_FUNC4(enum Chabu_ErrorCode, acceptConnection,  void*, struct Chabu_ConnectionInfo_Data*, struct Chabu_ConnectionInfo_Data*, struct Chabu_ByteBuffer_Data* );
DEFINE_FAKE_VOID_FUNC5(errorFunction, void*, enum Chabu_ErrorCode, const char* , int , const char*  );
DEFINE_FAKE_VOID_FUNC2(eventNotification, void*, enum Chabu_Event );
DEFINE_FAKE_VOID_FUNC2(networkRecvBuffer, void*, struct Chabu_ByteBuffer_Data* );
DEFINE_FAKE_VOID_FUNC2(networkXmitBuffer, void*, struct Chabu_ByteBuffer_Data* );

DEFINE_FAKE_VOID_FUNC4(channelEventNotification, void*, int, enum Chabu_Channel_Event, int32  );
DEFINE_FAKE_VALUE_FUNC3(struct Chabu_ByteBuffer_Data*, channelGetXmitBuffer, void*, int, int  );
DEFINE_FAKE_VALUE_FUNC3(struct Chabu_ByteBuffer_Data*, channelGetRecvBuffer, void*, int, int  );

void FakeFunctions_ResetAll(){

	RESET_FAKE(acceptConnection);
	RESET_FAKE(errorFunction);
	RESET_FAKE(networkRecvBuffer);
	RESET_FAKE(networkXmitBuffer);
	RESET_FAKE(eventNotification);

	RESET_FAKE(channelEventNotification);
	RESET_FAKE(channelGetXmitBuffer);
	RESET_FAKE(channelGetRecvBuffer);

}
