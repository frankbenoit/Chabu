package chabu.tester.data;

import java.nio.ByteBuffer;

public class CmdSetupChannelAdd extends ACmdScheduled {

	public final int channelId;
	public final int rxCount;
	public final int rxInitialOffset;
	public final int txInitialOffset;

	public CmdSetupChannelAdd(long schedTime, int channelId, int rxCount, int rxInitialOffset, int txInitialOffset ) {
		super( CommandId.SETUP_CHANNEL_ADD, schedTime );
		this.channelId = channelId;
		this.rxCount = rxCount;
		this.rxInitialOffset = rxInitialOffset;
		this.txInitialOffset = txInitialOffset;
	}
	@Override
	public void encode(ByteBuffer buf) {
		super.encode(buf);
		buf.put( (byte)channelId );
		buf.putInt( rxCount );
		buf.putInt( rxInitialOffset );
		buf.putInt( txInitialOffset );
	}
	
	static CmdSetupChannelAdd createSetupChannelAdd(ByteBuffer buf) {
		long time      = buf.getLong();
		int  channelId = buf.get() & 0xFF;
		int  rxCount   = buf.getInt();
		int  rxInitialOffset = buf.getInt();
		int  txInitialOffset = buf.getInt();
		return new CmdSetupChannelAdd(time, channelId, rxCount, rxInitialOffset, txInitialOffset );
	}

	@Override
	public String toString() {
		return String.format("Cmd[%s %s %s %s %s %s]", commandId, timeStr(schedTime), channelId, rxCount, rxInitialOffset, txInitialOffset );
	}


}
