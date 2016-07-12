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
DEFINE_FAKE_VOID_FUNC1(configureChannels, void*  );
DEFINE_FAKE_VOID_FUNC1(networkRegisterWriteRequest, void*  );
DEFINE_FAKE_VALUE_FUNC2(int, networkRecvBuffer, void*, struct Chabu_ByteBuffer_Data* );
DEFINE_FAKE_VALUE_FUNC2(int, networkXmitBuffer, void*, struct Chabu_ByteBuffer_Data* );

DEFINE_FAKE_VOID_FUNC4(channelEvent, void*, int, enum Chabu_Channel_Event, int32  );
DEFINE_FAKE_VALUE_FUNC2(struct Chabu_ByteBuffer_Data*, channelGetXmitBuffer, void*, int  );
DEFINE_FAKE_VALUE_FUNC2(struct Chabu_ByteBuffer_Data*, channelGetRecvBuffer, void*, int  );

void FakeFunctions_ResetAll(){

	RESET_FAKE(acceptConnection);
	RESET_FAKE(errorFunction);
	RESET_FAKE(configureChannels);
	RESET_FAKE(networkRecvBuffer);
	RESET_FAKE(networkXmitBuffer);
	RESET_FAKE(networkRegisterWriteRequest);

	RESET_FAKE(channelEvent);
	RESET_FAKE(channelGetXmitBuffer);
	RESET_FAKE(channelGetRecvBuffer);

}
