package org.chabu.prot.v1.internal;

import org.chabu.prot.v1.ChabuErrorCode;

public interface Aborter {
	
	void delayedAbort(int code, String message, Object ... args) ;
	
	void delayedAbort(ChabuErrorCode ec, String message, Object ... args) ;

}
