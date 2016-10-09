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

public class SingleEventNotifierFromTwoSources {

	boolean first = false;
	boolean second = false;
	private Runnable listener;
	
	public SingleEventNotifierFromTwoSources( Runnable listener ){
		this.listener = listener;
	}

	public void event1(){
		if( !first ){
			first = true;
			if( second ){
				listener.run();
			}
		}
	}
	public void event2(){
		if( !second ){
			second = true;
			if( first ){
				listener.run();
			}
		}
	}
}
