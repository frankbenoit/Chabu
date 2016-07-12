/*
 * FakeFunctions.h
 *
 *  Created on: 29.06.2016
 *      Author: Frank
 */

#ifndef FAKEFUNCTIONS_H_
#define FAKEFUNCTIONS_H_

#include <cstring>
#include "fff.h"
#include "Chabu.h"

DECLARE_FAKE_VALUE_FUNC4(enum Chabu_ErrorCode, acceptConnection,  void*, struct Chabu_ConnectionInfo_Data*, struct Chabu_ConnectionInfo_Data*, struct Chabu_ByteBuffer_Data* );
DECLARE_FAKE_VOID_FUNC5(errorFunction, void*, enum Chabu_ErrorCode, const char* , int , const char*  );
DECLARE_FAKE_VOID_FUNC2(eventNotification, void*, enum Chabu_Event  );
DECLARE_FAKE_VOID_FUNC4(channelEventNotification, void*, int, enum Chabu_Channel_Event, int32  );
DECLARE_FAKE_VALUE_FUNC2(struct Chabu_ByteBuffer_Data*, channelGetXmitBuffer, void*, int  );
DECLARE_FAKE_VALUE_FUNC2(struct Chabu_ByteBuffer_Data*, channelGetRecvBuffer, void*, int  );
DECLARE_FAKE_VALUE_FUNC2(int, networkRecvBuffer, void*, struct Chabu_ByteBuffer_Data* );
DECLARE_FAKE_VALUE_FUNC2(int, networkXmitBuffer, void*, struct Chabu_ByteBuffer_Data* );


void FakeFunctions_ResetAll();

#endif /* FAKEFUNCTIONS_H_ */
