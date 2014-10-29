package chabu.tester.data;

import java.nio.ByteBuffer;

public abstract class AResult extends AXferItem {
	
	public final ResultId resultId;
	public final long time;
	
	
	AResult( ResultId resultId, long time ){
		this.resultId = resultId;
		this.time = time;
	}

	@Override
	public void encode(ByteBuffer buf) {
		buf.put( (byte)this.resultId.getId() );
		buf.putLong( this.time );
	}

}
