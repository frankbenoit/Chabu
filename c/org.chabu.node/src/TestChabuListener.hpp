/*
 * TestChabuListener.hpp
 *
 *  Created on: 27.07.2016
 *      Author: Frank
 */

#ifndef TESTCHABULISTENER_HPP_
#define TESTCHABULISTENER_HPP_

#include "org/chabu/ChabuListener.hpp"
#include "Chabu.h"


class TestChabuListener : public org::chabu::ChabuListener {
public:
	TestChabuListener();
	virtual ~TestChabuListener();
	virtual enum Chabu_ErrorCode acceptConnection( struct Chabu_ConnectionInfo_Data& local, struct Chabu_ConnectionInfo_Data& remote, std::string& msg );
	virtual void    errorFunction               ( enum Chabu_ErrorCode code, const char* file, int line, const std::string msg );
	virtual void    eventNotification           ( enum Chabu_Event event ) ;
	virtual void    networkRecvBuffer           ( java::nio::ByteBuffer& buffer ) ;
	virtual void    networkXmitBuffer           ( java::nio::ByteBuffer& buffer ) ;
	virtual void    networkRegisterWriteRequest () ;
	virtual void    pingCompleted               () ;
	virtual void    remotePing                  ();
	virtual void    remotePing_DisposePongData  ();
};


#endif /* TESTCHABULISTENER_HPP_ */
