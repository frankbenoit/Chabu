package chabu.tester.data;

import java.nio.ByteBuffer;

public class CmdConnectionConnect extends ACmdScheduled {

	public final String address;
	public final int port;

	public CmdConnectionConnect( long schedTime, String address, int port ){
		super( CommandId.CONNECTION_CONNECT, schedTime );
		this.address = address;
		this.port = port;
	}
	
	@Override
	public void encode(ByteBuffer buf) {
		super.encode(buf);
		AXferItem.encodeString( buf, address );
		buf.putShort( (short)this.port );
	}
	
	static CmdConnectionConnect createConnectionStart(ByteBuffer buf) {
		long   time    = buf.getLong();
		String address = AXferItem.decodeString( buf );
		int    port    = buf.getShort();
		return new CmdConnectionConnect(time, address, port );
	}

	@Override
	public String toString() {
		return String.format("Cmd[%s %s %s %s]", commandId, schedTime, address, port );
	}


}
