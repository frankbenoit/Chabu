/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit, Stuttgart, Germany <keinfarbton@gmail.com>
 * 
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 * 
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
package org.chabu;

import java.nio.ByteBuffer;

import org.chabu.container.ByteQueueOutputPort;

public class ChabuChannelUserDefault implements ChabuChannelUser {
		protected ByteBuffer recv;
		protected ByteBuffer xmit;
		protected ChabuChannel channel;
		
		public ChabuChannelUserDefault( ByteBuffer recv, ByteBuffer xmit ){
			this.recv = recv;
			this.xmit = xmit;
			recv.limit( recv.position() );
		}
		
		@Override
		public void setChannel(ChabuChannel channel) {
			this.channel = channel;
		}
		
		@Override
		public boolean xmitEvent( ByteBuffer bufferToFill ) {
			xmit.flip();
			int oldLimit = xmit.limit();
			if( xmit.remaining() > bufferToFill.remaining() ){
				xmit.limit( xmit.position() + bufferToFill.remaining() );
			}
			bufferToFill.put(xmit);
			xmit.limit(oldLimit);
			xmit.compact();

			return false;
		}

		@Override
		public void recvEvent(ByteQueueOutputPort queueOutput) {
			recv.compact();
			queueOutput.poll( recv );
			queueOutput.commit();
			recv.flip();
		}

	}