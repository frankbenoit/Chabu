/*
 * ChabuByteBuffer.c
 *
 *  Created on: 02.07.2016
 *      Author: Frank
 */

#include "Chabu.h"
#include <string.h>

int  Chabu_ByteBuffer_xferAllPossible(struct Chabu_ByteBuffer_Data* trg, struct Chabu_ByteBuffer_Data* src){
	int remTrg = trg->limit - trg->position;
	int remSrc = src->limit - src->position;
	int rem = ( remTrg < remSrc ) ? remTrg : remSrc;
	memcpy( trg->data + trg->position, src->data + src->position, rem );
	src->position += rem;
	trg->position += rem;
	return rem;
}

void Chabu_ByteBuffer_compact(struct Chabu_ByteBuffer_Data* data){
	int rem = Chabu_ByteBuffer_remaining(data);
	memmove( data->data, data->data + data->position, rem);
	data->position = rem;
	data->limit = data->capacity;
}

void Chabu_ByteBuffer_putString(struct Chabu_ByteBuffer_Data* data, const char* const value){
	int len = Common_strnlen(value, 0x100 );
	Chabu_ByteBuffer_putIntBe( data, len );
	memcpy( data->data + data->position, value, len );
	data->position += len;
	int diff = Common_AlignUp4(len) - len;
	while( diff-- ){
		data->data[ data->position++ ] = 0;
	}
}
