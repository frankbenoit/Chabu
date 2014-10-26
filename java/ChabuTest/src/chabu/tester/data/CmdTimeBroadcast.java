package chabu.tester.data;

public class CmdTimeBroadcast extends ACommand {

	public final long time;

	public CmdTimeBroadcast( long time ){
		super( CommandId.CONNECTION_CONNECT );
		this.time = time;
	}
	
}
