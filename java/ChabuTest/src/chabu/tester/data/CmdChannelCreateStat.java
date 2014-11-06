package chabu.tester.data;

import java.nio.ByteBuffer;

public class CmdChannelCreateStat extends ACmdScheduled {

	public final int channelId;

	public CmdChannelCreateStat(long schedTime, int channelId) {
		super( CommandId.CHANNEL_CREATE_STAT, schedTime );
		this.channelId = channelId;
	}
	
	@Override
	public void encode(ByteBuffer buf) {
		super.encode(buf);
		buf.putShort( (short) channelId );
	}
	
	static ACommand createChannelCreateStat(ByteBuffer buf) {
		long time      = buf.getLong();
		int  channelId = buf.getShort() & 0xFFFF;
		return new CmdChannelCreateStat(time, channelId );
	}

	@Override
	public String toString() {
		return String.format("Cmd[%s %s %s]", commandId, timeStr(schedTime), channelId );
	}


}
