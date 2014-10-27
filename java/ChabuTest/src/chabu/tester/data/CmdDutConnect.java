package chabu.tester.data;

public class CmdDutConnect extends ACmdScheduled {

	public final String address;
	public final int port;

	public CmdDutConnect( long schedTime, String address, int port ){
		super( CommandId.DUT_CONNECT, schedTime );
		this.address = address;
		this.port = port;
	}
	
}
