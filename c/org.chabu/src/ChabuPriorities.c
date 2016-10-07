/*
 * ChabuPriorities.c
 *
 *  Created on: 15.07.2016
 *      Author: fbenoit1
 */

#include "Chabu.h"
#include "ChabuInternal.h"

static void setRequest( struct Chabu_Data* chabu, struct Chabu_PriorityEntry_Data* entry, struct Chabu_PriorityList_Data* prioList );

LIBRARY_API void Chabu_Priority_SetRequestCtrl( struct Chabu_Data* chabu, struct Chabu_Channel_Data* ch );

LIBRARY_API void Chabu_Priority_SetRequestCtrl_Arm( struct Chabu_Data* chabu, struct Chabu_Channel_Data* ch ){
	ch->xmit.requestCtrl_Arm = true;
	Chabu_Priority_SetRequestCtrl( chabu, ch );
}
LIBRARY_API void Chabu_Priority_SetRequestCtrl_Davail( struct Chabu_Data* chabu, struct Chabu_Channel_Data* ch ){
	ch->xmit.requestCtrl_Davail = true;
	Chabu_Priority_SetRequestCtrl( chabu, ch );
}
LIBRARY_API void Chabu_Priority_SetRequestCtrl_Reset( struct Chabu_Data* chabu, struct Chabu_Channel_Data* ch ){
	ch->xmit.requestCtrl_Reset = true;
	Chabu_Priority_SetRequestCtrl( chabu, ch );
}
LIBRARY_API void Chabu_Priority_SetRequestCtrl( struct Chabu_Data* chabu, struct Chabu_Channel_Data* ch ){

	struct Chabu_Priority_Data* prio = &chabu->priorities[ ch->priority ];

	setRequest( chabu, &ch->xmit.requestCtrl, &prio->ctrl );


}
LIBRARY_API void Chabu_Priority_SetRequestData( struct Chabu_Data* chabu, struct Chabu_Channel_Data* ch ){

	struct Chabu_Priority_Data* prio = &chabu->priorities[ ch->priority ];

	setRequest( chabu, &ch->xmit.requestData, &prio->data );

}

static void setRequest( struct Chabu_Data* chabu, struct Chabu_PriorityEntry_Data* entry, struct Chabu_PriorityList_Data* prioList ){
	UNUSED(chabu);

	if( prioList->request == NULL ){
		prioList->request = entry;
	}
	else if( prioList->request == entry ){
		return;
	}
	else {
		struct Chabu_PriorityEntry_Data* it = prioList->request;

		int channelId = entry->ch->channelId;
		while( it->next && ( it->next->ch->channelId < channelId )){
			struct Chabu_PriorityEntry_Data* next = it->next;
			it = next;
		}
		if( it->next && it->next->ch == entry->ch ){
			return;
		}
		entry->next = it->next;
		it->next = entry;
	}

}

static struct Chabu_Channel_Data* popNextRequest( struct Chabu_Data* chabu, bool ctrl );

LIBRARY_API struct Chabu_Channel_Data* Chabu_Priority_PopNextRequestCtrl( struct Chabu_Data* chabu ){
	return popNextRequest( chabu, true );
}
LIBRARY_API struct Chabu_Channel_Data* Chabu_Priority_PopNextRequestData( struct Chabu_Data* chabu ){
	return popNextRequest( chabu, false );
}

static struct Chabu_Channel_Data* popNextRequestForLevel( struct Chabu_Data* chabu, struct Chabu_PriorityList_Data* prioList ){
	UNUSED(chabu);

	if( !prioList->request ){
		return NULL;
	}

	int startChannelId = prioList->lastSelectedChannelId;

	// check first entry
	// take if it is the only one
	// take if it channelId is already good
	if(( prioList->request->next == NULL ) || ( prioList->request->ch->channelId > startChannelId )){
		struct Chabu_Channel_Data* res = prioList->request->ch;
		prioList->request = prioList->request->next;
		prioList->lastSelectedChannelId = res->channelId;
		return res;
	}

	// search to find first with channelId bigger than the startChannelId
	{
		struct Chabu_PriorityEntry_Data* it = prioList->request;

		while( it->next && ( it->next->ch->channelId <= startChannelId )){
			it = it->next;
		}

		if( it->next && (it->next->ch->channelId > startChannelId )){
			struct Chabu_PriorityEntry_Data* resEntry = it->next;
			it->next = resEntry->next;

			resEntry->next = NULL;

			struct Chabu_Channel_Data* ch = resEntry->ch;
			prioList->lastSelectedChannelId = ch->channelId;
			return ch;
		}
	}

	// none found, so take first
	{
		struct Chabu_PriorityEntry_Data* resEntry = prioList->request;
		prioList->request = prioList->request->next;
		prioList->lastSelectedChannelId = resEntry->ch->channelId;
		resEntry->next = NULL;
		return resEntry->ch;
	}
}
static struct Chabu_Channel_Data* popNextRequest( struct Chabu_Data* chabu, bool ctrl ){
	int prioIdx = 0;
	for( ; prioIdx < chabu->priorityCount; prioIdx ++ ){
		struct Chabu_Priority_Data* prio = &chabu->priorities[ prioIdx ];
		struct Chabu_PriorityList_Data* prioList = ctrl ? &prio->ctrl : &prio->data;
		if( !prioList->request ){
			continue;
		}
		struct Chabu_Channel_Data* res = popNextRequestForLevel( chabu, prioList );
		if( res ) {
			return res;
		}
	}
	return NULL;

}

