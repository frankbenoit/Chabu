package chabu;

import java.nio.ByteBuffer;

class Block {
	
	int     codingIdx;
	boolean valid;
	
	int channelId;
	int payloadSize;
	int seq;
	int arm;
	ByteBuffer payload;
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		if( payloadSize > 0 ){
			for( int i = 0; i < Math.min( 10, payloadSize); i++ ){
				if( i > 0 ){
					sb.append(" ");
				}
				sb.append( String.format( "%02X", payload.get(i) & 0xFF ));
			}
			if( payloadSize > 10 ){
				sb.append( "...");
			}
		}
		return String.format("Buffer[ch:%d pl:%d sq:%d ar:%d payload:%s]", channelId, payloadSize, seq, arm, sb );
	}
}
