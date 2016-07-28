/*
 * TestChabuChannelListener.cpp
 *
 *  Created on: 27.07.2016
 *      Author: Frank
 */

#include "TestChabuChannelListener.hpp"
#include <iostream>
#include <boost/format.hpp>

using std::cout;
using std::endl;
using boost::format;

TestChabuChannelListener::TestChabuChannelListener(int channelId)
: channelId( channelId )
, rx( rxData, sizeof( rxData ))
, tx( txData, sizeof( txData ))
{
}

TestChabuChannelListener::~TestChabuChannelListener()
{
}

java::nio::ByteBuffer& TestChabuChannelListener::getXmitBuffer( ChabuChannel& channel, int maxSize ){
	cout << format("TestChabuChannelListener::getXmitBuffer %d: %d") % channel.getChannelId() % maxSize << endl;
	return tx;
}
java::nio::ByteBuffer& TestChabuChannelListener::getRecvBuffer( ChabuChannel& channel, int wantedSize ){
	cout << format("TestChabuChannelListener::getRecvBuffer %d: %d") % channel.getChannelId() % wantedSize << endl;
	return rx;
}

void TestChabuChannelListener::remoteArm     ( ChabuChannel& channel){
	cout << format("TestChabuChannelListener::remoteArm %d") % channel.getChannelId() << endl;
}
void TestChabuChannelListener::remoteDavail  ( ChabuChannel& channel, int availSize ){
	cout << format("TestChabuChannelListener::remoteDavail %d: %d") % channel.getChannelId() % availSize << endl;

}
void TestChabuChannelListener::remoteReset   ( ChabuChannel& channel){
	cout << format("TestChabuChannelListener::remoteReset %d") % channel.getChannelId() << endl;

}
void TestChabuChannelListener::resetCompleted( ChabuChannel& channel){
	cout << format("TestChabuChannelListener::resetCompleted %d") % channel.getChannelId() << endl;

}
