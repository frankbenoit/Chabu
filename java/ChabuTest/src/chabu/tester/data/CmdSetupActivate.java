package chabu.tester.data;

import java.nio.ByteBuffer;

public class CmdSetupActivate extends ACmdScheduled {

	public final boolean byteOrderBigEndian;
	public final int maxPayloadSize;

	public CmdSetupActivate(long schedTime, boolean byteOrderBigEndian, int maxPayloadSize ) {
		super( CommandId.SETUP_ACTIVATE, schedTime );
		this.byteOrderBigEndian = byteOrderBigEndian;
		this.maxPayloadSize = maxPayloadSize;
	}
	@Override
	public void encode(ByteBuffer buf) {
		super.encode(buf);
		buf.put( (byte)( byteOrderBigEndian ? 1 : 0 ));
		buf.putInt( maxPayloadSize );
	}
	
	static CmdSetupActivate createSetupActivate(ByteBuffer buf) {
		long    time      = buf.getLong();
		boolean byteOrderBigEndian = ( buf.get() != 0 ) ? true : false;
		int     maxPayloadSize     = buf.getInt();
		return new CmdSetupActivate(time, byteOrderBigEndian, maxPayloadSize );
	}

	@Override
	public String toString() {
		return String.format("Cmd[%s %s %s %s]", commandId, schedTime, byteOrderBigEndian, maxPayloadSize );
	}


}
