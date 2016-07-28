/*
 * ChabuListener.hpp
 *
 *  Created on: 26.07.2016
 *      Author: Frank
 */

#ifndef ORG_CHABU_CHABULISTENER_HPP_
#define ORG_CHABU_CHABULISTENER_HPP_

#include <string>
#include "Chabu.h"
#include "../../java/nio/ByteBuffer.h"

namespace org { namespace chabu {

class ChabuListener {
public:
	ChabuListener();
	virtual ~ChabuListener();

	virtual enum Chabu_ErrorCode acceptConnection( struct Chabu_ConnectionInfo_Data& local, struct Chabu_ConnectionInfo_Data& remote, std::string& msg ) = 0;
	virtual void    errorFunction               ( enum Chabu_ErrorCode code, const char* file, int line, const std::string msg ) = 0;
	virtual void    eventNotification           ( enum Chabu_Event event ) = 0;
	virtual void    networkRecvBuffer           ( java::nio::ByteBuffer& buffer ) = 0;
	virtual void    networkXmitBuffer           ( java::nio::ByteBuffer& buffer ) = 0;
	virtual void    networkRegisterWriteRequest () = 0;
	virtual void    pingCompleted               () = 0;
	virtual void    remotePing                  () = 0;
	virtual void    remotePing_DisposePongData  () = 0;


};

}} /* namespace org::chabu */

#endif /* ORG_CHABU_CHABULISTENER_HPP_ */
