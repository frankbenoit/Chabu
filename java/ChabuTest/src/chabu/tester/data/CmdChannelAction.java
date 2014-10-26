package chabu.tester.data;

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
	
}
