package org.chabu;

import java.nio.ByteBuffer;

import org.chabu.prot.v1.ChabuByteExchange;
import org.chabu.prot.v1.ChabuChannel;
import org.chabu.prot.v1.internal.ByteBufferUtils;

public class TestChannelUser implements ChabuByteExchange {

	/**
	 * Normally in filling mode.
	 */
	private ByteBuffer recv = ByteBuffer.allocate(1000);
	
	
	/**
	 * Normally in filling mode.
	 */
	private ByteBuffer xmitUser = ByteBuffer.allocate(1000);
	private ByteBuffer xmitChabu = ByteBuffer.allocate(1000);
	
	private ChabuChannel channel;
	
	public TestChannelUser() {
		xmitChabu.limit(0);
	}
	
	
	@Override
	public void setChannel(ChabuChannel channel) {
		this.channel = channel;
	}

	public void addTxData( ByteBuffer newData ){
		int added = newData.remaining();
		while( newData.hasRemaining() ){
			if( !xmitUser.hasRemaining() ){
				ByteBuffer tx2 = ByteBuffer.allocate( xmitUser.capacity() * 2 );
				xmitUser.flip();
				tx2.put( xmitUser );
				xmitUser = tx2;
			}
			xmitUser.put( newData.get() );
		}
		channel.addXmitLimit(added);
	}
	@Override
	public ByteBuffer getXmitBuffer(int size) {
		if( xmitChabu.remaining() < size ){
			xmitChabu.compact();
			xmitUser.flip();
			ByteBufferUtils.transferUntilTargetPos(xmitUser, xmitChabu, size );
			xmitChabu.flip();
			xmitUser.compact();
		}
		return xmitChabu;
	}

	@Override
	public void xmitCompleted() {
	}

	@Override
	public ByteBuffer getRecvBuffer(int size) {
		recv.limit( recv.position() + size );
		return recv;
	}

	@Override
	public void recvCompleted() {
	}

	public void verifyReceivedData(ByteBuffer rx) {

		recv.flip();

		boolean isOk = true;
		int mismatchPos = -1;
		if( recv.limit() != rx.limit() ){
			isOk = false;
		}
		else {
			for( int i = 0; i < rx.limit(); i++ ){
				int exp = 0xFF & rx.get(i);
				int cur = 0xFF & recv.get(i);
				if( exp != cur ){
					isOk = false;
					mismatchPos = i;
					break;
				}
			}
		}

		if( !isOk ){
			System.out.println("TX by org.chabu:" + TestUtils.toHexString( recv, true ) + TestUtils.dumpHexString( recv ));
			System.out.println("Expected   :"+TestUtils.dumpHexString( rx ));
			if( recv.limit() != rx.limit()){
				System.out.printf("length (%s) does not match the expected length (%s). First mismatch at pos %s%n", 
						recv.limit(), rx.limit(), mismatchPos);
			}
			int searchLen = Math.min(rx.limit(), recv.limit());
			for( int i = 0; i < searchLen; i++ ){
				int exp = 0xFF & rx.get(i);
				int cur = 0xFF & recv.get(i);
				if( cur != exp ){ 
					System.out.printf("TX data (0x%02X) != expected (0x%02X) at index 0x%X%n", cur, exp, i );
					break;
				}
			}
		}

		this.channel.addRecvLimit(recv.limit());
		
		recv.clear();
		
		if( !isOk ){
			throw new RuntimeException("TX data mismatch");
		}
	}

	@Override
	public void xmitReset() {
		System.out.println("TestChannelUser.xmitReset()");
	}

	@Override
	public void recvReset() {
		System.out.println("TestChannelUser.recvReset()");
	}
	
}
