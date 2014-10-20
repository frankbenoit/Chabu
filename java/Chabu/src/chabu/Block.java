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
}
