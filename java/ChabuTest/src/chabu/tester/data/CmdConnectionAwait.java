package chabu.tester.data;

import java.nio.ByteBuffer;

public class CmdConnectionAwait extends ACmdScheduled {

	public final int port;

	public CmdConnectionAwait( long schedTime, int port ){
		super( CommandId.CONNECTION_AWAIT, schedTime );
		this.port = port;
	}
	
	@Override
	public void encode(ByteBuffer buf) {
		super.encode(buf);
		buf.putShort((short)port);
	}

	static CmdConnectionAwait createConnectionAwait(ByteBuffer buf) {
		long time = buf.getLong();
		int  port = buf.getShort();
		return new CmdConnectionAwait( time, port );
	}

	@Override
	public String toString() {
		return String.format("Cmd[%s %s %s]", commandId, timeStr(schedTime), port );
	}

}
