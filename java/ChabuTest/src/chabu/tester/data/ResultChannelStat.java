package chabu.tester.data;

public class ResultChannelStat extends AResult {

	public final int channelId;
	public final int rxCount;
	public final int txCount;

	ResultChannelStat(ResultId resultId, long time, int channelId, int rxCount, int txCount ) {
		super(resultId, time);
		this.channelId = channelId;
		this.rxCount = rxCount;
		this.txCount = txCount;
	}

}
