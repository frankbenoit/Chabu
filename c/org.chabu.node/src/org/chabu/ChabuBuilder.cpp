/*
 * ChabuBuilder.cpp
 *
 *  Created on: 27.07.2016
 *      Author: Frank
 */

#include "ChabuBuilder.hpp"

namespace org {
namespace chabu {

ChabuBuilder::ChabuBuilder(int applicationVersion, std::string applicationProtocolName, ChabuListener& listener)
: applicationVersion( applicationVersion )
, applicationProtocolName( applicationProtocolName )
, listener ( listener )
, recvPacketSize( 0x100 )
, priorityCount( 0 )
{
}

ChabuBuilder::~ChabuBuilder()
{
}

ChabuBuilder& ChabuBuilder::setRecvPacketSize( int size ){
	return *this;
}
ChabuBuilder& ChabuBuilder::addChannel( int channelId, int priority, ChabuChannelListener& listener ){

	return *this;
}
std::unique_ptr<Chabu> ChabuBuilder::build(){
	auto res = std::unique_ptr<Chabu>( new Chabu(*this));
	return res;
}



} /* namespace chabu */
} /* namespace org */
