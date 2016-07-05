/*
 * common.c
 *
 *  Created on: 11.08.2011
 *      Author: Frank Benoit
 */

#include "Common.h"
#include <string.h>

//pthread_mutex_t Common_global_mutex = PTHREAD_MUTEX_INITIALIZER;

void Common_Init(){
	//pthread_mutex_init( &Common_global_mutex, NULL );
}


void Common_Exit(){
	//pthread_mutex_destroy( &Common_global_mutex );
}

size_t Common_strnlen( const char *start, size_t maxlen ) {
	if( start == NULL ) return 0;
	void* _p2 = memchr( start, 0, maxlen);
	if( _p2 ){
		return (size_t)_p2 - (size_t)start;
	}
	else {
		return maxlen;
	}
}

