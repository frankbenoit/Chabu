/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit.
 * 
 * See the LICENSE.txt or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 * 
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
package org.chabu;

import org.chabu.internal.Chabu;
import org.chabu.internal.ChabuChannel;
import org.chabu.internal.Utils;

public class ChabuBuilder {
	
	private Chabu chabu;
	private int channelId;

	private ChabuBuilder( ChabuSetupInfo ci, IChabuNetwork nw, int priorityCount ){
		chabu = new Chabu( ci );
		chabu.setPriorityCount(priorityCount);
		chabu.setNetwork(nw);
	}

	public static ChabuBuilder start( ChabuSetupInfo ci, IChabuNetwork nw, int priorityCount ){
		return new ChabuBuilder(ci, nw, priorityCount);
	}

	public ChabuBuilder addChannel( int channelId, int recvBufferSize, int priority, IChabuChannelUser user ){
		Utils.ensure( channelId == this.channelId, ChabuErrorCode.CONFIGURATION_CH_ID, "Channel ID must be ascending, expected %s, but was %s", this.channelId, channelId );
		ChabuChannel channel = new ChabuChannel( recvBufferSize, priority, user );
		chabu.addChannel( channel );
		this.channelId++;
		return this;
	}
	
	public ChabuBuilder setConnectionValidator( IChabuConnectingValidator val ) {
		chabu.setConnectingValidator( val );
		return this;
	}
	
	
	public IChabu build() {
		Chabu res = chabu;
		chabu = null;
		res.activate();
		return res;
	}
}
