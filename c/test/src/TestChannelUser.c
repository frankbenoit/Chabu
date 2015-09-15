
#include "TestChannelUser.h"
#include "TraceRunner.h"
#include <Chabu.h>
#include <QueueVar.h>

void TestChannelUser_Init( struct TestChannelUser_Data* data, int channelId ){
	data->consumeRxInProgress = 0;
	data->channel = NULL;
	ByteBuffer_Init( &data->rx, data->rxQueueBuffer, sizeof(data->rxQueueBuffer));
	ByteBuffer_Init( &data->tx, data->txQueueBuffer, sizeof(data->txQueueBuffer));
	data->tx.limit = data->tx.capacity;
	data->channelId = channelId;
}

void TestChannelUser_ConsumeRxData(struct TestChannelUser_Data* data, struct ByteBuffer_Data* expectedData ){
	data->consumeRxInProgress = 1;

	//ByteBuffer_clear(&data->rx);
	//data->rx.limit = ByteBuffer_remaining(expectedData);
	//Chabu_Channel_evUserRecvRequest(data->channel);
	//ByteBuffer_flip(&data->rx);
	TraceRunner_Ensure( ByteBuffer_remaining(&data->rx) == ByteBuffer_remaining(expectedData), "RX does not have enough data %d != %d", ByteBuffer_remaining(&data->rx), ByteBuffer_remaining(expectedData));
	while( ByteBuffer_hasRemaining(expectedData) ){
		int exp;
		int cur;
		if( !ByteBuffer_hasRemaining(&data->rx) ){
			TraceRunner_Ensure( 0, "RX does not have enough data");
		}
		exp = 0xFF & ByteBuffer_get(expectedData);
		cur = 0xFF & ByteBuffer_get(&data->rx);
		TraceRunner_Ensure( exp == cur, "Data mismatch %02X != %02X", cur, exp );
	}
	ByteBuffer_compact(&data->rx);
	Chabu_Channel_XmitRequestData(data->channel);

	data->consumeRxInProgress = 0;
}

// application/test add tx data to be stored for being transmitted.
void TestChannelUser_AddTxData(struct TestChannelUser_Data* data, struct ByteBuffer_Data* newData ){
	int sz;
	TraceRunner_Ensure( ByteBuffer_remaining(newData) <= ByteBuffer_remaining(&data->tx), "" );
	sz = ByteBuffer_xferAllPossible( &data->tx, newData );
	//printf( "Added %d bytes for tx\n", sz );
	Chabu_Channel_XmitRequestData(data->channel);
}

	
void TestChannelUser_SetChannel(struct TestChannelUser_Data* data, struct Chabu_Channel_Data* channel) {
	data->channel = channel;
}

void TestChannelUser_RecvEvent(struct TestChannelUser_Data* data, struct Chabu_Channel_Data* channel) {
//		System.out.printf("TestChannelUser[%s].evRecv( bytes=%s )\n", channel.getId(), bufferToConsume.remaining() );

	//if( data->consumeRxInProgress )
	{
		int avail = QueueVar_Available(channel->recvQueue);
	ByteBuffer_compact(&data->rx);
		QueueVar_ReadIntoByteBuffer( channel->recvQueue, &data->rx );
	ByteBuffer_flip(&data->rx);
		//TraceRunner.ensure( !bufferToConsume.hasRemaining(), "evRecv cannot take all data");
	}
		
}

// called by chabu, to get data for tx
bool TestChannelUser_XmitEvent(struct TestChannelUser_Data* data, struct Chabu_Channel_Data* channel) {
//		System.out.printf("TestChannelUser[%s].evXmit()\n", channel.getId());
	ByteBuffer_flip(&data->tx);
	QueueVar_WriteFromByteBuffer( channel->xmitQueue, &data->tx );
	//while( ByteBuffer_hasRemaining(&data->tx) && ByteBuffer_hasRemaining(bufferToFill) ){
	//	ByteBuffer_putByte(bufferToFill, ByteBuffer_get(&data->tx) );
	//}
	if( ByteBuffer_hasRemaining(&data->tx) ){
		Chabu_Channel_XmitRequestData(data->channel);
	}
	ByteBuffer_compact(&data->tx);
	return data->tx.position > 0;
}

	

