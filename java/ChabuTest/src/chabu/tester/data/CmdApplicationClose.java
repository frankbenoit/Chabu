package chabu.tester.data;

public class CmdApplicationClose extends ACmdScheduled {

	public CmdApplicationClose( long schedTime ){
		super( CommandId.APPLICATION_CLOSE, schedTime );
	}
	
}
