package chabu.tester.data;

import java.nio.ByteBuffer;


public class CmdDutApplicationClose extends ACmdScheduled {

	public CmdDutApplicationClose( long schedTime ){
		super( CommandId.DUT_APPLICATION_CLOSE, schedTime );
	}
	
	static CmdDutApplicationClose createDutApplicationClose(ByteBuffer buf) {
		long time = buf.getLong();
		return new CmdDutApplicationClose( time );
	}

	@Override
	public String toString() {
		return String.format("Cmd[%s %s]", commandId, timeStr(schedTime) );
	}

}
