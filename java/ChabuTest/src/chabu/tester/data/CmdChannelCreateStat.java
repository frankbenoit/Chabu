package chabu.tester.data;

public class CmdChannelCreateStat extends ACmdScheduled {

	public final int channelId;

	public CmdChannelCreateStat(long schedTime, int channelId) {
		super( CommandId.CHANNEL_ACTION, schedTime );
		this.channelId = channelId;
	}
	
}
