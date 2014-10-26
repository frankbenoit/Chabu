package chabu.tester.data;

public class ResultError extends AResult {

	public final int code;
	public final String message;

	ResultError(ResultId resultId, long time, int code, String message) {
		super(resultId, time);
		this.code = code;
		this.message = message;
	}

}
