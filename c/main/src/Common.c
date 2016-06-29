/*
 * common.c
 *
 *  Created on: 11.08.2011
 *      Author: Frank Benoit
 */

#include "Common.h"

//pthread_mutex_t Common_global_mutex = PTHREAD_MUTEX_INITIALIZER;

void Common_Init(){
	//pthread_mutex_init( &Common_global_mutex, NULL );
}


void Common_Exit(){
	//pthread_mutex_destroy( &Common_global_mutex );
}
