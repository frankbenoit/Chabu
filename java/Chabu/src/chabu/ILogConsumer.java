package chabu;

public interface ILogConsumer {

	enum Category {
		CHABU_INT,
		CHANNEL_INT,
		NW_CHABU,
		CHABU_USER
	}
	
	void log( Category category, String instanceName, String text, Object ... args );
	
}
