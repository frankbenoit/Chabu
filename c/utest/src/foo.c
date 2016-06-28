/*
 * foo.c
 *
 *  Created on: 25.06.2016
 *      Author: fbenoit1
 */
#include <stdlib.h>
#include <string.h>

void foo(int size)
{
	char * p = (char*) malloc(size);
	strcpy( p, "hello world\n" );
}
