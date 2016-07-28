/*
 * ChabuChannel.hpp
 *
 *  Created on: 25.07.2016
 *      Author: Frank
 */

#ifndef ORG_CHABU_CHABUCHANNEL_HPP_
#define ORG_CHABU_CHABUCHANNEL_HPP_

#include <cstdint>
using std::int64_t;

namespace org {
namespace chabu {

class Chabu;

class ChabuChannel {
	Chabu& chabu;
	int channelId;
public:
	ChabuChannel(Chabu& chabu, int channelId);
	virtual ~ChabuChannel();

	inline int getChannelId(){ return channelId; }


	int64_t getRecvPosition();
	int64_t getRecvLimit();
	int64_t getXmitPosition();
	int64_t getXmitLimit();

};

} /* namespace chabu */
} /* namespace org */

#endif /* ORG_CHABU_CHABUCHANNEL_HPP_ */
