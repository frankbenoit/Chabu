package org.chabu.prot.v1.internal;

import java.util.ArrayList;
import java.util.function.BiFunction;

public class ChabuFactory {
	
	public ChabuReceiver createReceiverStartup(AbortMessage abortMessage, Setup setup, Runnable completedStartup){
		return new ChabuReceiverStartup(abortMessage, setup, completedStartup);
	}

	public ChabuReceiver createReceiverNormal(ChabuReceiver receiver, ArrayList<ChabuChannelImpl> channels, AbortMessage localAbortMessage, Setup setup){
		return new ChabuReceiverNormal( receiver, channels, localAbortMessage, setup);
	}
	
	public ChabuXmitter createXmitterStartup(AbortMessage abortMessage, Setup setup, Runnable completionListener){
		return new ChabuXmitterStartup(abortMessage, setup, completionListener);
	}
	
	public ChabuXmitter createXmitterNormal(AbortMessage abortMessage, int priorityCount, ArrayList<ChabuChannelImpl> channels, BiFunction<Integer, Integer, Priorizer> priorizerFactory, int maxXmitSize){		
		return new ChabuXmitterNormal( abortMessage, priorityCount, channels, priorizerFactory, maxXmitSize );
	}
}


