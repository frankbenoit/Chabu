package mctcp.tester;

public enum CommandId {
	
	TIME_BROADCAST     (0),
	CLOSE_APPLICATION  (1),
	START_CONNECTION   (2),
	AWAIT_CONNECTION   (3),
	CLOSE_CONNECTION   (4),
	ADD_CHANNEL        (5),
	ACTION_CHANNEL     (6),
	CREATE_CHANNEL_STAT(7),
	;
	
	private final int id;

	private CommandId( int id ){
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
}
