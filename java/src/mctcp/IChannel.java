package mctcp;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public interface IChannel {
	
	public boolean txPossible();
	public ByteBuffer txGetBuffer();
	
	public boolean rxPossible();
	public ByteBuffer rxGetBuffer();

	public void setHandler( Consumer<IChannel> h );
	
	public void registerWaitForRead();
	public void registerWaitForWrite();

	public void close();
	
}
