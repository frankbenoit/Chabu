package chabu.tester.data;

public class CmdConnectionConnect extends ACmdScheduled {

	public final String address;
	public final int port;

	public CmdConnectionConnect( long schedTime, String address, int port ){
		super( CommandId.CONNECTION_CONNECT, schedTime );
		this.address = address;
		this.port = port;
	}
	
}
