/*
 * queue_var.c
 *
 *  Created on: 29.08.2011
 *      Author: Frank Benoit
 */
#include "Common.h"
#include "QueueVar.h"

void QueueVar_Init( struct QueueVar* queue, const char* name, uint8* buf, int buf_size ){
	queue->name = name;
	queue->buf = buf;
	queue->buf_size = buf_size;
	queue->wr_idx = 0;
	queue->rd_idx = 0;
}
void   QueueVar_SetCallbackSupplied( struct QueueVar* queue, TQueueVar_NotifySupplied* callbackSupplied, void* ctx ){
	if( callbackSupplied ){
		// avoid reconfigure by accident.
		// if this is intended, first set the call back to NULL
		Assert( queue->callbackSupplied == NULL );
	}
	queue->callbackSupplied = callbackSupplied;
	queue->callbackSuppliedData = ctx;
}
void   QueueVar_SetCallbackConsumed( struct QueueVar* queue, TQueueVar_NotifyConsumed* callbackConsumed, void* ctx ){
	if( callbackConsumed ){
		// avoid reconfigure by accident.
		// if this is intended, first set the call back to NULL
		Assert( queue->callbackConsumed == NULL );
	}
	queue->callbackConsumed = callbackConsumed;
	queue->callbackConsumedData = ctx;
}

void QueueVar_Clear( struct QueueVar* queue ){
	int sz = QueueVar_Available(queue);
	queue->wr_idx = 0;
	queue->rd_idx = 0;
	if( queue->callbackConsumed ){
		queue->callbackConsumed( queue->callbackConsumedData, queue, sz );
	}
}


void QueueVar_Write( struct QueueVar* queue, const void* buf, int len ){
	Assert( len >= 0 );
	Assert( QueueVar_Free(queue) >= len );

	int rd = queue->rd_idx;
	int wr = queue->wr_idx;
	AssertPrintf( wr < queue->buf_size, "%d<%d in %s", wr, queue->buf_size, queue->name );

	int remaining = len;
	int offset = 0;
	int end = wr + remaining;
	if( end > queue->buf_size ){
		int cpy_len = queue->buf_size - wr;

		Assert( cpy_len >= 0 );
		Assert( cpy_len <= len );
		Assert( wr >= 0 );

		Common_MemCopy(
				queue->buf, queue->buf_size, wr,
				buf       , len            ,  0,
				cpy_len );

		remaining -= cpy_len;
		Assert( remaining >= 0 );
		offset = cpy_len;
		wr = 0;
	}
	Assert( offset+remaining == len );
	Assert( wr+remaining <= queue->buf_size );
	Assert( wr >= 0 );

	Common_MemCopy(
			queue->buf, queue->buf_size, wr,
			buf       , len            , offset,
			remaining );

	wr += remaining;
	if( wr == queue->buf_size ){
		wr = 0;
	}

	// needed to update and make the atomic check with rd_idx
	queue->wr_idx = wr;
	if( len ){
		// after adding data, they cannot be equal
		Assert( rd != wr );
	}

	if( queue->callbackSupplied ){
		queue->callbackSupplied( queue->callbackSuppliedData, queue, len );
	}

}
void   QueueVar_WriteBufferAsPacket( struct QueueVar* queue, Buffer* buf ){
	int size = align4(buf->used);
	QueueVar_Write( queue, &size, 4 );
	QueueVar_Write( queue, buf->data, size );
}

int QueueVar_Free( struct QueueVar* queue ){
	int res = queue->buf_size -1 -QueueVar_Available(queue);
	Assert( res >= 0 );
	return res;
}

void QueueVar_Move( struct QueueVar* src_queue, struct QueueVar* trg_queue, int size ){

	Assert( QueueVar_Free(trg_queue) >= size);
	Assert( QueueVar_Available(src_queue) >= size);

	while( size > 0 ){
		void* ptr = QueueVar_ReadPtr(src_queue);
		int cpy_len = QueueVar_ReadChunkSize(src_queue);
		if( cpy_len > size ) {
			cpy_len = size;
		}
		QueueVar_Write(trg_queue, ptr, cpy_len);
		QueueVar_ReadCommit( src_queue, cpy_len );
		size -= cpy_len;
	}
}

int QueueVar_Size( struct QueueVar* queue ){
	return queue->buf_size;
}

int QueueVar_Available( struct QueueVar* queue ){
	int wr = queue->wr_idx;
	int rd = queue->rd_idx;
	if( rd > wr ){
		wr += queue->buf_size;
	}
	int res = wr - rd;
	if( res < 0 ) xil_printf( "queue name %s, r=%d, w=%d\n\r", queue->name, queue->rd_idx, queue->wr_idx );
	Assert( res >= 0 );
	return res;
}

/**
 * TODO: rename -> this function perform a MemCopy action, so its not only a peek!
 */
void QueueVar_Peek( struct QueueVar* queue, void* buf, int len ){
	int rd = queue->rd_idx;
	int wr = queue->wr_idx;

	//FIXME remove later ...
	Assert(( QueueVar_Available(queue) >= len ) && ( len >= 0 ));

	int remaining = len;
	int cpyOffset = 0;
	int end = rd + remaining;
	if( end > queue->buf_size ){
		int cpy_len = queue->buf_size - rd;

		Assert( wr < rd );
		Assert( cpy_len < len );
		Assert( rd+cpy_len <= queue->buf_size );
		Common_MemCopy(
				buf       , len            , 0,
				queue->buf, queue->buf_size, rd,
				cpy_len );

		remaining -= cpy_len;
		cpyOffset = cpy_len;
		rd = 0;
	}
	// the remaining piece of data end before the buffer ends.
	Assert( rd+remaining <= queue->buf_size );
	if( ! ( ( rd+remaining <= wr ) || (wr < rd) ) ){
		dbg_printf( "Queue %s, wr:%d rd:%d remaining:%d", queue->name, wr, rd, remaining );
	}
	// the remaining piece of data ends before the wr -or- it starts behind the wr.
	Assert(( rd+remaining <= wr ) || (wr < rd));
	// the target data does not exceed the len of the passed buf.
	Assert( cpyOffset+remaining == len );

	Common_MemCopy(
			buf        , len           , cpyOffset,
			queue->buf, queue->buf_size, rd       ,
			remaining );
}

/**
 * TODO: rename -> this function perform a MemCopy action, so its not only a peek!
 */
void QueueVar_PeekOffset( struct QueueVar* queue, void* buf, int len, int offset ){
	int rd = queue->rd_idx + offset;
	if( rd >= queue->buf_size ){
		rd -= queue->buf_size;
	}
	int remaining = len;
	int cpyOffset = 0;
	int end = rd + remaining;
	if( end > queue->buf_size ){
		int cpy_len = queue->buf_size - rd;

		Assert( queue->wr_idx < rd );
		Assert( cpy_len < len );
		Assert( rd+cpy_len <= queue->buf_size );

		Common_MemCopy(
				buf       , len            , 0 ,
				queue->buf, queue->buf_size, rd,
				cpy_len );

		remaining -= cpy_len;
		cpyOffset = cpy_len;
		rd = 0;
	}
	Assert( rd+remaining <= queue->buf_size );
	Assert(( rd+remaining <= queue->wr_idx ) || (queue->wr_idx < rd));
	Assert( cpyOffset+remaining == len );

	Common_MemCopy(
			buf       , len            , cpyOffset,
			queue->buf, queue->buf_size, rd       ,
			remaining );
}
void QueueVar_Read( struct QueueVar* queue, void* buf, int len ){
	QueueVar_Peek(queue, buf, len);
	QueueVar_ReadCommit( queue, len );
}
uint8* QueueVar_ReadPtr( struct QueueVar* queue ){
	return &queue->buf[queue->rd_idx];
}
int QueueVar_ReadChunkSize( struct QueueVar* queue ){
	int wr = queue->wr_idx;
	int rd = queue->rd_idx;
	if( wr >= rd ){
		return wr - rd;
	}
	else {
		return queue->buf_size - rd;
	}
}
void QueueVar_ReadCommit( struct QueueVar* queue, int len ){
	Assert( len >= 0 );
	if( !(len <= QueueVar_Available(queue)) ){
		xil_printf( "QueueVar_ReadCommit %x, len=%d, %s avail=%d\n\r", queue, len, queue->name, QueueVar_Available(queue) );
	}
	Assert( len <= QueueVar_Available(queue) );

	int rd = queue->rd_idx;
	rd += len;
	if( rd >= queue->buf_size ){
		rd -= queue->buf_size;
	}

	Assert( rd >= 0 );
	Assert( rd <= queue->buf_size );
	//FIXME: not necessary if rd == queue->buf_size than rd -= queue->buf_size; occur before.
	if( rd == queue->buf_size ){
		rd = 0;
	}
	queue->rd_idx = rd;
	if( queue->callbackConsumed ){
		queue->callbackConsumed( queue->callbackConsumedData, queue, len );
	}

}

