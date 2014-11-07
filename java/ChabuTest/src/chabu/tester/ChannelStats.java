package chabu.tester;

public class ChannelStats {

	int channelId;
	DutId dutId;
	boolean isTx;
	
	double[] rate = new double[100];
	double[] time = new double[100];
	int lastTimestamp = Integer.MIN_VALUE;
	int count = 0;

}
