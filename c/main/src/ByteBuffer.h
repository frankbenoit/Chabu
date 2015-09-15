#ifndef __BYTE_BUFFER__H
#define __BYTE_BUFFER__H

#include "Common.h"


#ifdef __cplusplus
extern "C" {
#endif

struct ByteBuffer_Data {
	int position;
	int limit;
	int capacity;
	uint8 * data;
	bool   byteOrderIsBigEndian;
};

LIBRARY_API void ByteBuffer_Init( struct ByteBuffer_Data* data, uint8* memory, int size );
LIBRARY_API void ByteBuffer_clear(struct ByteBuffer_Data* data);
LIBRARY_API void ByteBuffer_flip(struct ByteBuffer_Data* data);
LIBRARY_API bool ByteBuffer_hasRemaining(struct ByteBuffer_Data* data);
LIBRARY_API int  ByteBuffer_xferAllPossible(struct ByteBuffer_Data* trg, struct ByteBuffer_Data* src);
LIBRARY_API int  ByteBuffer_remaining(struct ByteBuffer_Data* data);
LIBRARY_API void ByteBuffer_putByte(struct ByteBuffer_Data* data, uint8 value);
LIBRARY_API uint8 ByteBuffer_get(struct ByteBuffer_Data* data);
LIBRARY_API void ByteBuffer_compact(struct ByteBuffer_Data* data);

#ifdef __cplusplus
}
#endif

#endif

