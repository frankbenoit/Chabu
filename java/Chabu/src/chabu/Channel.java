package chabu;

import java.nio.ByteBuffer;

import chabu.ILogConsumer.Category;



public final class Channel {

	private static final int PACKETFLAG_ARM = 0x0001;
	private static final int PACKETFLAG_SEQ = 0x0002;
	private static final int Chabu_HEADER_SEQ_SIZE = 10;
	private static final int Chabu_HEADER_SIZE_MAX = 10;
	private static final int Chabu_HEADER_ARM_SIZE = 8;

	private int     channelId    = -1;
	private String  instanceName = "ChabuChannel[<not yet active>]";
	
	private Chabu        chabu;
	private IChabuChannelUser user;
	
	private ByteBuffer xmitBuffer;
	private int        xmitSeq = 0;
	private int        xmitArm = 0;
	
	private ByteBuffer xmitHeader = ByteBuffer.allocate(10);
	private int        xmitLastIndex = 0;
	private int        xmitLastLength = 0;
	
	private boolean    recvArmShouldBeXmit  = false;
	
	private ByteBuffer recvBuffer;
	private int        recvSeq = 0;
	private int        recvArm = 0;
	
	int        priority = 0;
	
	public Channel(int recvBufferSize, int xmitBufferSize ) {
		
		this.recvBuffer  = ByteBuffer.allocate(recvBufferSize);
		this.recvArm = recvBufferSize;
		this.recvArmShouldBeXmit = true;

	}
	
	/**
	 * Set the priority, lowest prior is 0.
	 *  
	 * @param priority
	 */
	public void setPriority( int priority ){
		Utils.ensure( chabu == null, "only allowed when not yet activated" );
		this.priority = priority;
	}

	void activate(Chabu chabu, int channelId ){

		this.xmitHeader.order( chabu.getByteOrder() );
		
		this.chabu      = chabu;
		this.channelId  = channelId;
		this.instanceName = String.format("%s.ch%d", chabu.instanceName, channelId );
	}
	
	public void setNetworkUser(IChabuChannelUser user) {
		this.user = user;
		user.setChannel(this);
	}
	
	public void evUserRecvRequest(){
		log(Category.CHABU_USER, "evUserRecvRequest()");
	}

	public void evUserXmitRequest(){
		log(Category.CHABU_USER, "evUserXmitRequest()");
		chabu.evUserXmitRequest(channelId);
	}

	/**
	 * Call this, to force the channel user evRecv be used.
	 */
	public void pushRecvData(){
		handleRecv(null);
	}
	void handleRecv( ByteBuffer buf ) {
		log(Category.CHANNEL_INT, "ChRecv %s %s", this, buf );

//TODO is this really needed
//		if( recvBuffer.position() > 0 ){
			callUserToTakeRecvData();
//		}
//		user.evRecv( null );
		if( buf == null ){
			return;
		}
		
		if( buf.remaining() < 8 ){
			return;
		}
		int pkf = buf.getShort(buf.position()+2) & 0xFFFF;
		if( pkf == PACKETFLAG_ARM ){
			handleRecvArm( buf.getInt(buf.position()+4) );
			buf.position( buf.position() + 8 );
			return;
		}
		else if( pkf == PACKETFLAG_SEQ ){
			
			if( buf.remaining() < 10 ){
				return;
			}
			int seq = buf.getInt  (buf.position()+4);
			int pls = buf.getShort(buf.position()+8) & 0xFFFF;
			
			Utils.ensure( this.recvSeq == seq );
			
			if( buf.remaining() < 10+pls ){
				return; // packet not yet complete
			}
			if( recvBuffer.remaining() < pls ){
				return; // cannot take all payload, yet
			}
			
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
		log(Category.CHABU_USER, "ChRecvCallingUser %5s bytes avail", avail );
		user.evRecv( recvBuffer );
		int consumed = avail - recvBuffer.remaining();
		log(Category.CHABU_USER, "ChRecvCalledUser  %5s bytes consu", consumed  );
		recvBuffer.compact();
		if( consumed > 0 ){
			this.recvArm += consumed;
			this.recvArmShouldBeXmit = true;
			chabu.evUserXmitRequest(channelId);
		}
	}

	private void handleRecvArm(int arm) {
		if( this.xmitArm != arm ){
			chabu.evUserXmitRequest(channelId);
		}
		this.xmitArm = arm;
	}

	private void log( Category cat, String fmt, Object ... args ){
		chabu.logHelper(cat, instanceName, fmt, args);
	}

	/**
	 * @return true if this channel needs to be continue xmitting
	 */
	boolean handleXmit( ByteBuffer buf ) {
		log( Category.CHANNEL_INT, "ChXmit %s", toString() );

//		if( !buf.hasRemaining() ){
//			return ( xmitLastLength > 0 && xmitLastIndex < xmitLastLength );
//		}
		
		if( this.xmitBuffer == null ){
			this.xmitBuffer = ByteBuffer.allocate( chabu.getMaxXmitPayloadSize() );
			this.xmitBuffer.order( chabu.getByteOrder() );
		}


		// get/fill xmit data from user
		// xmitBuffer in filling mode
		if( xmitBuffer.hasRemaining() ){
			int avail = xmitBuffer.remaining();
			log(Category.CHABU_USER, "ChXmitCallingUser %5s bytes free", avail );
			user.evXmit(xmitBuffer);
			int consumed = avail - xmitBuffer.remaining();
			log(Category.CHABU_USER, "ChXmitCalledUser  %5s bytes fill", consumed );
		}

		if( xmitLastIndex == 0 && xmitLastLength == 0 ){
			// start new header
			
			if( recvArmShouldBeXmit ){
				recvArmShouldBeXmit = false;
				
				xmitHeader.clear();
				
				xmitHeader.putShort( (short)channelId );      // CID
				xmitHeader.putShort( (short)PACKETFLAG_ARM ); // PKF
				xmitHeader.putInt( recvArm );                 // ARM
				xmitHeader.flip();
				log( Category.CHANNEL_INT, "ChXmit recvArm=%d", recvArm );
				xmitLastLength = Chabu_HEADER_ARM_SIZE;
			}
			else {
				int pls = xmitBuffer.position();
				Utils.ensure( pls >= 0 && pls <= 0xFFFF );
				if( pls > chabu.getMaxXmitPayloadSize() ){
					pls = chabu.getMaxXmitPayloadSize();
				}
				int remainArm = xmitArm - xmitSeq;
				if( pls > remainArm ){
					pls = remainArm;
				}

				Utils.ensure( pls >= 0 ); // negative shall not occur
				if( pls > 0 ){

					xmitHeader.clear();
					
					xmitHeader.putShort( (short)channelId );
					xmitHeader.putShort( (short)PACKETFLAG_SEQ );
					xmitHeader.putInt( xmitSeq );
					xmitHeader.putShort( (short)pls );
					xmitLastLength = pls + Chabu_HEADER_SEQ_SIZE;
					xmitSeq += pls;
					
					xmitHeader.flip();
				}

			}
			
		}
		

		if( xmitLastLength != 0 ){

			// block header
			if(( xmitLastIndex < Chabu_HEADER_SIZE_MAX ) && ( xmitLastIndex < xmitLastLength ) && buf.hasRemaining()){

				int copySz = xmitLastLength - xmitLastIndex;
				if( copySz > Chabu_HEADER_SIZE_MAX ){
					copySz = Chabu_HEADER_SIZE_MAX;
				}
				if( copySz > buf.remaining() ){
					copySz = buf.remaining();
				}

				Utils.ensure( copySz >= 0 );

				int hdrLimit = xmitHeader.limit();
				xmitHeader.limit( xmitHeader.position() + copySz );

				int oldPos = buf.position();
				buf.put( xmitHeader );
				Utils.ensure( oldPos + copySz == buf.position() );
				xmitHeader.limit(hdrLimit);
				xmitLastIndex += copySz;

			}

			// block payload
			if(( xmitLastIndex >= Chabu_HEADER_SIZE_MAX ) && ( xmitLastIndex < xmitLastLength ) && buf.hasRemaining() ){

				int copySz = xmitLastLength - xmitLastIndex;
				if( copySz > buf.remaining() ){
					copySz = buf.remaining();
				}

				// xmitBuffer to consume/xmit mode
				xmitBuffer.flip();
				
				Utils.ensure(( copySz >= 0 ) && ( copySz <= buf.remaining() ) && ( copySz <= xmitBuffer.remaining() ), "copySz:%s, buf.remaining():%s, xmitBuffer.remaining():%s", copySz, buf.remaining(), xmitBuffer.remaining());
				
				int xmitLimit = xmitBuffer.limit();
				xmitBuffer.limit( xmitBuffer.position() + copySz );
				int oldPos = buf.position();
				buf.put( xmitBuffer );
				Utils.ensure( oldPos + copySz == buf.position(), "oldPos:%s copySz:%s oldPos+copySz:%s buf.position():%s", oldPos, copySz, oldPos + copySz, buf.position() );
				xmitBuffer.limit(xmitLimit);

				xmitLastIndex += copySz;

				// xmitBuffer to back to filling mode
				xmitBuffer.compact();
				
				Utils.ensure( xmitBuffer.position() >= (xmitLastLength - xmitLastIndex), "xmitBuffer.position():%s >= (xmitLastLength:%s - xmitLastIndex:%s)", xmitBuffer.position(), xmitLastLength, xmitLastIndex );
			}
			

			// block completed
			if( xmitLastIndex >= xmitLastLength ){
				Utils.ensure( xmitLastIndex == xmitLastLength );
				xmitLastIndex  = 0;
				xmitLastLength = 0;
//				userCallback( userData, channel, Chabu_Channel_Event_Transmitted );
			}
		}

		// get/fill xmit data from user
		// xmitBuffer in filling mode
		if( xmitBuffer.hasRemaining() ){
			int avail = xmitBuffer.remaining();
			log(Category.CHABU_USER, "ChXmitCallingUser %5s bytes free", avail );
			user.evXmit(xmitBuffer);
			int consumed = avail - xmitBuffer.remaining();
			log(Category.CHABU_USER, "ChXmitCalledUser  %5s bytes fill", consumed );
		}
		//Check if data is still available
		if( xmitBuffer.position() > 0 ){
			//register next xmit on same channel.
			chabu.evUserXmitRequest(channelId);
		}
		
		// true if there is outstanding data for the current block
		return ( xmitLastLength > 0 ) && ( xmitLastIndex < xmitLastLength );
	}
	
	public String toString(){
		return String.format("Channel[%s recvS:%s recvA:%s xmitS:%s xmitA:%s]", channelId, this.recvSeq, this.recvArm, this.xmitSeq, this.xmitArm );
	}

	public int getId() {
		return channelId;
	}
	
}
