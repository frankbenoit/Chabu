package chabu.tester.data;

public enum CommandId {
	
	TIME_BROADCAST     (0),
	APPLICATION_CLOSE  (1),
	CONNECTION_CONNECT (2),
	CONNECTION_AWAIT   (3),
	CONNECTION_CLOSE   (4),
	CHANNEL_ADD        (5),
	CHANNEL_ACTION     (6),
	CHANNEL_CREATE_STAT(7),
	;
	
	private final int id;

	private CommandId( int id ){
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
}
