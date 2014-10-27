package chabu.tester.data;

public class CmdDutDisconnect extends ACmdScheduled {

	public CmdDutDisconnect( long schedTime ){
		super( CommandId.DUT_DISCONNECT, schedTime );
	}
	
}
