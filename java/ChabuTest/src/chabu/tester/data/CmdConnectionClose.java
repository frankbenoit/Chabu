package chabu.tester.data;

import java.nio.ByteBuffer;

public class CmdConnectionClose extends ACmdScheduled {

	public CmdConnectionClose( long schedTime ){
		super( CommandId.CONNECTION_CLOSE, schedTime );
	}
	
	@Override
	public void encode(ByteBuffer buf) {
		super.encode(buf);
	}
	
	static CmdConnectionClose createConnectionClose(ByteBuffer buf) {
		long time = buf.getLong();
		return new CmdConnectionClose(time);
	}

	@Override
	public String toString() {
		return String.format("Cmd[%s %s]", commandId, timeStr(schedTime) );
	}

}
