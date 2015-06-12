package org.chabu;

import java.io.PrintWriter;
import java.nio.ByteBuffer;

public interface IChabu {

	void evRecv(ByteBuffer bb);

	boolean evXmit(ByteBuffer txBuf);

	void setTracePrinter( PrintWriter writer );
	
	int getChannelCount();
	
	IChabuChannel getChannel( int channelId );
	
	IChabuNetwork getNetwork();
}
