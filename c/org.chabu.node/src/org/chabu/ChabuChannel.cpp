/*
 * ChabuChannel.cpp
 *
 *  Created on: 25.07.2016
 *      Author: Frank
 */

#include "ChabuChannel.hpp"
#include "Chabu.hpp"

namespace org {
namespace chabu {

ChabuChannel::ChabuChannel( Chabu& chabu, int channelId )
: chabu( chabu )
, channelId( channelId )
{
}

ChabuChannel::~ChabuChannel()
{
}

int64_t ChabuChannel::getRecvPosition(){
	return Chabu_Channel_GetRecvPosition( &chabu.c_chabu, channelId );
}
int64_t ChabuChannel::getRecvLimit(){
	return Chabu_Channel_GetRecvLimit( &chabu.c_chabu, channelId );
}
int64_t ChabuChannel::getXmitPosition(){
	return Chabu_Channel_GetXmitPosition( &chabu.c_chabu, channelId );
}
int64_t ChabuChannel::getXmitLimit(){
	return Chabu_Channel_GetXmitLimit( &chabu.c_chabu, channelId );
}


} /* namespace chabu */
} /* namespace org */
