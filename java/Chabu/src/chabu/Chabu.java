package chabu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;


public class Chabu implements INetworkUser {

	public static final int PROTOCOL_VERSION = 1;

	ILogConsumer logConsumer;
	
	private ArrayList<Channel> channels = new ArrayList<>(256);
	private int      priorityCount = 1;
	private BitSet[] xmitChannelRequest;
	private int      xmitChannelIdx = 0;
	private BitSet[] recvChannelRequest;
	private int      recvChannelIdx = 0;
	
	private INetwork nw;
	private boolean startupRx = true;/// TODO: what means startupRx ?
	private boolean startupTx = true;/// TODO: what means startupTx ?
	int maxChannelId;
	
	private boolean activated = false;
	
	String instanceName = "Chabu";
	
	private ChabuConnectingInfo infoLocal;
	private ChabuConnectingInfo infoRemote;

	private Channel xmitContinueChannel;

	
	
	public Chabu( ChabuConnectingInfo info ){
		this.infoLocal = info;
	}
	
	public void setLogConsumer( ILogConsumer log ){
		this.logConsumer = log;
	}
	/**
	 * The firmware does not allow to add channels during the operational time.
	 * The ModuleLink feature need to allow to add further channels during the runtime.
	 * The Cte must ensure itself that the channel is available at firmware side.
	 * @param channel
	 */
	public void addChannel( Channel channel ){
		channels.add(channel);
	}
	public void setPriorityCount( int priorityCount ){
		Utils.ensure( !activated );
		Utils.ensure( priorityCount >= 1 && priorityCount <= 20 );
		this.priorityCount = priorityCount;
	}
	
	int getMaxXmitPayloadSize(){
		return infoRemote.maxReceivePayloadSize;
	}
	
	/**
	 * TODO: What does activate do?
	 * 
	 * 
	 */
	public void activate(){
		Utils.ensure( channels.size() > 0 );
		for( int i = 0; i < channels.size(); i++ ){
			Channel ch = channels.get(i);
			ch.activate(this, i );
		}
		
		xmitChannelRequest = new BitSet[ priorityCount ];
		for (int i = 0; i < xmitChannelRequest.length; i++) {
			xmitChannelRequest[i] = new BitSet(channels.size());
			xmitChannelRequest[i].set(0, channels.size() );
		}
		
		recvChannelRequest = new BitSet[ priorityCount ];
		for (int i = 0; i < recvChannelRequest.length; i++) {
			recvChannelRequest[i] = new BitSet(channels.size());
			recvChannelRequest[i].set(0, channels.size() );
		}
		
		maxChannelId = channels.size() -1;
		activated = true;
	}
	
	/**
	 * TODO: TBC from Frank Benoit
	 * 
	 * Receive the data from XXX and transmit/shift/copy to YYY
	 * 
	 * @param buf: must contain a full chabu frame (incl. all protocol parameters) a frame part is NOK.
	 */
	public void evRecv(ByteBuffer buf) {
		log(ILogConsumer.Category.NW_CHABU, "evRecv()");
		
		while( calcNextRecvChannel() ){
			Channel ch = channels.get(recvChannelIdx);
			log( ILogConsumer.Category.CHABU_INT, "Ch[%s]Recv null", recvChannelIdx );
			ch.handleRecv(null);
		}
		
		int oldRemaining = -1;
		while( oldRemaining != buf.remaining() ){
			oldRemaining = buf.remaining();
			
			/// when is startupRx set before?
			if( startupRx ){
				Utils.ensure( activated );
				ChabuConnectingInfo info = recvProtocolParameterSpecification(buf);
				if( info != null ){
					this.infoRemote = info;
					//System.out.println("recv prot spec OK");
					startupRx = false;	
					continue;
				}
				break;
			}

			if( buf.remaining() < 8 ){
				break;
			}
			log( ILogConsumer.Category.CHABU_INT, "Recv pos %d", buf.position() );
			int channelId = buf.getShort(buf.position()+0) & 0xFFFF;
			Channel channel = channels.get(channelId);
			channel.handleRecv(buf);
			
		}
	}

	void evUserRecvRequest(int channelId){
		log(ILogConsumer.Category.CHABU_INT, "evUserRunRequest");
		synchronized(this){
			int priority = channels.get(channelId).priority;
			recvChannelRequest[priority].set( channelId );
		}
		nw.evUserRecvRequest();
	}

	void evUserXmitRequest(int channelId){
		log(ILogConsumer.Category.CHABU_INT, "evUserWriteRequest");
		synchronized(this){
			int priority = channels.get(channelId).priority;
			xmitChannelRequest[priority].set( channelId );
		}
		nw.evUserXmitRequest();
	}

	void logHelper(ILogConsumer.Category cat, String instanceName, String fmt, Object ... args ) {
		ILogConsumer log = this.logConsumer;
		
		if( cat == ILogConsumer.Category.NW_CHABU ) return;
		if( cat == ILogConsumer.Category.CHABU_INT ) return;
		if( cat == ILogConsumer.Category.CHANNEL_INT ) return;
//		if( cat == ILogConsumer.Category.CHABU_USER ) return;
		
		//System.out.printf("%s %s %s\n", cat, instanceName, String.format(fmt, args));
		if( log != null ){
			log.log( cat, instanceName, fmt, args);
		}
	}
	private void log(ILogConsumer.Category cat, String fmt, Object ... args ) {
		logHelper( cat, instanceName, fmt, args );
	}

	/**
	 * Copy the User data from the Channel to the buffer.
	 * 
	 * @param buf
	 * @return 	true 	not implemented
	 * 			false 	no flush performed.
	 */
	public boolean evXmit(ByteBuffer buf) {
		log(ILogConsumer.Category.NW_CHABU, "evXmit()");
		int oldRemaining = -1;
		while( buf.remaining() > 10 && oldRemaining != buf.remaining()){
			oldRemaining = buf.remaining();
			
			if( startupTx ){
				xmitProtocolParameterSpecification(buf);
				continue;
			}
			if( startupRx ){
				break;
			}

			if( xmitContinueChannel == null ){
				calcNextXmitChannel();
			}
			if( xmitContinueChannel != null ){
				Channel ch = xmitContinueChannel;
				xmitContinueChannel = null;
				boolean contXmit = ch.handleXmit( buf );
				if( contXmit ){
					this.xmitContinueChannel = ch;
				}
			}
		}
		return false; // flushing not implemented
	}

	private boolean calcNextXmitChannel() {
		synchronized(this){
			int idx = -1;
			for( int prio = priorityCount-1; prio >= 0 && idx < 0; prio-- ){
				if( xmitChannelIdx+1 < xmitChannelRequest[prio].size() ){
					idx = xmitChannelRequest[prio].nextSetBit(xmitChannelIdx+1);
				}
				if( idx < 0 ){
					idx = xmitChannelRequest[prio].nextSetBit(0);
				}
				if( idx >= 0 ){
					xmitChannelRequest[prio].clear(idx);
				}
			}
			
			if( idx >= 0 ){
				xmitChannelIdx = idx;
				xmitContinueChannel = channels.get(idx);
				return true;
			}
			else {
				return false;
			}
		}
	}

	private boolean calcNextRecvChannel() {
		synchronized(this){
			int idx = -1;
			for( int i = priorityCount-1; i >= 0 && idx < 0; i-- ){
				if( recvChannelIdx+1 < recvChannelRequest[i].size() ){
					idx = recvChannelRequest[i].nextSetBit(recvChannelIdx);
				}
				if( idx < 0 ){
					idx = recvChannelRequest[i].nextSetBit(0);
				}
				
				if( idx >= 0 ){
					recvChannelRequest[i].clear(idx);
				}
			}
			if( idx >= 0 ){
				recvChannelIdx = idx;
				return true;
			}
			else {
				return false;
			}
		}
	}
	
	public void setNetwork(INetwork nw) {
		Utils.ensure( this.nw == null && nw != null );
		this.nw = nw;
	}
	
	/**
	 * Put on the buffer the needed chabu protocol informations: chabu version, byte order, payloadsize, channel count
	 * 
	 * These values must be set previous to infoLocal
	 * 
	 * @param buf
	 */
	private void xmitProtocolParameterSpecification(ByteBuffer buf) {
		Utils.ensure( buf.order() == ByteOrder.BIG_ENDIAN );
		Utils.ensure( activated );
		Utils.ensure( buf.remaining() >= 11 );
		buf.put     ( (byte ) infoLocal.chabuProtocolVersion );
		buf.put     ( (byte ) (infoLocal.byteOrderBigEndian ? 1 : 0 ));
		buf.putShort( (short) infoLocal.maxReceivePayloadSize       );
		buf.putShort( (short) infoLocal.receiveCannelCount          );
		buf.putInt( infoLocal.applicationVersion );
		byte[] anlBytes = infoLocal.applicationName.getBytes( StandardCharsets.UTF_8 );
		Utils.ensure( anlBytes.length <= 255 );
		buf.put( (byte)anlBytes.length );
		buf.put( anlBytes );
		log(ILogConsumer.Category.CHABU_INT, "Protocol info local xmit");
		startupTx = false;
	}

	private ChabuConnectingInfo recvProtocolParameterSpecification(ByteBuffer buf) {
		
		Utils.ensure( buf.order() == ByteOrder.BIG_ENDIAN );
		
		if( buf.remaining() < 1 ){
			return null;
		}

		ChabuConnectingInfo info = new ChabuConnectingInfo();
		info.chabuProtocolVersion = buf.get(0) & 0xFF;
		
		if( info.chabuProtocolVersion != PROTOCOL_VERSION ){
			throw new ChabuConnectionAbortedException(String.format("Chabu protocol version mismatch. Expected %d, received %d", PROTOCOL_VERSION, info.chabuProtocolVersion ));
		}
		
		if( buf.remaining() < 11 ){
			return null;
		}

		info.byteOrderBigEndian    = ( buf.get(1) != 0 );
		
		if( info.byteOrderBigEndian != infoLocal.byteOrderBigEndian ){
			throw new ChabuConnectionAbortedException(String.format("Chabu protocol byte-order mismatch. local %s, remote %s", infoLocal.byteOrderBigEndian, info.byteOrderBigEndian ));
		}
		
		info.maxReceivePayloadSize = buf.getShort(2) & 0xFFFF;
		info.receiveCannelCount    = buf.getShort(4) & 0xFFFF;
		info.applicationVersion    = buf.getInt(6);
		int anl = buf.get(10) & 0xFF;
		if( buf.remaining() < 11+anl ){
			return null;
		}
			
		// ok, now really take received data
		buf.position( buf.position() + 11 );
		
		byte[] anlBytes = new byte[anl];
		buf.get( anlBytes );
		info.applicationName = new String( anlBytes, StandardCharsets.UTF_8 );
		log(ILogConsumer.Category.CHABU_INT, "Protocol info remote recv");
		//System.out.println("Chabu.recvProtocolParameterSpecification()");
		return info;
		
	}

	public ByteOrder getByteOrder() {
		return infoLocal.byteOrderBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
	}

}
