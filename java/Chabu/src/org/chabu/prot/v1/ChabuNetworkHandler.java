package org.chabu.prot.v1;

import java.io.IOException;
import java.nio.channels.ByteChannel;

public interface ChabuNetworkHandler {

	void handleChannel( ByteChannel channel ) throws IOException;

}
