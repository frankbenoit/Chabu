package chabu.tester.data;

public abstract class ACmdScheduled extends ACommand {
	
	public final long schedTime;

	protected ACmdScheduled( CommandId commandId, long schedTime ){
		super( commandId );
		this.schedTime = schedTime;
	}
}
