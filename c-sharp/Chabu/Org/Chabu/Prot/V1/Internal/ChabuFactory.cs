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
using System;
using System.Diagnostics;

namespace Org.Chabu.Prot.V1.Internal
{
    public class ChabuFactory {
	
	public ChabuReceiver createReceiverStartup(AbortMessage abortMessage, Setup setup, Runnable completedStartup){
		return new ChabuReceiverStartup(abortMessage, setup, completedStartup);
	}

	public ChabuReceiver createReceiverNormal(ChabuReceiver receiver, ArrayList<ChabuChannelImpl> channels, AbortMessage localAbortMessage, Setup setup){
		return new ChabuReceiverNormal( receiver, channels, localAbortMessage, setup);
	}
	
	public ChabuXmitter createXmitterStartup(AbortMessage abortMessage, Runnable xmitRequestListener, Setup setup, Runnable completionListener){
		return new ChabuXmitterStartup(abortMessage, xmitRequestListener, setup, completionListener);
	}
	
	public ChabuXmitter createXmitterNormal(AbortMessage abortMessage, Runnable xmitRequestListener, int priorityCount, ArrayList<ChabuChannelImpl> channels, BiFunction<Integer, Integer, Priorizer> priorizerFactory, int maxXmitSize){		
		return new ChabuXmitterNormal( abortMessage, xmitRequestListener, priorityCount, channels, priorizerFactory, maxXmitSize );
	}
}


