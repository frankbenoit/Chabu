/*
 * ChabuOpts.h
 *
 *  Created on: 12.12.2014
 *      Author: Frank
 */

#ifndef CHABUOPTS_H_
#define CHABUOPTS_H_

#include <stdlib.h>
#ifdef _MSC_VER
#include <string.h>
#define Chabu_strnlen( _p, _l ) strnlen( _p, _l )
#else
# define Chabu_strnlen( _p, _l ) ({ void* _p2 = memchr((_p), 0, _l); (_p2 == NULL ? _l : (_p2 - (void*)(_p))); })
#endif

#define Chabu_dbg_printf(...)
#define Chabu_dbg_memory( context, ptr, len )
#define Chabu_CHANNEL_COUNT_MAX 8


//#define Chabu_AssertPrintf(_cond, fmt, args... ) 								\
//				fprintf(stderr, fmt, ##args ); 									\

#define Chabu_AssertPrintf(_cond, fmt, ... ) 								\
	do{ 																		\
		if( !(_cond) ){															\
				fprintf(stderr, fmt, __VA_ARGS__ );								\
				fprintf(stderr, "\n" ); 										\
				fprintf(stderr, "Assert failed %s:%d\n", __FILE__, __LINE__ ); 	\
				exit(EXIT_FAILURE); 											\
		} 																		\
	}while(false)





#define Chabu_Assert(_cond) 													\
	do{ 																		\
		if( !(_cond) ){															\
				fprintf(stderr, "Assert failed %s:%d\n", __FILE__, __LINE__ ); 	\
				exit(EXIT_FAILURE); 											\
		} 																		\
	}while(false)


//#define Chabu_USE_LOCK
#define Chabu_LOCK_TYPE
#define Chabu_LOCK_CREATE( _var )
#define Chabu_LOCK_DO_LOCK( _var )
#define Chabu_LOCK_DO_UNLOCK( _var )

#endif /* CHABUOPTS_H_ */
