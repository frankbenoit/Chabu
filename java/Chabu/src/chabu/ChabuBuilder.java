package chabu;

import chabu.internal.Chabu;
import chabu.internal.ChabuChannel;
import chabu.internal.Utils;

public class ChabuBuilder {
	
	private Chabu chabu;
	private int channelId;

	private ChabuBuilder( ChabuConnectingInfo ci ){
		chabu = new Chabu( ci );
	}

	public static ChabuBuilder start( ChabuConnectingInfo ci ){
		return new ChabuBuilder(ci);
	}
	public ChabuBuilder addChannel( int channelId, int recvBufferSize, int xmitBufferSize, int priority, IChabuChannelUser user ){
		Utils.ensure( channelId == this.channelId, "Channel ID must be ascending, expected %s, but was %s", this.channelId, channelId );
		ChabuChannel channel = new ChabuChannel( recvBufferSize, xmitBufferSize );
		channel.setPriority(priority);
		channel.setNetworkUser( user );
		chabu.addChannel( channel );
		this.channelId++;
		return this;
	}
	
	public ChabuBuilder setPriorityCount(int priorityCount) {
		chabu.setPriorityCount(priorityCount);
		return this;
	}

	public ChabuBuilder setNetwork(IChabuNetwork nw) {
		chabu.setNetwork(nw);
		return this;
	}

	public ChabuBuilder setConnectionValidator( IChabuConnectingValidator val ) {
		chabu.setConnectingValidator( val );
		return this;
	}
	
	
	public IChabu build() {
		Chabu res = chabu;
		chabu = null;
		res.activate();
		return res;
	}
}
