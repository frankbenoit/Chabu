package org.chabu.nwtest.server;

import java.io.IOException;

import org.chabu.prot.v1.Chabu;

public interface TestServerPort {
	void close();
	void setChabu(Chabu chabu);
	void connectSync(String hostName, int port);
	void expectClose();
	void ensureClosed();
}
