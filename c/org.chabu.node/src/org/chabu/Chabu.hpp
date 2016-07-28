/*
 * Chabu.hpp
 *
 *  Created on: 25.07.2016
 *      Author: Frank
 */

#ifndef ORG_CHABU_CHABU_HPP_
#define ORG_CHABU_CHABU_HPP_

#include <vector>
#include "Chabu.h"

#include "ChabuListener.hpp"
#include "ChabuChannel.hpp"

namespace org {
namespace chabu {

class ChabuBuilder;

class Chabu {
	struct Chabu_Data c_chabu;
	struct Chabu_Channel_Data * c_channelData;
	struct Chabu_Priority_Data * c_priorityData;

	ChabuListener& listener;
	std::vector<ChabuChannel> channels;

	Chabu( ChabuBuilder& builder );
	friend class ChabuBuilder;
	friend class ChabuChannel;
public:
	virtual ~Chabu();


	enum Chabu_ErrorCode acceptConnection      ( struct Chabu_ConnectionInfo_Data* local, struct Chabu_ConnectionInfo_Data* remote, struct Chabu_ByteBuffer_Data* msg ) noexcept;
	void           errorFunction               ( enum Chabu_ErrorCode code, const char* file, int line, const char* msg ) noexcept;
	void           eventNotification           ( enum Chabu_Event event ) noexcept;
	void           networkRecvBuffer           ( struct Chabu_ByteBuffer_Data* buffer ) noexcept;
	void           networkXmitBuffer           ( struct Chabu_ByteBuffer_Data* buffer ) noexcept;

	inline int           getChannelCount()          { return channels.size(); }
	inline ChabuChannel& getChannel( int channelId ){ return channels.at(channelId); }
	std::string toString();
};

} /* namespace chabu */
} /* namespace org */

#endif /* ORG_CHABU_CHABU_HPP_ */
