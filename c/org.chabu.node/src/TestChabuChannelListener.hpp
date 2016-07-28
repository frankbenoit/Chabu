/*
 * TestChabuChannelListener.hpp
 *
 *  Created on: 27.07.2016
 *      Author: Frank
 */

#ifndef TESTCHABUCHANNELLISTENER_HPP_
#define TESTCHABUCHANNELLISTENER_HPP_

#include "org/chabu/ChabuChannelListener.hpp"

using org::chabu::ChabuChannel;

class TestChabuChannelListener : public org::chabu::ChabuChannelListener {

	int channelId;
	uint8_t rxData[1000];
	uint8_t txData[1000];
	java::nio::ByteBuffer rx;
	java::nio::ByteBuffer tx;

public:
	TestChabuChannelListener( int channelId );
	virtual ~TestChabuChannelListener();


	virtual java::nio::ByteBuffer& getXmitBuffer( ChabuChannel& channel, int maxSize );
	virtual java::nio::ByteBuffer& getRecvBuffer( ChabuChannel& channel, int wantedSize );

	virtual void remoteArm     ( ChabuChannel& channel);
	virtual void remoteDavail  ( ChabuChannel& channel, int availSize );
	virtual void remoteReset   ( ChabuChannel& channel);
	virtual void resetCompleted( ChabuChannel& channel);
};

#endif /* TESTCHABUCHANNELLISTENER_HPP_ */
