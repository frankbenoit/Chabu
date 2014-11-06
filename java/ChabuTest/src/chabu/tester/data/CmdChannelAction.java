package chabu.tester.data;

import java.nio.ByteBuffer;

public class CmdChannelAction extends ACmdScheduled {

	public final int channelId;
	public final int txCount;
	public final int rxCount;

	public CmdChannelAction(long schedTime, int channelId, int txCount, int rxCount) {
		super( CommandId.CHANNEL_ACTION, schedTime );
		this.channelId = channelId;
		this.txCount = txCount;
		this.rxCount = rxCount;
	}

	@Override
	public void encode(ByteBuffer buf) {
		buf.put( (byte)this.commandId.getId() );
		buf.putLong( this.schedTime );
		buf.putShort( (short)this.channelId );
		buf.putInt( this.txCount );
		buf.putInt( this.rxCount );
	}

	static CmdChannelAction createChannelAction(ByteBuffer buf) {
		long time     = buf.getLong();
		int channelId = buf.getShort() & 0xFFFF;
		int txCount   = buf.getInt();
		int rxCount   = buf.getInt();
		return new CmdChannelAction(time, channelId, txCount, rxCount );
	}

	@Override
	public String toString() {
		return String.format("Cmd[%s t:%s ch:%s rx:%5s tx:%5s]", commandId, timeStr(schedTime), channelId, rxCount, txCount );
	}


}
