/*
 * ChabuInternal.h
 *
 *  Created on: 30.06.2016
 *      Author: Frank
 */

#ifndef CHABUINTERNAL_H_
#define CHABUINTERNAL_H_


extern const struct Chabu_StructInfo structInfo_chabu;
extern const struct Chabu_StructInfo structInfo_channel;


#define CHANNEL_COUNT_MAX  100
#define PRIORITY_COUNT_MAX 100
#define APN_MAX_LENGTH      56
#define RPS_MIN             0x100LL
#define RPS_MAX             0x10000000UL
#define PACKET_MAGIC        0x77770000UL

#define REPORT_ERROR_IF( cond, c, e, f, ... ) do { if( (cond) ) { reportError( (c), (e), __FILE__, __LINE__, (f), ##__VA_ARGS__ ); } } while(false)
#define REPORT_ERROR( c, e, f, ... ) reportError( (c), (e), __FILE__, __LINE__, (f), ##__VA_ARGS__ )

enum PacketType {
	PacketType_Setup   = 0xF0,
	PacketType_Accept  = 0xE1,
	PacketType_Abort   = 0xD2,
	PacketType_ARM     = 0xC3,
	PacketType_SEQ     = 0xB4,
	PacketType_RST_REQ = 0xA5,
	PacketType_RST_ACK = 0x96,
	PacketType_DAVAIL  = 0x87,
	PacketType_PING    = 0x78,
	PacketType_PONG    = 0x69,
};

#endif /* CHABUINTERNAL_H_ */
