package chabu.tester.data;

public abstract class AResult {
	
	public final ResultId resultId;
	public final long time;
	
	
	AResult( ResultId resultId, long time ){
		this.resultId = resultId;
		this.time = time;
	}
}
