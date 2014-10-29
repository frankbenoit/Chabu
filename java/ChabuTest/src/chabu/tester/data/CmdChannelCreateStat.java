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
		buf.put( (byte) channelId );
	}
}
