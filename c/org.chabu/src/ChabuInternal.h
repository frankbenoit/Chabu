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
#define RPS_MAX             0x10000000LL

#define REPORT_ERROR_IF( cond, c, e, f, ... ) do { if( (cond) ) { reportError( (c), (e), __FILE__, __LINE__, (f), ##__VA_ARGS__ ); } } while(false)
#define REPORT_ERROR( c, e, f, ... ) reportError( (c), (e), __FILE__, __LINE__, (f), ##__VA_ARGS__ )



#endif /* CHABUINTERNAL_H_ */
