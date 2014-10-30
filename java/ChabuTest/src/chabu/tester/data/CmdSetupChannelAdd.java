package chabu.tester.data;

import java.nio.ByteBuffer;

public class CmdSetupChannelAdd extends ACmdScheduled {

	public final int channelId;
	public final int rxCount;

	public CmdSetupChannelAdd(long schedTime, int channelId, int rxCount) {
		super( CommandId.SETUP_CHANNEL_ADD, schedTime );
		this.channelId = channelId;
		this.rxCount = rxCount;
	}
	@Override
	public void encode(ByteBuffer buf) {
		super.encode(buf);
		buf.put( (byte)channelId );
		buf.putInt( rxCount );
	}
	
	static CmdSetupChannelAdd createSetupChannelAdd(ByteBuffer buf) {
		long time      = buf.getLong();
		int  channelId = buf.get() & 0xFF;
		int  rxCount   = buf.getInt();
		return new CmdSetupChannelAdd(time, channelId, rxCount );
	}

	@Override
	public String toString() {
		return String.format("Cmd[%s %s %s %s]", commandId, schedTime, channelId, rxCount );
	}


}
