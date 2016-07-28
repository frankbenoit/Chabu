/*
 * ChabuBuilder.hpp
 *
 *  Created on: 27.07.2016
 *      Author: Frank
 */

#ifndef ORG_CHABU_CHABUBUILDER_HPP_
#define ORG_CHABU_CHABUBUILDER_HPP_

#include <memory>
#include <vector>

#include "Chabu.hpp"
#include "ChabuListener.hpp"
#include "ChabuChannelListener.hpp"

namespace org {
namespace chabu {

using std::string;
using std::vector;

class ChabuBuilder {

	struct ChabuChannelSetup {
		int priority;
		ChabuChannelListener& listener;
	};

	int         applicationVersion;
	std::string applicationProtocolName;
	ChabuListener& listener;
	int         recvPacketSize;
	int         priorityCount;
	vector<ChabuChannelSetup> channels;

	friend class Chabu;

public:
	ChabuBuilder(int applicationVersion, std::string applicationName, ChabuListener& listener);
	virtual ~ChabuBuilder();

	ChabuBuilder& setRecvPacketSize( int size );
	ChabuBuilder& addChannel( int channelId, int priority, ChabuChannelListener& listener );
	std::unique_ptr<Chabu> build();

};

} /* namespace chabu */
} /* namespace org */

#endif /* ORG_CHABU_CHABUBUILDER_HPP_ */
