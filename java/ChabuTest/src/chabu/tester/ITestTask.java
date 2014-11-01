package chabu.tester;

public interface ITestTask {

	final long MSEC = 1_000_000L;
	final long SEC = 1_000*MSEC;

	void task( ChabuTestNw nw ) throws Exception;
}
