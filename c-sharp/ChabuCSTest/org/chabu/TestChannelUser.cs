using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace org.chabu
{

    using org.chabu.container;
    using ByteBuffer = System.IO.MemoryStream;

    public class TestChannelUser : ChabuChannelUser {

	    /**
	     * Normally in filling mode.
	     */
	    private ByteBuffer rx = new ByteBuffer(1000);
	
	    /**
	     * Normally in filling mode.
	     */
        private ByteBuffer tx = new ByteBuffer(1000);
	
	    private ChabuChannel channel;
	    private bool consumeRxInProgress = false;

	    public void setChannel(ChabuChannel channel) {
		    this.channel = channel;
	    }

	    public void recvEvent(ByteQueueOutputPort queue) {
    //		System.out.printf("TestChannelUser[%s].evRecv( bytes=%s )\n", channel.getId(), bufferToConsume.remaining() );

		    if( consumeRxInProgress ){
			    queue.poll(rx);
			    queue.commit();
			    //TraceRunner.ensure( !bufferToConsume.hasRemaining(), "evRecv cannot take all data");
		    }
		
	    }


	    public bool xmitEvent(ByteBuffer bufferToFill) {
    //		System.out.printf("TestChannelUser[%s].evXmit()\n", channel.getId());
		    tx.flip();
		    while( tx.hasRemaining() && bufferToFill.hasRemaining() ){
			    bufferToFill.put( tx.get() );
		    }
		    if( tx.hasRemaining() ){
			    channel.xmitRegisterRequest();
		    }
		    tx.compact();
		    return tx.position() > 0;
	    }

	    public void addTxData( ByteBuffer newData ){
		    while( newData.hasRemaining() ){
			    if( !tx.hasRemaining() ){
				    ByteBuffer tx2 = new ByteBuffer( tx.capacity() * 2 );
				    tx.flip();
				    tx2.put( tx );
				    tx = tx2;
			    }
			    tx.put( newData.get() );
		    }
		    channel.xmitRegisterRequest();
	    }
	
	    public void consumeRxData( ByteBuffer expectedData ){
		    consumeRxInProgress = true;
		    try{
			    rx.clear();
			    rx.limit( expectedData.remaining() );
			    channel.recvRegisterRequest();
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
			    channel.xmitRegisterRequest();
		    }
		    finally {
			    consumeRxInProgress = false;
		    }
	    }
	
    }
}