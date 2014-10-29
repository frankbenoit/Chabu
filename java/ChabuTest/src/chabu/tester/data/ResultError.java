package chabu.tester.data;

import java.nio.ByteBuffer;

public class ResultError extends AResult {

	public final int code;
	public final String message;

	ResultError(long time, int code, String message) {
		super(ResultId.ERROR, time);
		this.code = code;
		this.message = message;
	}
	
	@Override
	public void encode(ByteBuffer buf) {
		super.encode(buf);
		buf.putInt( (byte)this.code );
		AXferItem.encodeString(buf, message);
	}
	
	static ResultError createResultChannelStat(ByteBuffer buf) {
		long   time       = buf.getLong();
		int    code       = buf.getInt();
		String message    = AXferItem.decodeString(buf);
		return new ResultError(time, code, message );
	}

}
