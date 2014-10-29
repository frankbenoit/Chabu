package chabu.tester.data;

import java.nio.ByteBuffer;

public class CmdTimeBroadcast extends ACommand {

	public final long time;

	public CmdTimeBroadcast( long time ){
		super( CommandId.TIME_BROADCAST );
		this.time = time;
	}
	@Override
	public void encode(ByteBuffer buf) {
		buf.put( (byte)this.commandId.getId() );
		buf.putLong( this.time );
	}

}
