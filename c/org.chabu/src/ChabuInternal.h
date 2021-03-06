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
extern const struct Chabu_StructInfo structInfo_priority;


#define CHANNEL_COUNT_MAX  100
#define PRIORITY_COUNT_MAX 100
#define APN_MAX_LENGTH      56
#define RPS_MIN             0x100LL
#define RPS_MAX             0x10000000LL
#define PACKET_MAGIC        0x77770000ULL
#define PROTOCOL_NAME       "CHABU"
#define SEQ_HEADER_SZ       20

#define REPORT_ERROR_IF( cond, c, e, f, ... ) do { if( (cond) ) { Chabu_ReportError( (c), (e), __FILE__, __LINE__, (f), ##__VA_ARGS__ ); } } while(false)
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

void Chabu_ReportError( struct Chabu_Data* chabu, enum Chabu_ErrorCode error, const char* file, int line, const char* fmt, ... );

LIBRARY_API void Chabu_Priority_SetRequestCtrl_Arm( struct Chabu_Data* chabu, struct Chabu_Channel_Data* ch );
LIBRARY_API void Chabu_Priority_SetRequestCtrl_Davail( struct Chabu_Data* chabu, struct Chabu_Channel_Data* ch );
LIBRARY_API void Chabu_Priority_SetRequestCtrl_Reset( struct Chabu_Data* chabu, struct Chabu_Channel_Data* ch );
//LIBRARY_API void Chabu_Priority_SetRequestCtrl( struct Chabu_Data* chabu, struct Chabu_Channel_Data* ch );
LIBRARY_API void Chabu_Priority_SetRequestData( struct Chabu_Data* chabu, struct Chabu_Channel_Data* ch );
LIBRARY_API struct Chabu_Channel_Data* Chabu_Priority_PopNextRequestCtrl( struct Chabu_Data* chabu );
LIBRARY_API struct Chabu_Channel_Data* Chabu_Priority_PopNextRequestData( struct Chabu_Data* chabu );

#endif /* CHABUINTERNAL_H_ */
