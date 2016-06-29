/*
 * FakeFunctions.cpp
 *
 *  Created on: 29.06.2016
 *      Author: Frank
 */

#include "FakeFunctions.h"
//DECLARE_FAKE_VOID_FUNC0

DEFINE_FFF_GLOBALS;

DEFINE_FAKE_VOID_FUNC5(errorFunction, void*, enum Chabu_ErrorCode, const char* , int , const char*  );
DEFINE_FAKE_VOID_FUNC1(configureChannels, void*  );
DEFINE_FAKE_VOID_FUNC1(networkRegisterWriteRequest, void*  );
DEFINE_FAKE_VALUE_FUNC2(int, networkRecvBuffer, void*, struct Buffer* );
DEFINE_FAKE_VALUE_FUNC2(int, networkXmitBuffer, void*, struct Buffer* );

DEFINE_FAKE_VALUE_FUNC1(struct Buffer*, channelGetXmitBuffer, void*  );
DEFINE_FAKE_VOID_FUNC1(channelXmitCompleted, void*  );
DEFINE_FAKE_VALUE_FUNC1(struct Buffer*, channelGetRecvBuffer, void*  );
DEFINE_FAKE_VOID_FUNC1(channelRecvCompleted, void*  );

void FakeFunctions_ResetAll(){

	RESET_FAKE(errorFunction);
	RESET_FAKE(configureChannels);
	RESET_FAKE(networkRecvBuffer);
	RESET_FAKE(networkXmitBuffer);
	RESET_FAKE(networkRegisterWriteRequest);

	RESET_FAKE(channelGetXmitBuffer);
	RESET_FAKE(channelXmitCompleted);
	RESET_FAKE(channelGetRecvBuffer);
	RESET_FAKE(channelRecvCompleted);

}
