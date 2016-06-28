
#include "ByteBuffer.h"


LIBRARY_API void ByteBuffer_Init( struct ByteBuffer_Data* data, uint8* memory, int size ){
	data->capacity = size;
	data->limit    = 0;
	data->position = 0;
	data->data     = memory;
	data->byteOrderIsBigEndian = true;
}
LIBRARY_API void ByteBuffer_clear(struct ByteBuffer_Data* data){
	data->limit    = 0;
	data->position = 0;
}
LIBRARY_API void ByteBuffer_flip(struct ByteBuffer_Data* data){
	data->limit    = data->position;
	data->position = 0;
}

LIBRARY_API bool ByteBuffer_hasRemaining(struct ByteBuffer_Data* data){
	return data->position < data->limit;
}

LIBRARY_API int  ByteBuffer_xferAllPossible(struct ByteBuffer_Data* trg, struct ByteBuffer_Data* src){
	int remTrg = trg->limit - trg->position;
	int remSrc = src->limit - src->position;
	int rem = ( remTrg < remSrc ) ? remTrg : remSrc;
	memcpy( trg->data + trg->position, src->data + src->position, rem );
	src->position += rem;
	trg->position += rem;
	return rem;
}

LIBRARY_API int  ByteBuffer_remaining(struct ByteBuffer_Data* data){
	return data->limit - data->position;
}

LIBRARY_API void ByteBuffer_putByte(struct ByteBuffer_Data* data, uint8 value){
	data->data[ data->position ] = value;
	data->position++;
}

LIBRARY_API uint8 ByteBuffer_get(struct ByteBuffer_Data* data){
	uint8 res = data->data[ data->position ];
	data->position++;
	return res;
}

LIBRARY_API void ByteBuffer_compact(struct ByteBuffer_Data* data){
	int rem = ByteBuffer_remaining(data);
	memmove( data->data, data->data + data->position, rem);
	data->position = rem;
	data->limit = data->capacity;
}


