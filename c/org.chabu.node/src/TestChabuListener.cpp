/*
 * TestChabuListener.cpp
 *
 *  Created on: 27.07.2016
 *      Author: Frank
 */

#include "TestChabuListener.hpp"
#include <iostream>
#include <boost/format.hpp>

using std::cout;
using std::endl;
using boost::format;


TestChabuListener::TestChabuListener()
{
}

TestChabuListener::~TestChabuListener()
{
}

enum Chabu_ErrorCode TestChabuListener::acceptConnection( struct Chabu_ConnectionInfo_Data& local, struct Chabu_ConnectionInfo_Data& remote, std::string& msg ){;
	cout << format("TestChabuListener::acceptConnection")  << endl;
	return Chabu_ErrorCode_OK_NOERROR;
}
void    TestChabuListener::errorFunction               ( enum Chabu_ErrorCode code, const char* file, int line, const std::string msg ){
	cout << format("TestChabuListener::errorFunction %s:%s %s") % file % line % msg  << endl;
}
void    TestChabuListener::eventNotification           ( enum Chabu_Event event ) {
	cout << format("TestChabuListener::eventNotification %s") % event  << endl;
}
void    TestChabuListener::networkRecvBuffer           ( java::nio::ByteBuffer& buffer ) {

	cout << format("TestChabuListener::networkRecvBuffer")  << endl;
}
void    TestChabuListener::networkXmitBuffer           ( java::nio::ByteBuffer& buffer ) {

	cout << format("TestChabuListener::networkXmitBuffer")  << endl;
}
void    TestChabuListener::networkRegisterWriteRequest () {

	cout << format("TestChabuListener::networkRegisterWriteRequest")  << endl;
}
void    TestChabuListener::pingCompleted               () {

	cout << format("TestChabuListener::pingCompleted")  << endl;
}
void    TestChabuListener::remotePing                  () {

	cout << format("TestChabuListener::remotePing")  << endl;
}
void    TestChabuListener::remotePing_DisposePongData  () {

	cout << format("TestChabuListener::remotePing_DisposePongData")  << endl;
}

