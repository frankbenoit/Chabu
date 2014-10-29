package chabu.tester.data;

import java.nio.ByteBuffer;

public class CmdChannelAdd extends ACmdScheduled {

	public final int channelId;
	public final int rxCount;

	public CmdChannelAdd(long schedTime, int channelId, int rxCount) {
		super( CommandId.CHANNEL_ADD, schedTime );
		this.channelId = channelId;
		this.rxCount = rxCount;
	}
	@Override
	public void encode(ByteBuffer buf) {
		super.encode(buf);
		buf.put( (byte)channelId );
		buf.putShort( (short)rxCount );
	}
}
