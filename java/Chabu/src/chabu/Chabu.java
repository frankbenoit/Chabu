package chabu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;


public class Chabu {

	public static final int PROTOCOL_VERSION = 1;

	ILogConsumer logConsumer;
	
	private ArrayList<Channel> channels = new ArrayList<>(256);
	private int      priorityCount = 1;
	private BitSet[] xmitChannelRequest;
	private int      xmitChannelIdx = 0;
	private BitSet[] recvChannelRequest;
	private int      recvChannelIdx = 0;
	
	private IChabuNetwork nw;
	
	/**
	 * True until the startup data is completely received.
	 */
	private boolean startupRx = true;
	/**
	 * True until the startup data is completely sent.
	 */
	private boolean startupTx = true;

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
	 * Priority that forces the processing to be done with highest possible priority.
	 * Lower priority values result in later processing.
	 */
	public int getHighestValidPriority(){
		return priorityCount-1;
	}
	
	/**
	 * When activate is called, chabu enters operation. No subsequent calls to {@link #addChannel(Channel)} or {@link #setPriorityCount(int)} are allowed.
	 */
	public void activate(){
		Utils.ensure( channels.size() > 0 );
		for( int i = 0; i < channels.size(); i++ ){
			Channel ch = channels.get(i);
			Utils.ensure( ch.priority < priorityCount, "Channel %s has higher priority (%s) as the max %s", i, ch.priority, priorityCount );
			ch.activate(this, i );
		}
		
		xmitChannelRequest = new BitSet[ priorityCount ];
		for (int i = 0; i < xmitChannelRequest.length; i++) {
			xmitChannelRequest[i] = new BitSet(channels.size());
		}
		
		recvChannelRequest = new BitSet[ priorityCount ];
		for (int i = 0; i < recvChannelRequest.length; i++) {
			recvChannelRequest[i] = new BitSet(channels.size());
		}
		
		maxChannelId = channels.size() -1;
		activated = true;
	}
	
	/**
	 * Receive the data from the network and process it into the channels.
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
			chabu.Utils.ensure(priority < xmitChannelRequest.length, "priority:%s < xmitChannelRequest.length:%s", priority, xmitChannelRequest.length );
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
		while( buf.hasRemaining() && oldRemaining != buf.remaining()){
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
			for( int prio = priorityCount-1; prio >= 0; prio-- ){
				int idxCandidate = -1;
				BitSet prioBitSet = xmitChannelRequest[prio];
				
				// search from last channel pos on
				if( xmitChannelIdx+1 < prioBitSet.size() ){
					idxCandidate = prioBitSet.nextSetBit(xmitChannelIdx+1);
				}
				
				// try from idx zero
				if( idxCandidate < 0 ){
					idxCandidate = prioBitSet.nextSetBit(0);
				}
				
				// if found, clear and use it
				if( idxCandidate >= 0 ){
					prioBitSet.clear(idxCandidate);
					xmitChannelIdx = idxCandidate;
					xmitContinueChannel = channels.get(idxCandidate);
					Utils.ensure( xmitContinueChannel.priority == prio );
					return true;
				}
			}
			return false;
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
	
	public void setNetwork(IChabuNetwork nw) {
		Utils.ensure( this.nw == null && nw != null );
		this.nw = nw;
		nw.setChabu(this);
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
