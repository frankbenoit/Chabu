

#ifndef QUEUE_VAR_H_
#define QUEUE_VAR_H_

#include "Common.h"
#include "ByteBuffer.h"

#ifdef __cplusplus
  extern "C" {
#endif


struct QueueVar;

typedef void (CALL_SPEC TQueueVar_AssertFunction)( struct QueueVar* queue, const char* file, int line, const char* fmt, ... );
typedef void (CALL_SPEC TQueueVar_NotifySupplied)( void* ctx, struct QueueVar* queue, int amount );
typedef void (CALL_SPEC TQueueVar_NotifyConsumed)( void* ctx, struct QueueVar* queue, int amount );

struct QueueVar {
	TQueueVar_AssertFunction  * assertFunction;
	const char              * name;
	uint8                   * buf;
	int                       buf_size;
	int                       rd_idx;
	int                       wr_idx;
	TQueueVar_NotifySupplied* callbackSupplied;
	void*                     callbackSuppliedData;
	TQueueVar_NotifyConsumed* callbackConsumed;
	void*                     callbackConsumedData;
};

/**
 * Initialize a queue
 */
LIBRARY_API void QueueVar_Init( struct QueueVar* queue, const char* name, uint8* buf, int buf_size );

LIBRARY_API void   QueueVar_SetCallbackSupplied( struct QueueVar* queue, TQueueVar_NotifySupplied* callbackSupplied, void* ctx );
LIBRARY_API void   QueueVar_SetCallbackConsumed( struct QueueVar* queue, TQueueVar_NotifyConsumed* callbackConsumed, void* ctx );

/**
 * Reset the queue to have not more contained data
 */
LIBRARY_API void   QueueVar_Clear( struct QueueVar* queue );

/**
 * Get the amount of bytes that can be written into the queue at the moment.
 */
LIBRARY_API int    QueueVar_Free( struct QueueVar* queue );

/**
 * Write data into the queue.
 */
LIBRARY_API void   QueueVar_Write( struct QueueVar* queue, const void* buf, int len );

/**
 * Write the content of buf into the queue, prepend a 4-byte length to the data.
 * The length is 4-byte aligned.
 */
LIBRARY_API void   QueueVar_WriteBufferAsPacket( struct QueueVar* queue, Buffer* buf );

/**
 * Get the amount of bytes, that can be read from the queue at the moment.
 */
LIBRARY_API int    QueueVar_Available( struct QueueVar* queue );

/**
 * Copy data from the queue to buf, but do not remove that data from the queue.
 */
LIBRARY_API void   QueueVar_Peek( struct QueueVar* queue, void* buf, int len );

/**
 * Copy data from the queue to buf, but do not remove that data from the queue.
 */
LIBRARY_API void   QueueVar_PeekOffset( struct QueueVar* queue, void* buf, int len, int offset );

/**
 * Copy data from the queue to buf, and remove that data from the queue.
 */
LIBRARY_API void   QueueVar_Read( struct QueueVar* queue, void* buf, int len );

/**
 * Get a pointer to the memory in the queue, where the next data is read from.
 * See QueueVar_ReadChunkSize(), to know how much data can be read from this pointer.
 * The length of available data is limited by the contained data, and by the end of
 * the internal memory, where the data wraps around to the beginning of that memory.
 */
LIBRARY_API uint8* QueueVar_ReadPtr( struct QueueVar* queue );

/**
 * Get the amount of bytes, that can be read from the pointer returned by QueueVar_ReadPtr.
 * The length of available data is limited by the contained data, and by the end of
 * the internal memory, where the data wraps around to the beginning of that memory.
 */
LIBRARY_API int    QueueVar_ReadChunkSize( struct QueueVar* queue );

/**
 * Remove the "len" amount of bytes from the queue.
 */
LIBRARY_API void   QueueVar_ReadCommit( struct QueueVar* queue, int len );

/**
 * Move data from one queue to the other.
 */
LIBRARY_API void   QueueVar_Move( struct QueueVar* src_queue, struct QueueVar* trg_queue, int size );

/**
 * Get the amount of contained bytes
 */
LIBRARY_API int    QueueVar_Size( struct QueueVar* queue );


LIBRARY_API int    QueueVar_ReadIntoByteBuffer( struct QueueVar* queue, struct ByteBuffer_Data* byteBuffer );

LIBRARY_API int    QueueVar_WriteFromByteBuffer( struct QueueVar* queue, struct ByteBuffer_Data* byteBuffer );

#ifdef __cplusplus
  }
#endif


#endif /* QUEUE_VAR_H_ */
