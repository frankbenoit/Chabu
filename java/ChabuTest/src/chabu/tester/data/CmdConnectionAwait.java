package chabu.tester.data;

import java.nio.ByteBuffer;

public class CmdConnectionAwait extends ACmdScheduled {

	public final int port;

	public CmdConnectionAwait( long schedTime, int port ){
		super( CommandId.CONNECTION_CONNECT, schedTime );
		this.port = port;
	}
	
	@Override
	public void encode(ByteBuffer buf) {
		super.encode(buf);
		buf.putShort((short)port);
	}
}
