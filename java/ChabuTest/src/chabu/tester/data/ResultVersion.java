package chabu.tester.data;

public class ResultVersion extends AResult {

	public final int version;

	ResultVersion(ResultId resultId, long time, int version) {
		super(resultId, time);
		this.version = version;
	}

}
