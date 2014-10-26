package chabu.tester.data;

public enum ResultId {
	
	VERSION     (0),
	ERROR       (1),
	CHANNEL_STAT(2),
	;
	
	private final int id;

	private ResultId( int id ){
		this.id = id;
	}
	
	public int getId() {
		return id;
	}
}
