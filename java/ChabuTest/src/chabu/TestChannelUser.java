package chabu;

import java.nio.ByteBuffer;

public class TestChannelUser implements IChabuChannelUser {

	/**
	 * Normally in filling mode.
	 */
	private ByteBuffer rx = ByteBuffer.allocate(1000);
	
	/**
	 * Normally in filling mode.
	 */
	private ByteBuffer tx = ByteBuffer.allocate(1000);
	
	private IChabuChannel channel;
	private boolean consumeRxInProgress = false;
	@Override
	public void setChannel(IChabuChannel channel) {
		this.channel = channel;
	}

	@Override
	public void evRecv(ByteBuffer bufferToConsume) {
//		System.out.printf("TestChannelUser[%s].evRecv( bytes=%s )\n", channel.getId(), bufferToConsume.remaining() );

		if( consumeRxInProgress ){
			while( rx.hasRemaining() && bufferToConsume.hasRemaining() ){
				rx.put( bufferToConsume.get() );
			}
			//TraceRunner.ensure( !bufferToConsume.hasRemaining(), "evRecv cannot take all data");
		}
		
	}

	@Override
	public boolean evXmit(ByteBuffer bufferToFill) {
//		System.out.printf("TestChannelUser[%s].evXmit()\n", channel.getId());
		tx.flip();
		while( tx.hasRemaining() && bufferToFill.hasRemaining() ){
			bufferToFill.put( tx.get() );
		}
		tx.compact();
		return tx.position() > 0;
	}

	public void addTxData( ByteBuffer newData ){
		while( newData.hasRemaining() ){
			if( !tx.hasRemaining() ){
				ByteBuffer tx2 = ByteBuffer.allocate( tx.capacity() * 2 );
				tx.flip();
				tx2.put( tx );
				tx = tx2;
			}
			tx.put( newData.get() );
		}
		channel.evUserXmitRequest();
	}
	
	public void consumeRxData( ByteBuffer expectedData ){
		consumeRxInProgress = true;
		try{
			rx.clear();
			rx.limit( expectedData.remaining() );
			channel.evUserRecvRequest();
			rx.flip();
			TraceRunner.ensure( rx.remaining() == expectedData.remaining(), "RX does not have enough data %s != %s", rx.remaining(), expectedData.remaining());
			while( expectedData.hasRemaining() ){
				if( !rx.hasRemaining() ){
					TraceRunner.ensure( false, "RX does not have enough data");
				}
				int exp = 0xFF & expectedData.get();
				int cur = 0xFF & rx.get();
				TraceRunner.ensure( exp == cur, "Data mismatch %02X != %02X", cur, exp );
			}
			rx.compact();
			channel.evUserXmitRequest();
		}
		finally {
			consumeRxInProgress = false;
		}
	}
	
}
