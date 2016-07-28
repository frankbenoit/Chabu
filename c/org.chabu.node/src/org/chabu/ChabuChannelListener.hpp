/*
 * ChabuChannelListener.hpp
 *
 *  Created on: 27.07.2016
 *      Author: Frank
 */

#ifndef ORG_CHABU_CHABUCHANNELLISTENER_HPP_
#define ORG_CHABU_CHABUCHANNELLISTENER_HPP_

#include "../../java/nio/ByteBuffer.h"
#include "ChabuChannel.hpp"

namespace org {
namespace chabu {

class ChabuChannelListener {
public:
	ChabuChannelListener();
	virtual ~ChabuChannelListener();

	virtual java::nio::ByteBuffer& getXmitBuffer( ChabuChannel& channel, int maxSize ) = 0;
	virtual java::nio::ByteBuffer& getRecvBuffer( ChabuChannel& channel, int wantedSize ) = 0;

	virtual void remoteArm     ( ChabuChannel& channel) = 0;
	virtual void remoteDavail  ( ChabuChannel& channel, int availSize ) = 0;
	virtual void remoteReset   ( ChabuChannel& channel) = 0;
	virtual void resetCompleted( ChabuChannel& channel) = 0;
};

} /* namespace chabu */
} /* namespace org */

#endif /* ORG_CHABU_CHABUCHANNELLISTENER_HPP_ */
