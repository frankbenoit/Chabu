package chabu.tester.data;

public class CmdDutApplicationClose extends ACmdScheduled {

	public CmdDutApplicationClose( long schedTime ){
		super( CommandId.DUT_APPLICATION_CLOSE, schedTime );
	}
	
}
