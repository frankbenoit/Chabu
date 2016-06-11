package org.chabu.nwtest.server;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public interface ConnectionFactory<T extends AConnection> {
	T create( TestServerPort testServer, SocketChannel channel, SelectionKey key, Runnable xmitRequestListener );
}
