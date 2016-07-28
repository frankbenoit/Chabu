/*
 * Chabu.cpp
 *
 *  Created on: 25.07.2016
 *      Author: Frank
 */

#include "Chabu.hpp"
#include <stdexcept>
#include "ChabuBuilder.hpp"
namespace org {
namespace chabu {

static enum Chabu_ErrorCode CALL_SPEC ccb_acceptConnection      ( void* userData, struct Chabu_ConnectionInfo_Data* local, struct Chabu_ConnectionInfo_Data* remote, struct Chabu_ByteBuffer_Data* msg ){
	Chabu* chabu = reinterpret_cast<Chabu*>(userData);
	return chabu->acceptConnection( local, remote, msg );
}
static void CALL_SPEC ccb_errorFunction               ( void* userData, enum Chabu_ErrorCode code, const char* file, int line, const char* msg ){
	Chabu* chabu = reinterpret_cast<Chabu*>(userData);
	chabu->errorFunction( code, file, line, msg );
}
static void CALL_SPEC ccb_eventNotification           ( void* userData, enum Chabu_Event event ){
	Chabu* chabu = reinterpret_cast<Chabu*>(userData);
	chabu->eventNotification( event );
}
static void CALL_SPEC ccb_networkRecvBuffer           ( void* userData, struct Chabu_ByteBuffer_Data* buffer ){
	Chabu* chabu = reinterpret_cast<Chabu*>(userData);
	chabu->networkRecvBuffer( buffer );
}
static void CALL_SPEC ccb_networkXmitBuffer           ( void* userData, struct Chabu_ByteBuffer_Data* buffer ){
	Chabu* chabu = reinterpret_cast<Chabu*>(userData);
	chabu->networkXmitBuffer( buffer );
}

Chabu::Chabu( ChabuBuilder& builder )
: c_channelData  ( nullptr )
, c_priorityData ( nullptr )
, listener( listener )
{
	if( builder.channels.size() > 1000 ) throw std::overflow_error("Chabu: builder.channels.size() > 1000");
	if( builder.priorityCount > 1000 ) throw std::overflow_error("Chabu: builder.priorityCount > 1000");

	c_channelData  = new Chabu_Channel_Data[builder.channels.size()];
	c_priorityData = new Chabu_Priority_Data[builder.priorityCount];

	Chabu_Init( &c_chabu,
			builder.applicationVersion, builder.applicationProtocolName.c_str(),
			builder.recvPacketSize,
			c_channelData, builder.channels.size(),
			c_priorityData, builder.priorityCount,
			ccb_errorFunction,
			ccb_acceptConnection,
			ccb_eventNotification,
			ccb_networkRecvBuffer, ccb_networkXmitBuffer, this );
}

Chabu::~Chabu()
{
	delete [] c_channelData;
	delete [] c_priorityData;
}

enum Chabu_ErrorCode Chabu::acceptConnection      ( struct Chabu_ConnectionInfo_Data* local, struct Chabu_ConnectionInfo_Data* remote, struct Chabu_ByteBuffer_Data* msg ) noexcept {
	return Chabu_ErrorCode_OK_NOERROR;
}
void           Chabu::errorFunction               ( enum Chabu_ErrorCode code, const char* file, int line, const char* msg ) noexcept {

}
void           Chabu::eventNotification           ( enum Chabu_Event event ) noexcept {

}
void           Chabu::networkRecvBuffer           ( struct Chabu_ByteBuffer_Data* buffer ) noexcept {

}
void           Chabu::networkXmitBuffer           ( struct Chabu_ByteBuffer_Data* buffer ) noexcept {

}
std::string Chabu::toString(){
	return "Chabu::toString";
}

} /* namespace chabu */
} /* namespace org */
