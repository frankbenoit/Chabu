/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit, Stuttgart, Germany <fr@nk-benoit.de>
 *
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 *
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
package org.chabu.prot.v1.internal;

import java.util.BitSet;

import org.chabu.prot.v1.ChabuErrorCode;

public class Priorizer {
	
	private final BitSet[] requests;
	private int lastChannel;
	private final int channelCount;
	
	public Priorizer( int priorityCount, int channelCount ){
		this.channelCount = channelCount;
		requests = new BitSet[ priorityCount ];
		for (int i = 0; i < priorityCount; i++) {
			requests[i] = new BitSet(channelCount);
		}
		lastChannel = channelCount -1;
	}

	public void request(int priority, int channelId) {
		Utils.ensure(priority < requests.length, ChabuErrorCode.ASSERT, 
				"priority:%s < xmitChannelRequestData.length:%s", priority, requests.length );
		requests[ priority ].set(channelId);
	}

	public int popNextRequest() {
		for( int prio = requests.length-1; prio >= 0; prio-- ){
			
			int res = calcNextXmitChannelForPrio(prio );
			if( res >= 0 ){
				return res;
			}
			
		}
		return -1;
	}
	
	private int calcNextXmitChannelForPrio(int prio ) {
		int idxCandidate = -1;
		BitSet prioBitSet = requests[prio];

		// search from last channel pos on
		if( lastChannel+1 < channelCount ){
			idxCandidate = prioBitSet.nextSetBit(lastChannel+1);
		}

		// try from idx zero
		if( idxCandidate < 0 ){
			idxCandidate = prioBitSet.nextSetBit(0);
		}

		// if found, clear and use it
		if( idxCandidate >= 0 ){
			prioBitSet.clear(idxCandidate);
			lastChannel = idxCandidate;
			return idxCandidate;
		}
		return -1;
	}

}
