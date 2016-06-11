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
