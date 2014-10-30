package chabu.tester.data;

import java.nio.ByteBuffer;

import chabu.Utils;

public class CmdDutConnect extends ACmdScheduled {

	public final String address;
	public final int port;

	public CmdDutConnect( long schedTime, String address, int port ){
		super( CommandId.DUT_CONNECT, schedTime );
		this.address = address;
		this.port = port;
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
