package chabu.tester.data;

public enum CommandId {
	
	TIME_BROADCAST        ( 0),
	DUT_CONNECT           ( 1),
	DUT_DISCONNECT        ( 2),
	DUT_APPLICATION_CLOSE ( 3),
	CONNECTION_CONNECT    ( 4),
	CONNECTION_AWAIT      ( 5),
	CONNECTION_CLOSE      ( 6),
	SETUP_CHANNEL_ADD     ( 7),
	SETUP_ACTIVATE        ( 8),
	CHANNEL_ACTION        ( 9),
	CHANNEL_CREATE_STAT   (10),
	;
	
	private final int id;

	private CommandId( int id ){
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
}
