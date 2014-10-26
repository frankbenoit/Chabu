package chabu.tester.data;

public class CmdConnectionAwait extends ACmdScheduled {

	public final int port;

	public CmdConnectionAwait( long schedTime, int port ){
		super( CommandId.CONNECTION_CONNECT, schedTime );
		this.port = port;
	}
	
}
