// ChabuCTest.cpp : Definiert den Einstiegspunkt für die Konsolenanwendung.
//

//#include "stdafx.h"
#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>

#include <Common.h>
#include <Chabu.h>
#include <QueueVar.h>

#include "TestUtil.h"
#include "TraceRunner.h"
#include "TestChannelUser.h"
#include "DataGen.h"

//typedef void (TChabu_UserCallback       )( void* userData, struct Chabu_Data*         chabuData, enum Chabu_Event         event );
//typedef void (TChabu_ChannelUserCallback)( void* userData, struct Chabu_Channel_Data* chabuData, enum Chabu_Channel_Event event );

#define APPLNAME_MAX 	"12345678901234567890123456789012345678901234567890123456"
		
static void setupTraceRunner() ;

static enum Chabu_Event lastEvent;
static enum Chabu_ErrorCode expectCode = Chabu_ErrorCode_OK_NOERROR;
static struct Chabu_Data chabu;
static struct Chabu_Channel_Data channels[5];
static struct TraceRunner_Data r;

static uint8 rx_buf[0x1000];
static uint8 tx_buf[0x1000];

static struct QueueVar rxQueues[10];
static struct QueueVar txQueues[10];
static uint8 rxQueueBuffers[1000][10];
static uint8 txQueueBuffers[1000][10];

static void testExit(){
	getchar(); 
	exit(EXIT_FAILURE);
}

#define EXPECT_ERROR_CODE(_code) do{					\
	expectCode = _code;									\
} while(0)

#define VERIFY_ERROR_CODE() do{											\
		if( expectCode != Chabu_ErrorCode_OK_NOERROR ){							\
			printf( "Error code not set loc=%s(%d)\n", __FILE__, __LINE__);		\
			testExit();															\
		}																		\
	} while(0)

void __cdecl Chabu_AssertFunction( enum Chabu_ErrorCode code, void* userData, struct Chabu_Data* chabuData, const char* file, int line, const char* fmt, ... ){
    va_list myargs;
	
	if( expectCode == code ){
		expectCode = Chabu_ErrorCode_OK_NOERROR;
		return;
	}

	va_start(myargs, fmt);
    vprintf(fmt, myargs);
    va_end(myargs);
	printf("\n");
	printf("Assert in code=%d %s %d\n", code, file, line );
}

void __cdecl QueueVar_AssertFunction( struct QueueVar* queue, const char* file, int line, const char* fmt, ... ){
    va_list myargs;
    va_start(myargs, fmt);
    vprintf(fmt, myargs);
    va_end(myargs);
	printf("\n");
	printf("Assert in QueueVar %s %s %d\n", queue->name, file, line );
}

void __cdecl chabuUserCallback( void* userData, struct Chabu_Data* chabuData, enum Chabu_Event event ){
	printf("chabuUserCallback %d\n", event );
	lastEvent = event;
}


struct TestChannelUser_Data testChannelUsers[5];

void __cdecl chabuChannelUserCallback( void* userData, struct Chabu_Channel_Data* chabuChannel, enum Chabu_Channel_Event event ){
	struct TestChannelUser_Data* testChannelUser = (struct TestChannelUser_Data*)userData;
	printf("chabuChannelUserCallback %d\n", event );
	switch( event ){
	case Chabu_Channel_Event_Activated:
		TestChannelUser_SetChannel( testChannelUser, chabuChannel );
		break;
	case Chabu_Channel_Event_DataAvailable:
		TestChannelUser_RecvEvent( testChannelUser, chabuChannel );
		break;
	case Chabu_Channel_Event_PreTransmit:
		TestChannelUser_XmitEvent( testChannelUser, chabuChannel );
		break;
	case Chabu_Channel_Event_Transmitted:
		break;
	}
}

static void resetTestEnv(){
	int i;
	QueueVar_Init( &rxQueues[0], "rx", rxQueueBuffers[0], 1000 );
	QueueVar_Init( &txQueues[0], "tx", txQueueBuffers[0], 1000 );
	for( i = 0; i < 5; i++ ){
		TestChannelUser_Init( &testChannelUsers[i], i );
	}
}

static void TestSetupConnection_LocalConnectionInfo_MaxReceiveSize(){
	printf("--- Running %s ---\n", __FUNCTION__);
	
	EXPECT_ERROR_CODE(Chabu_ErrorCode_OK_NOERROR);
	Chabu_Init( &chabu, 1, 0x123, "ABC", rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	VERIFY_ERROR_CODE();

	EXPECT_ERROR_CODE(Chabu_ErrorCode_SETUP_LOCAL_MAXRECVSIZE);
	Chabu_Init( &chabu, 1, 0x123, "ABC", rx_buf,       0, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	VERIFY_ERROR_CODE();

	EXPECT_ERROR_CODE(Chabu_ErrorCode_SETUP_LOCAL_MAXRECVSIZE);
	Chabu_Init( &chabu, 1, 0x123, "ABC", rx_buf, 0x100-1, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	VERIFY_ERROR_CODE();

}
void TestSetupConnection_LocalConnectionInfo_ApplicationName()  {
	printf("--- Running %s ---\n", __FUNCTION__);

	EXPECT_ERROR_CODE(Chabu_ErrorCode_SETUP_LOCAL_APPLICATIONNAME);
	Chabu_Init( &chabu, 1, 0x123, NULL, rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	VERIFY_ERROR_CODE();

		
	EXPECT_ERROR_CODE(Chabu_ErrorCode_SETUP_LOCAL_APPLICATIONNAME);
	Chabu_Init( &chabu, 1, 0x123, APPLNAME_MAX "-", rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	VERIFY_ERROR_CODE();

	Chabu_Init( &chabu, 1, 0x123, APPLNAME_MAX, rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	VERIFY_ERROR_CODE();
	Chabu_Init( &chabu, 1, 0x123, "", rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	VERIFY_ERROR_CODE();
		
	//Chabu chabu = ChabuBuilder
	//		.start(0x123, "", 0x100, 3)
	//		.setConnectionValidator((local, remote) => {
	//			Console.WriteLine("Local  "+local.applicationName);
	//               Console.WriteLine("Remote " + remote.applicationName);
	//				return null;
	//		})
	//		.addChannel(0, 0x100, 0, new TestChannelUser())
	//		.build();


}		

void TestSetupConnection_LocalConnectionInfo_PriorityCount() {
	printf("--- Running %s ---\n", __FUNCTION__);
		
	// Prio count <= 0
	EXPECT_ERROR_CODE(Chabu_ErrorCode_CONFIGURATION_PRIOCOUNT);
	Chabu_Init( &chabu, 0, 0x123, "ABC", rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	VERIFY_ERROR_CODE();

	EXPECT_ERROR_CODE(Chabu_ErrorCode_CONFIGURATION_PRIOCOUNT);
	Chabu_Init( &chabu, -1, 0x123, "ABC", rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	VERIFY_ERROR_CODE();

	EXPECT_ERROR_CODE(Chabu_ErrorCode_CONFIGURATION_PRIOCOUNT);
	Chabu_Init( &chabu, Chabu_PRIORITY_COUNT_MAX+1, 0x123, "ABC", rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	VERIFY_ERROR_CODE();
}

void TestSetupConnection_LocalConnectionInfo_ChannelConfig()  {
	printf("--- Running %s ---\n", __FUNCTION__);
		
	resetTestEnv();

	//EXPECT_ERROR_CODE(Chabu_ErrorCode_CONFIGURATION_PRIOCOUNT);
	Chabu_Init( &chabu, 3, 0x123, "ABC", rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	Chabu_Init_AddChannel( &chabu, 0, &channels[0], chabuChannelUserCallback, NULL, 0, &rxQueues[0], &txQueues[0] );
	VERIFY_ERROR_CODE();

	EXPECT_ERROR_CODE(Chabu_ErrorCode_CONFIGURATION_CH_ID);
	Chabu_Init( &chabu, 3, 0x123, "ABC", rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	Chabu_Init_AddChannel( &chabu, 1, &channels[0], chabuChannelUserCallback, NULL, 0, &rxQueues[0], &txQueues[0] );
	VERIFY_ERROR_CODE();

	EXPECT_ERROR_CODE(Chabu_ErrorCode_CONFIGURATION_CH_PRIO);
	Chabu_Init( &chabu, 3, 0x123, "ABC", rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	Chabu_Init_AddChannel( &chabu, 0, &channels[0], chabuChannelUserCallback, NULL, -1/* >= 0 */, &rxQueues[0], &txQueues[0] );
	VERIFY_ERROR_CODE();

	EXPECT_ERROR_CODE(Chabu_ErrorCode_CONFIGURATION_CH_PRIO);
	Chabu_Init( &chabu, 3, 0x123, "ABC", rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	Chabu_Init_AddChannel( &chabu, 0, &channels[0], chabuChannelUserCallback, NULL, 3/* not < 3 */, &rxQueues[0], &txQueues[0] );
	VERIFY_ERROR_CODE();
	
	resetTestEnv();

	EXPECT_ERROR_CODE(Chabu_ErrorCode_CONFIGURATION_CH_USER);
	Chabu_Init( &chabu, 3, 0x123, "ABC", rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	Chabu_Init_AddChannel( &chabu, 0, &channels[0], NULL, NULL, 0, &rxQueues[0], &txQueues[0] );
	VERIFY_ERROR_CODE();

	resetTestEnv();

	EXPECT_ERROR_CODE(Chabu_ErrorCode_CONFIGURATION_NO_CHANNELS);
	Chabu_Init( &chabu, 3, 0x123, "ABC", rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	Chabu_Init_Complete( &chabu );
	VERIFY_ERROR_CODE();
}		
void TestSetupConnection_LocalConnectionInfo_ConnectionValidator()  {
	printf("--- Running %s ---\n", __FUNCTION__);
		
	resetTestEnv();

	//ChabuBuilder
	//	.start(0x123, "ABC", 0x100, 3)
	//	.addChannel( 0, 20, 0, new TestChannelUser())
	//	.setConnectionValidator( (local, remote) => null )
	//	.build();
	//	
	//assertException( ChabuErrorCode.CONFIGURATION_VALIDATOR, ()=>{
	//	ChabuBuilder
	//	.start(0x123, "ABC", 0x100, 3)
	//	.addChannel( 0, 20, 0, new TestChannelUser())
	//	.setConnectionValidator( null )
	//	.build();
	//	Assert.Fail();
	//});

	//assertException( ChabuErrorCode.CONFIGURATION_VALIDATOR, ()=>{
	//	ChabuBuilder
	//	.start(0x123, "ABC", 0x100, 3)
	//	.addChannel( 0, 20, 0, new TestChannelUser())
	//	.setConnectionValidator( (local, remote) => null )
	//	.setConnectionValidator( (local, remote) => null )
	//	.build();
	//});

	//{
	//	resetTestEnv();
	//	QueueVar_Init( &queues[0], "rx", queueBuffers[0], 20 );
	//	QueueVar_Init( &queues[1], "tx", queueBuffers[1], 20 );

	//	Chabu_Init( &chabu, 3, 0x123, "ABC", rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	//	Chabu_Init_AddChannel( &chabu, 0, &channels[0], chabuChannelUserCallback, NULL, 0, &queues[0], &queues[1] );
	////	Chabu chabu = ChabuBuilder
	////			.start(0x123, "ABC", 0x100, 3)
	////			.addChannel( 0, 20, 0, new TestChannelUser())
	////			.setConnectionValidator( (local, remote) => {
	////				return new ChabuConnectionAcceptInfo( 0x177, "To Test");
	////			})
	////			.build();
	//		
	//	TraceRunner_Init( &r, &chabu );
	//		
	//	TraceRunner_WireRxHex( &r, "00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 00 00 00 01 00 00 01 00 00 00 01 23 00 00 00 03 41 41 41 00");
	//	TraceRunner_WireTxHex( &r, "00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 00 00 00 01 00 00 01 00 00 00 01 23 00 00 00 03 41 42 43 00");
	//	TraceRunner_WireRxHex( &r, "00 00 00 08 77 77 00 E1");
	//	EXPECT_ERROR_CODE(Chabu_ErrorCode_CONFIGURATION_NO_CHANNELS);
	////	assertException( 0x177, ()=>{
	//		TraceRunner_WireTxHexMore( &r, 20, "00 00 00 08 77 77 00 E1");
	////	});
	//	VERIFY_ERROR_CODE();
	//}
}


void TestTransfers_First()  {
	printf("--- Running %s ---\n", __FUNCTION__);
		
	resetTestEnv();

	
	setupTraceRunner();

	// Fill data
	TraceRunner_ApplToChannelHex( &r, 0, _strdup(""
			"AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "
			"AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "
			"AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "
			"AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "
			"AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55"));

	// recv initial ARMs
	TraceRunner_WireRxHex( &r, _strdup(""
			"00 00 00 10 77 77 00 C3 00 00 00 00 00 00 07 FF "
			"00 00 00 10 77 77 00 C3 00 00 00 01 00 00 07 FF"));

	// send initial ARM with the first data
	TraceRunner_WireTxHex( &r, _strdup(""
			// ARM[0]=64
			"00 00 00 10 77 77 00 C3 00 00 00 00 00 00 02 00 "
			// ARM[1]=64
			"00 00 00 10 77 77 00 C3 00 00 00 01 00 00 00 64 "
			"00 00 00 10 77 77 00 C3 00 00 00 02 00 00 00 64 "
			"00 00 00 10 77 77 00 C3 00 00 00 03 00 00 00 64 "
			"00 00 00 10 77 77 00 C3 00 00 00 04 00 00 00 64 "
			// SEQ[0]=0, DATA[50]
			"00 00 00 64 77 77 00 B4 00 00 00 00 00 00 00 00 00 00 00 50 "
			"AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 " 
			"AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "  
			"AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "  
			"AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55 "  
			"AA AA AA AA 55 55 55 55 AA AA AA AA 55 55 55 55"));
		
	// recv some data
	TraceRunner_WireRxHex( &r, _strdup("00 00 00 1C 77 77 00 B4 00 00 00 00 00 00 00 00 00 00 00 08 01 02 03 04 05 06 07 08"));

	TraceRunner_ChannelToApplHex( &r, 0, _strdup("01 02 03 04 05 06 07 08"));

}


static void setupTraceRunner() {
	resetTestEnv();
	QueueVar_Init( &rxQueues[0], "rx0", rxQueueBuffers[0], 0x201 );
	QueueVar_Init( &rxQueues[1], "rx1", rxQueueBuffers[1], 101 );
	QueueVar_Init( &rxQueues[2], "rx2", rxQueueBuffers[2], 101 );
	QueueVar_Init( &rxQueues[3], "rx3", rxQueueBuffers[3], 101 );
	QueueVar_Init( &rxQueues[4], "rx4", rxQueueBuffers[4], 101 );

	QueueVar_Init( &txQueues[0], "tx0", txQueueBuffers[0], 1000 );
	QueueVar_Init( &txQueues[1], "tx1", txQueueBuffers[1], 1000 );
	QueueVar_Init( &txQueues[2], "tx2", txQueueBuffers[2], 1000 );
	QueueVar_Init( &txQueues[3], "tx3", txQueueBuffers[3], 1000 );
	QueueVar_Init( &txQueues[4], "tx4", txQueueBuffers[4], 1000 );

	Chabu_Init( &chabu, 3, 12345678, "ABC", rx_buf, 0x100, tx_buf, 0x100, chabuUserCallback, NULL, Chabu_AssertFunction );
	Chabu_Init_AddChannel( &chabu, 0, &channels[0], chabuChannelUserCallback, &testChannelUsers[0], 2, &rxQueues[0], &txQueues[0] );
	Chabu_Init_AddChannel( &chabu, 1, &channels[1], chabuChannelUserCallback, &testChannelUsers[1], 1, &rxQueues[1], &txQueues[1] );
	Chabu_Init_AddChannel( &chabu, 2, &channels[2], chabuChannelUserCallback, &testChannelUsers[2], 1, &rxQueues[2], &txQueues[2] );
	Chabu_Init_AddChannel( &chabu, 3, &channels[3], chabuChannelUserCallback, &testChannelUsers[3], 1, &rxQueues[3], &txQueues[3] );
	Chabu_Init_AddChannel( &chabu, 4, &channels[4], chabuChannelUserCallback, &testChannelUsers[4], 0, &rxQueues[4], &txQueues[4] );
	Chabu_Init_Complete( &chabu );
	//Chabu chabu = ChabuBuilder
	//		.start(12345678, "ABC", 0x100, 3)
	//		.addChannel(0, 0x200, 2, new TestChannelUser())
	//		.addChannel(1, 100, 1, new TestChannelUser())
	//		.addChannel(2, 100, 1, new TestChannelUser())
	//		.addChannel(3, 100, 1, new TestChannelUser())
	//		.addChannel(4, 100, 0, new TestChannelUser())
	//		.build();

	TraceRunner_Init( &r, &chabu );
	//TraceRunner r = TraceRunner.test(chabu);
		
	// SETUP
	TraceRunner_WireRxAutoLength( &r, _strdup(""
			"77 77 00 F0 "
			"00 00 00 05 43 48 41 42 55 00 00 00 "
			"00 00 00 01 "
			"00 00 01 00 "
			"00 00 00 06 "
			"00 00 00 03 41 42 43 00")); //TestUtils.test2LengthAndHex("ABC"));
		    
	TraceRunner_WireTxAutoLength( &r, _strdup(""
			"77 77 00 F0 "
			"00 00 00 05 43 48 41 42 55 00 00 00 "
			"00 00 00 01 "
			"00 00 01 00 "
			"00 bc 61 4e "
			"00 00 00 03 41 42 43 00")); //TestUtils.test2LengthAndHex("ABC"));

	// ACCEPT
	TraceRunner_WireRxAutoLength( &r, _strdup("77 77 00 E1"));
	TraceRunner_WireTxAutoLength( &r, _strdup("77 77 00 E1"));
}

void TestTransfers_PayloadLimit()  {
	struct DataGen_Data dg;
	printf("--- Running %s ---\n", __FUNCTION__);

	setupTraceRunner();
	DataGen_Init( &dg, "1", 42 );
		
	// Fill data
	TraceRunner_ApplToChannelHex( &r, 0, DataGen_GetGenBytesString( &dg, 257 ) );

	// recv initial ARMs
	TraceRunner_WireRxHex( &r, _strdup(""
			"00 00 00 10 77 77 00 C3 00 00 00 00 00 00 07 FF "
			"00 00 00 10 77 77 00 C3 00 00 00 01 00 00 07 FF"));

	// send initial ARM with the first data
	TraceRunner_WireTxHex( &r,  TestUtil_StrCat( 5, _strdup(""
			// ARM[0]=64
			"00 00 00 10 77 77 00 C3 00 00 00 00 00 00 02 00 "
			// ARM[1]=64
			"00 00 00 10 77 77 00 C3 00 00 00 01 00 00 00 64 "
			"00 00 00 10 77 77 00 C3 00 00 00 02 00 00 00 64 "
			"00 00 00 10 77 77 00 C3 00 00 00 03 00 00 00 64 "
			"00 00 00 10 77 77 00 C3 00 00 00 04 00 00 00 64 "
			// SEQ[0]=0, DATA[50]
			// len   SE chan. seq........ pls..
			"00 00 01 00 77 77 00 B4 00 00 00 00 00 00 00 00 00 00 00 EC "), DataGen_GetExpBytesString( &dg, 236), _strdup(" "
			"00 00 00 2C 77 77 00 B4 00 00 00 00 00 00 00 EC 00 00 00 15 "), DataGen_GetExpBytesString( &dg,  21), _strdup(" 00 00 00")
			));
		
	TraceRunner_WireTxHexMore( &r,  20, "");

	DataGen_EnsureSamePosition(&dg);
		
	TraceRunner_WireRxHex( &r, TestUtil_StrCat( 5,_strdup(
	"00 00 01 00 77 77 00 B4 00 00 00 00 00 00 00 00 00 00 00 EC "), DataGen_GetExpBytesString( &dg, 236), _strdup(" "
	"00 00 00 2C 77 77 00 B4 00 00 00 00 00 00 00 EC 00 00 00 15 "), DataGen_GetExpBytesString( &dg,  21), _strdup(" 00 00 00")));
		
	TraceRunner_ChannelToApplHex( &r, 0, DataGen_GetGenBytesString( &dg, 257));
	DataGen_EnsureSamePosition(&dg);
		
	// SEQ[0]=F5+0c=0x101 +0x200 -> ARM = 0x301
	TraceRunner_WireTxHexMore( &r,  20, "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 03 01" );
	TraceRunner_WireRxHex( &r,  "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 08 FF " );
}
	
void TestTransfers_Priorities()  {
	printf("--- Running %s ---\n", __FUNCTION__);
//	TraceRunner r = setupTraceRunner();
//	DataGen dg0 = new DataGen("0", 42 );
//	DataGen dg1 = new DataGen("1", 42 );
//	DataGen dg2 = new DataGen("2", 42 );
//	DataGen dg3 = new DataGen("3", 42 );
//	DataGen dg4 = new DataGen("4", 42 );
//		
//	// Fill data all 5 channels
//	TraceRunner_ApplToChannelHex( &r, 0, dg0.getGenBytesString( 257 ) );
//	TraceRunner_ApplToChannelHex( &r, 1, dg1.getGenBytesString( 257 ) );
//	TraceRunner_ApplToChannelHex( &r, 2, dg2.getGenBytesString( 257 ) );
//	TraceRunner_ApplToChannelHex( &r, 3, dg3.getGenBytesString( 257 ) );
//	TraceRunner_ApplToChannelHex( &r, 4, dg4.getGenBytesString( 257 ) );
//
//	// ARM all channels
//	TraceRunner_WireRxAutoLength( &r, ""
//			+ "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 07 FF "
//			+ "00 00 00 10 77 77 00 C3 00 00 00 01 00 00 07 FF "
//			+ "00 00 00 10 77 77 00 C3 00 00 00 02 00 00 07 FF "
//			+ "00 00 00 10 77 77 00 C3 00 00 00 03 00 00 07 FF "
//			+ "00 00 00 10 77 77 00 C3 00 00 00 04 00 00 07 FF "
//			);
//
//	// send initial ARM with the first data
//	TraceRunner_WireTxAutoLength( &r,  ""
//			// ARM[0]=64
//			+ "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 02 00 "
//			// ARM[1]=64
//			+ "00 00 00 10 77 77 00 C3 00 00 00 01 00 00 00 64 "
//			+ "00 00 00 10 77 77 00 C3 00 00 00 02 00 00 00 64 "
//			+ "00 00 00 10 77 77 00 C3 00 00 00 03 00 00 00 64 "
//			+ "00 00 00 10 77 77 00 C3 00 00 00 04 00 00 00 64 "
//			// SEQ[0]=0, DATA[50]
//				
//			// 2 blocks 236+21
//			// len   SE chan. seq........ pls..
//			+ "00 00 01 00 77 77 00 B4 00 00 00 00 00 00 00 00 00 00 00 EC " + dg0.getExpBytesString(236) + " "
//			+ "00 00 00 2C 77 77 00 B4 00 00 00 00 00 00 00 EC 00 00 00 15 " + dg0.getExpBytesString(21) + " 00 00 00 "
//			);
//		
//	// see the data of same priority in round robin
//	// 1, 2, 3, 1, 2, 3
//	TraceRunner_WireTxAutoLength( &r,  ""
//			+ "00 00 01 00 77 77 00 B4 00 00 00 01 00 00 00 00 00 00 00 EC " + dg1.getExpBytesString(236) + " "
//			+ "00 00 01 00 77 77 00 B4 00 00 00 02 00 00 00 00 00 00 00 EC " + dg2.getExpBytesString(236) + " "
//			+ "00 00 01 00 77 77 00 B4 00 00 00 03 00 00 00 00 00 00 00 EC " + dg3.getExpBytesString(236) + " "
//			+ "00 00 00 2C 77 77 00 B4 00 00 00 01 00 00 00 EC 00 00 00 15 " + dg1.getExpBytesString(21) + " 00 00 00 "
//			);
//
//	TraceRunner_WireTxAutoLength( &r,  ""
//			+ "00 00 00 2C 77 77 00 B4 00 00 00 02 00 00 00 EC 00 00 00 15 " + dg2.getExpBytesString( 21) + " 00 00 00 "
//			+ "00 00 00 2C 77 77 00 B4 00 00 00 03 00 00 00 EC 00 00 00 15 " + dg3.getExpBytesString( 21) + " 00 00 00 "
//			);
//		
////		TraceRunner_WireTxAutoLength( &r,  20, "");
//
//	dg0.ensureSamePosition();
//		
//	TraceRunner_WireRxAutoLength( &r, ""
//	+ "00 00 01 00 77 77 00 B4 00 00 00 00 00 00 00 00 00 00 00 EC " + dg0.getExpBytesString(236) + " "
//	+ "00 00 00 2C 77 77 00 B4 00 00 00 00 00 00 00 EC 00 00 00 15 " + dg0.getExpBytesString( 21) + " 00 00 00");
//		
//	TraceRunner_ApplToChannelHex( &r, 2, dg2.getGenBytesString( 257 ) );
//	TraceRunner_ApplToChannelHex( &r, 3, dg3.getGenBytesString( 257 ) );
//
//	TraceRunner_WireTxAutoLength( &r,  ""
//			+ "00 00 01 00 77 77 00 B4 00 00 00 02 00 00 01 01 00 00 00 EC " + dg2.getExpBytesString(236) + " "
//			);
//
//	TraceRunner_ApplToChannelHex( &r, 2, dg2.getGenBytesString( 257 ) );
//
//	TraceRunner_WireTxAutoLength( &r,  ""
//			+ "00 00 01 00 77 77 00 B4 00 00 00 03 00 00 01 01 00 00 00 EC " + dg3.getExpBytesString(236) + " "
//			+ "00 00 01 00 77 77 00 B4 00 00 00 02 00 00 01 ED 00 00 00 EC " + dg2.getExpBytesString(236) + " "
//			+ "00 00 00 2C 77 77 00 B4 00 00 00 03 00 00 01 ED 00 00 00 15 " + dg3.getExpBytesString( 21) + " 00 00 00 "
//			);
//
//	TraceRunner_WireTxAutoLength( &r,  ""
//			+ "00 00 00 40 77 77 00 B4 00 00 00 02 00 00 02 D9 00 00 00 2A " + dg2.getExpBytesString(0x2A) + " 00 00 "
//			+ "00 00 01 00 77 77 00 B4 00 00 00 04 00 00 00 00 00 00 00 EC " + dg4.getExpBytesString(236) + " "
//			+ "00 00 00 2C 77 77 00 B4 00 00 00 04 00 00 00 EC 00 00 00 15 " + dg4.getExpBytesString( 21) + " 00 00 00"
//			);
//
//	TraceRunner_ChannelToApplHex( &r, 0, dg0.getGenBytesString(257));
//	dg0.ensureSamePosition();
//		
//	// SEQ[0]=F5+0c=0x101 +0x200 -> ARM = 0x301
//	TraceRunner_WireTxAutoLength( &r,  20, "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 03 01" );
//	TraceRunner_WireRxAutoLength( &r,      "00 00 00 10 77 77 00 C3 00 00 00 00 00 00 08 FF " );
}
	
static void TestSetupConnection(){
	printf("--  Running %s  --\n", __FUNCTION__);
	TestSetupConnection_LocalConnectionInfo_MaxReceiveSize();
	TestSetupConnection_LocalConnectionInfo_ApplicationName();
	TestSetupConnection_LocalConnectionInfo_PriorityCount();
	TestSetupConnection_LocalConnectionInfo_ChannelConfig();
}

static void TestTransfers(){
	printf("--  Running %s  --\n", __FUNCTION__);
	TestTransfers_First();
	TestTransfers_PayloadLimit();
	TestTransfers_Priorities();
}

int main()
{
	TestSetupConnection();
	TestTransfers();
	printf("*** finished ***\n");
	getchar();
	return 0;
}

