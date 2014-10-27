package chabu.tester.data;

import java.nio.ByteBuffer;

public class CmdTimeBroadcast extends ACommand {

	public final long time;

	public CmdTimeBroadcast( long time ){
		super( CommandId.CONNECTION_CONNECT );
		this.time = time;
	}
	@Override
	public void encode(ByteBuffer buf) {
		buf.put( (byte)this.commandId.getId() );
		buf.putLong( this.time );
	}

}
