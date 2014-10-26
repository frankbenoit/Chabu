package chabu.tester.data;

public class CmdChannelAdd extends ACmdScheduled {

	public final int channelId;
	public final int rxCount;

	public CmdChannelAdd(long schedTime, int channelId, int rxCount) {
		super( CommandId.CHANNEL_ACTION, schedTime );
		this.channelId = channelId;
		this.rxCount = rxCount;
	}
	
}
