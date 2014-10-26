package chabu.tester.data;

public class CmdConnectionClose extends ACmdScheduled {

	public CmdConnectionClose( long schedTime ){
		super( CommandId.CONNECTION_CLOSE, schedTime );
	}
	
}
