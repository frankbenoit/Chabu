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

import java.io.PrintWriter;
import java.nio.ByteBuffer;

/**
 * 
 *
 */
public interface IChabu {

	void evRecv(ByteBuffer bb);

	boolean evXmit(ByteBuffer txBuf);

	void setTracePrinter( PrintWriter writer );
	
	int getChannelCount();
	
	IChabuChannel getChannel( int channelId );
	
	IChabuNetwork getNetwork();
}
