package chabu.tester.data;

import java.nio.ByteBuffer;

public class ResultVersion extends AResult {

	public final int version;

	public ResultVersion(int version) {
		super(ResultId.VERSION, 0);
		this.version = version;
	}
	
	@Override
	public void encode(ByteBuffer buf) {
		buf.put( (byte)this.resultId.getId() );
		buf.put( (byte)this.version );
	}
	
	static ResultVersion createResultChannelStat(ByteBuffer buf) {
		int    version    = buf.get() & 0xFF;
		return new ResultVersion(version );
	}

}
