/*
 * PriorityTest.cpp
 *
 *  Created on: 15.07.2016
 *      Author: Frank
 */

#include "gtest/gtest.h"
#include "FakeFunctions.h"
#include <Chabu.h>
#include <string>
#include "Utils.h"
using std::string;

extern "C" {
	LIBRARY_API void Chabu_Priority_SetRequestCtrl( struct Chabu_Data* chabu, struct Chabu_Channel_Data* ch );
	LIBRARY_API void Chabu_Priority_SetRequestData( struct Chabu_Data* chabu, struct Chabu_Channel_Data* ch );
	LIBRARY_API struct Chabu_Channel_Data* Chabu_Priority_PopNextRequestCtrl( struct Chabu_Data* chabu );
	LIBRARY_API struct Chabu_Channel_Data* Chabu_Priority_PopNextRequestData( struct Chabu_Data* chabu );
}
namespace {

	struct Chabu_Data chabu;
	struct Chabu_Channel_Data channel[25];
	struct Chabu_Priority_Data priority[5];




}

static void setup(){
	chabu.channels = channel;
	chabu.priorities = priority;
	chabu.channelCount = countof(channel);
	chabu.priorityCount = countof(priority);

	for( size_t i = 0; i < countof(channel); i++ ){
		struct Chabu_Channel_Data& ch = channel[i];

		ch.channelId = i;

		ch.xmitRequestCtrl.ch = &ch;
		ch.xmitRequestCtrl.next = nullptr;

		ch.xmitRequestData.ch = &ch;
		ch.xmitRequestData.next = nullptr;
		ch.priority = i / 5;
	}

	for( size_t i = 0; i < countof(priority); i++ ){
		struct Chabu_Priority_Data& prio = priority[i];

		prio.ctrl.lastSelectedChannelId = -1;
		prio.ctrl.request = nullptr;

		prio.data.lastSelectedChannelId = -1;
		prio.data.request = nullptr;

	}

}
TEST( PriorityTest, none_returnNull ){
	setup();


	EXPECT_EQ( NULL, Chabu_Priority_PopNextRequestCtrl( &chabu ) );
	EXPECT_EQ( NULL, Chabu_Priority_PopNextRequestData( &chabu ) );


}

static void put(int chIdx ){
	Chabu_Priority_SetRequestData( &chabu, &channel[chIdx] );
}

static int pop(){
	struct Chabu_Channel_Data* res = Chabu_Priority_PopNextRequestData( &chabu );
	if( res ) return res->channelId;
	return -1;
}


TEST( PriorityTest, addAll_retrieveAll ){
	setup();
	for( size_t i = 0; i < countof(channel); i++ ){
		put(i);
	}
	for( size_t i = 0; i < countof(channel); i++ ){
		EXPECT_EQ( (int)i, pop() );
	}
	EXPECT_EQ( -1, pop() );

}

TEST( PriorityTest, addAfterPop_nextIsNotAgainTheJustAdded ){
	setup();
	put(1);
	put(2);
	put(3);
	EXPECT_EQ( 1, pop() );
	EXPECT_EQ( 2, pop() );
	put(2);
	EXPECT_EQ( 3, pop() );
	EXPECT_EQ( 2, pop() );
	EXPECT_EQ( -1, pop() );

}

TEST( PriorityTest, addHigherPrio_nextIsHigherPrio ){
	setup();
	put(10);
	put(12);
	EXPECT_EQ( 10, pop() );
	put(3);
	EXPECT_EQ( 3, pop() );
	EXPECT_EQ( 12, pop() );
}

