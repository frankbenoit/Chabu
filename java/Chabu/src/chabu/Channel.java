package chabu;

import java.nio.ByteBuffer;

import chabu.ILogConsumer.Category;



public final class Channel implements INetwork {

	private static final int PACKETFLAG_ARM = 0x0001;
	private static final int PACKETFLAG_SEQ = 0x0002;

	private int     channelId    = -1;
	private String  instanceName = "ChabuChannel[<not yet active>]";
	
	private Chabu        chabu;
	private INetworkUser user;
	
	private ByteBuffer xmitBuffer;
	private int        xmitSeq = 0;
	private int        xmitArm = 0;
	
	private boolean    recvArmShouldBeXmit  = false;
	
	private ByteBuffer recvBuffer;
	private int        recvSeq = 0;
	private int        recvArm = 0;
	
	
	public Channel(int recvBufferSize, int xmitBufferSize ) {
		
		this.recvBuffer  = ByteBuffer.allocate(recvBufferSize);
		
		this.recvArm = recvBufferSize;
		this.recvArmShouldBeXmit = true;
	}

	void activate(Chabu chabu, int channelId ){
		this.chabu      = chabu;
		this.channelId  = channelId;
		this.instanceName = String.format("%s.ch%d", chabu.instanceName, channelId );
	}
	
	@Override
	public void setNetworkUser(INetworkUser user) {
		this.user = user;
	}
	
	@Override
	public void evUserRecvRequest(){
		log(Category.CHABU_USER, "evUserRecvRequest()");
	}

	@Override
	public void evUserXmitRequest(){
		log(Category.CHABU_USER, "evUserXmitRequest()");
		chabu.evUserWriteRequest(channelId);
	}

	void handleRecv( ByteBuffer buf ) {
		log(Category.CHANNEL_INT, "ChRecv %s %s", this, buf );
		if( recvBuffer.position() > 0 ){
			callUserToTakeRecvData();
		}
		if( buf.remaining() < 8 ){
			return;
		}
		int pkf = buf.getShort(2) & 0xFFFF;
		if( pkf == PACKETFLAG_ARM ){
			handleRecvArm( buf.getInt(4) );
			buf.position( buf.position() + 8 );
			return;
		}
		else if( pkf == PACKETFLAG_SEQ ){
			
			if( buf.remaining() < 10 ){
				return;
			}
			int pls = buf.getShort(8) & 0xFFFF;
			if( buf.remaining() < 10+pls ){
				return;
			}
			int seq = buf.getInt(4) & 0xFFFF;
			Utils.ensure( this.recvSeq == seq );
			buf.position( buf.position() + 10 );
			if( pls > 0 ){
				int oldLimit = buf.limit();
				buf.limit( buf.position() + pls );
				recvBuffer.put( buf );
				buf.limit( oldLimit );
				this.recvSeq += pls;
				callUserToTakeRecvData();
			}
			return;
		}
		else{
			throw Utils.fail("Unknown PKF %04X", pkf );
		}
	}
	private void callUserToTakeRecvData() {
		recvBuffer.flip();
		int avail = recvBuffer.remaining();
		log(Category.CHABU_USER, "ChRecvCallingUser %s: %s bytes available", this, avail );
		user.evRecv( recvBuffer );
		int consumed = avail - recvBuffer.remaining();
		log(Category.CHABU_USER, "ChRecvCalledUser %s: %s bytes consumed", this, consumed  );
		recvBuffer.compact();
		if( consumed > 0 ){
			this.recvArm += consumed;
			this.recvArmShouldBeXmit = true;
			chabu.evUserWriteRequest(channelId);
		}
	}

	private void handleRecvArm(int arm) {
		if( this.xmitArm != arm ){
			chabu.evUserWriteRequest(channelId);
		}
		this.xmitArm = arm;
	}

	private void log( Category cat, String fmt, Object ... args ){
		ILogConsumer log = chabu.log;
		if( log != null ){
			log.log( cat, instanceName, fmt, args );
		}
	}

	void handleXmit( ByteBuffer buf ) {
		log( Category.CHANNEL_INT, "ChXmit %s", toString() );

		int startPos = buf.position();
		
		if( recvArmShouldBeXmit && buf.remaining() >= 8 ){
			recvArmShouldBeXmit = false;
			buf.putShort( (short)channelId );      // CID
			buf.putShort( (short)PACKETFLAG_ARM ); // PKF
			buf.putInt( recvArm );                 // ARM
			log( Category.CHANNEL_INT, "ChXmit recvArm=%d", recvArm );
		}
		
		if( xmitBuffer == null ){
			xmitBuffer = ByteBuffer.allocate( chabu.getMaxXmitPayloadSize() );
			xmitBuffer.order( chabu.getByteOrder() );
		}
		if( xmitBuffer.position() > 0 ){
			
			xmitBuffer.flip();
			
			int pls = buf.remaining() - 10;
			if( pls > 0 ){
				if( pls > xmitBuffer.remaining() ){
					pls = xmitBuffer.remaining();
				}
				if( pls > 0xFFFF ){
					pls = 0xFFFF;
				}
				buf.putShort( (short) channelId );      // CID
				buf.putShort( (short) PACKETFLAG_SEQ ); // PKF
				buf.putInt( xmitSeq );                  // SEQ
				buf.putShort( (short)pls );             // PLS
				int oldLimit = xmitBuffer.limit();
				xmitBuffer.limit( xmitBuffer.position() + pls );
				buf.put( xmitBuffer );
				xmitBuffer.limit( oldLimit );
				log( Category.CHANNEL_INT, "ChXmit xmitSeq=%5d pls=%5d", xmitSeq, pls );
				xmitSeq += pls;
			}
			xmitBuffer.compact();
		}
		
		int xmitSz = buf.position() - startPos;
		if( xmitSz == 0 ){
			log( Category.CHANNEL_INT, "handleXmit no-action" );
		}
	}
	
	public String toString(){
		return String.format("Channel[%s recvS:%s recvA:%s xmitS:%s xmitA:%s]", channelId, this.recvSeq, this.recvArm, this.xmitSeq, this.xmitArm );
	}
	
}
