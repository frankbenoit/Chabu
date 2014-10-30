package chabu.tester.data;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public abstract class AXferItem {
	
	protected abstract void encode( ByteBuffer buf );

	public static void encodeItem( ByteBuffer buf, AXferItem item ){
		int pos = buf.position();
		buf.putInt(123); // fill dummy length
		item.encode(buf);
		int packetSz = buf.position() - pos - 4;
		//System.out.printf("ACommand.encode packetSz:%s %s\n", packetSz, buf);
		buf.putInt( pos, packetSz ); // now make the length valid
	}

	/**
	 * Decode String from a leading length field as short and then the string as UTF-8 coded bytes.
	 * 
	 * @param bb
	 * @return
	 */
	protected static String decodeString( ByteBuffer bb ){
		int len = bb.getShort();
		byte[] data = new byte[len];
		bb.get( data );
		return new String( data, StandardCharsets.UTF_8 );
	}

	private static final byte[] nullBytes = new byte[0]; 
	
	/**
	 * Encode String to a leading length field as short and then the string as UTF-8 coded bytes.
	 * @param buf
	 * @param string
	 */
	protected static void encodeString(ByteBuffer buf, String string) {
		
		byte[] bytes = nullBytes;
		if( string != null ) {
			bytes = string.getBytes(StandardCharsets.UTF_8);
		}
				
		buf.putShort((short) bytes.length );
		buf.put( bytes );
	}

}
