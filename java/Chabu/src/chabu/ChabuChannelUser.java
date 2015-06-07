package chabu;

import java.nio.ByteBuffer;

public class ChabuChannelUser implements IChabuChannelUser {
		protected ByteBuffer recv;
		protected ByteBuffer xmit;
		protected IChabuChannel channel;
		
		public ChabuChannelUser( ByteBuffer recv, ByteBuffer xmit ){
			this.recv = recv;
			this.xmit = xmit;
			recv.limit( recv.position() );
		}
		
		public void setChannel(IChabuChannel channel) {
			this.channel = channel;
		}
		
		public boolean evXmit(ByteBuffer bufferToFill) {
//			System.out.println("ModuleC5v2.RunnerCtrl.strmChannelUser.new INetworkUser() {...}.evXmit()");
			xmit.flip();
			int oldLimit = xmit.limit();
			//int sz = strmRqBuffer.remaining();
			if( xmit.remaining() > bufferToFill.remaining() ){
				xmit.limit( xmit.position() + bufferToFill.remaining() );
				//sz = bufferToFill.remaining();
			}
			bufferToFill.put(xmit);
			xmit.limit(oldLimit);
			xmit.compact();
			//System.out.printf( "% 5d: to chabu %d bytes, %d in buffer\n", System.currentTimeMillis()-dbgSt, sz, strmRqBuffer.position() );

			return false;
		}
		public void evRecv(ByteBuffer bufferToConsume) {
//			System.out.println("ModuleC5v2.RunnerCtrl.strmChannelUser.new INetworkUser() {...}.evRecv()");
			if( bufferToConsume != null ){
				recv.compact();
				recv.put( bufferToConsume );
				recv.flip();
			}
		}
	}