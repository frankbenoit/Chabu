package chabu.internal;

public enum PacketType {
	SETUP ( 0xF0, 10 ),
	ACCEPT( 0xE1,  1 ),
	ABORT ( 0xD2,  7 ),
	ARM   ( 0xC3,  7 ),
	SEQ   ( 0xB4,  9 ),
	;
	
	public final int id;
	public final int headerSize;

	private PacketType( int id, int minPacketSize ){
		this.id = id;
		this.headerSize = minPacketSize;
	}

	public static PacketType findPacketType( int id ){
		PacketType[] values = values();
		for( int i = 0; i < values.length; i++ ){
			if( values[i].id == id ) return values[i];
		}
		return null;
	}
}
