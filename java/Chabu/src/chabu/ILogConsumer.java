package chabu;

public interface ILogConsumer {

	enum Category {
		CHABU, CHANNEL
	}
	
	void log( Category category, String instanceName, String text, Object ... args );
	
}
