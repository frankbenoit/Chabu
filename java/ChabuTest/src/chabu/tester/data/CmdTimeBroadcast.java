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

	static CmdTimeBroadcast createTimeBroadcast(ByteBuffer buf) {
		long time = buf.getLong();
		return new CmdTimeBroadcast( time );
	}
	
	@Override
	public String toString() {
		return String.format("Cmd[%s %s]", commandId, time );
	}

}
