package mctcp;

import java.util.function.Consumer;

public interface INetworkConnector {

	void start();

	IChannel getChannel(int i);
	public void setChannelHandler( Consumer<IChannel> h );

}
