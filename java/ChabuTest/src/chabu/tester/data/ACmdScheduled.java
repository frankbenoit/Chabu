package chabu.tester.data;

import java.nio.ByteBuffer;

public abstract class ACmdScheduled extends ACommand {
	
	public final long schedTime;

	protected ACmdScheduled( CommandId commandId, long schedTime ){
		super( commandId );
		this.schedTime = schedTime;
	}
	
	@Override
	public void encode(ByteBuffer buf) {
		buf.put( (byte)this.commandId.getId() );
		buf.putLong( this.schedTime );
	}

}
