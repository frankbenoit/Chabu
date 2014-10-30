package chabu.tester.data;

import java.nio.ByteBuffer;

import chabu.Utils;

public class CmdDutDisconnect extends ACmdScheduled {

	public CmdDutDisconnect( long schedTime ){
		super( CommandId.DUT_DISCONNECT, schedTime );
	}
	
	@Override
	public void encode(ByteBuffer buf) {
		Utils.fail("not implemented");
	}

	@Override
	public String toString() {
		return String.format("Cmd[%s %s]", commandId, schedTime );
	}

}
