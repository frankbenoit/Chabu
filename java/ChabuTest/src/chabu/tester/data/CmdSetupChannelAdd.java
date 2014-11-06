package chabu.tester.data;

import java.nio.ByteBuffer;

public class CmdSetupChannelAdd extends ACmdScheduled {

	public final int channelId;
	public final int rxCount;
	public final int txCount;
	public final int rxInitialOffset;
	public final int txInitialOffset;

	public CmdSetupChannelAdd(long schedTime, int channelId, int rxCount, int txCount, int rxInitialOffset, int txInitialOffset ) {
		super( CommandId.SETUP_CHANNEL_ADD, schedTime );
		this.channelId = channelId;
		this.rxCount = rxCount;
		this.txCount = txCount;
		this.rxInitialOffset = rxInitialOffset;
		this.txInitialOffset = txInitialOffset;
	}
	@Override
	public void encode(ByteBuffer buf) {
		super.encode(buf);
		buf.putShort( (short)channelId );
		buf.putInt( rxCount );
		buf.putInt( txCount );
		buf.putInt( rxInitialOffset );
		buf.putInt( txInitialOffset );
	}
	
	static CmdSetupChannelAdd createSetupChannelAdd(ByteBuffer buf) {
		long time            = buf.getLong();
		int  channelId       = buf.getShort() & 0xFFFF;
		int  rxCount         = buf.getInt();
		int  txCount         = buf.getInt();
		int  rxInitialOffset = buf.getInt();
		int  txInitialOffset = buf.getInt();
		return new CmdSetupChannelAdd(time, channelId, rxCount, txCount, rxInitialOffset, txInitialOffset );
	}

	@Override
	public String toString() {
		return String.format("Cmd[%s %s %s %s %5s %5s]", commandId, timeStr(schedTime), channelId, rxCount, txCount, rxInitialOffset, txInitialOffset );
	}


}
