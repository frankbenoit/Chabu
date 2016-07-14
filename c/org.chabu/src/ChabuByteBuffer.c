/*
 * ChabuByteBuffer.c
 *
 *  Created on: 02.07.2016
 *      Author: Frank
 */

#include "Chabu.h"
#include <string.h>
#include <limits.h>

int  Chabu_ByteBuffer_xferAllPossible(struct Chabu_ByteBuffer_Data* trg, struct Chabu_ByteBuffer_Data* src){
	return Chabu_ByteBuffer_xferWithMax( trg, src, INT_MAX );
}
int  Chabu_ByteBuffer_xferWithMax(struct Chabu_ByteBuffer_Data* trg, struct Chabu_ByteBuffer_Data* src, int maxLength){
	int remTrg = trg->limit - trg->position;
	int remSrc = src->limit - src->position;
	int rem = ( remTrg < remSrc ) ? remTrg : remSrc;
	if( rem > maxLength ){
		rem = maxLength;
	}
	memcpy( trg->data + trg->position, src->data + src->position, rem );
	src->position += rem;
	trg->position += rem;
	return rem;
}

static inline void appendPadding(struct Chabu_ByteBuffer_Data* data, int length ){
	while( length-- ){
		data->data[ data->position++ ] = 0;
	}
}

void Chabu_ByteBuffer_putStringFromBuffer(struct Chabu_ByteBuffer_Data* data, struct Chabu_ByteBuffer_Data* stringBuffer){
	int length = Chabu_ByteBuffer_remaining( stringBuffer );
	Chabu_ByteBuffer_putIntBe( data, length );
	Chabu_ByteBuffer_xferAllPossible( data, stringBuffer );
	appendPadding( data, Common_AlignUp4(length)-length );
}


void Chabu_ByteBuffer_compact(struct Chabu_ByteBuffer_Data* data){
	int rem = Chabu_ByteBuffer_remaining(data);
	memmove( data->data, data->data + data->position, rem);
	data->position = rem;
	data->limit = data->capacity;
}

int32 Chabu_ByteBuffer_getIntAt_BE(struct Chabu_ByteBuffer_Data* data, int pos ){
	int result = 0;
	result = data->data[ pos++ ];
	result <<= 8;
	result |= data->data[ pos++ ];
	result <<= 8;
	result |= data->data[ pos++ ];
	result <<= 8;
	result |= data->data[ pos ];
	return result;
}

int32 Chabu_ByteBuffer_getInt_BE(struct Chabu_ByteBuffer_Data* data ){
	int result = 0;
	result = data->data[ data->position++ ];
	result <<= 8;
	result |= data->data[ data->position++ ];
	result <<= 8;
	result |= data->data[ data->position++ ];
	result <<= 8;
	result |= data->data[ data->position++ ];
	return result;
}

int32 Chabu_ByteBuffer_getString(struct Chabu_ByteBuffer_Data* data, char* buffer, int bufferSize ){
	int size = Chabu_ByteBuffer_getInt_BE( data );
	int i;
	int maxLength = bufferSize - 1;
	for( i = 0; i < size && i < maxLength; i++ ){
		buffer[i] = data->data[ data->position + i ];
	}
	data->position += Common_AlignUp4(size);
	int stringLength = ( size < maxLength ) ? size : maxLength;
	buffer[ stringLength ] = 0;
	return stringLength;
}

void Chabu_ByteBuffer_putString(struct Chabu_ByteBuffer_Data* data, const char* const value){
	int len = Common_strnlen(value, 0x100 );
	Chabu_ByteBuffer_putIntBe( data, len );
	memcpy( data->data + data->position, value, len );
	data->position += len;
	appendPadding( data, Common_AlignUp4(len)-len );
}
