package chabu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;

import chabu.ILogConsumer.Category;

public class Chabu implements INetworkUser {

	public static final int PROTOCOL_VERSION = 1;

	ILogConsumer log;
	
	private ArrayList<Channel> channels = new ArrayList<>(256);
	private BitSet xmitChannelRequest;
	private int    xmitChannelIdx = 0;
	private BitSet recvChannelRequest;
	private int    recvChannelIdx = 0;
	
	private INetwork nw;
	private boolean startupRx = true;
	private boolean startupTx = true;
	int maxChannelId;
	
	private boolean activated = false;
	
	String instanceName = "Chabu";
	
	private ChabuConnectingInfo infoLocal;
	private ChabuConnectingInfo infoRemote;
	
	
	public Chabu( ChabuConnectingInfo info ){
		this.infoLocal = info;
	}
	
	public void setLogConsumer( ILogConsumer log ){
		this.log = log;
	}
	public void addChannel( Channel channel ){
		Utils.ensure( startupRx && startupTx );
		Utils.ensure( !activated );
		channels.add(channel);
	}
	
	int getMaxXmitPayloadSize(){
		return infoRemote.maxReceivePayloadSize;
	}
	
	public void activate(){
		Utils.ensure( channels.size() > 0 );
		for( int i = 0; i < channels.size(); i++ ){
			Channel ch = channels.get(i);
			ch.activate(this, i );
		}
		
		xmitChannelRequest = new BitSet(channels.size());
		xmitChannelRequest.set(0, channels.size() );
		
		recvChannelRequest = new BitSet(channels.size());
		recvChannelRequest.set(0, channels.size() );
		
		maxChannelId = channels.size() -1;
		activated = true;
	}
	
	public void evRecv(ByteBuffer buf) {
		log(Category.NW_CHABU, "evRecv()");
		
		while( calcNextRecvChannel() ){
			Channel ch = channels.get(recvChannelIdx);
			log( Category.CHABU_INT, "Ch[%s]Recv null", recvChannelIdx );
			ch.handleRecv(null);
		}
		
		int oldRemaining = -1;
		while( oldRemaining != buf.remaining() ){
			oldRemaining = buf.remaining();
			
			if( startupRx ){
				Utils.ensure( activated );
				ChabuConnectingInfo info = recvProtocolParameterSpecification(buf);
				if( info != null ){
					startupRx = false;	
					this.infoRemote = info;
					continue;
				}
				break;
			}

			if( buf.remaining() < 8 ){
				break;
			}
			log( Category.CHABU_INT, "Recv pos %d", buf.position() );
			int channelId = buf.getShort(buf.position()+0) & 0xFFFF;
			Channel channel = channels.get(channelId);
			channel.handleRecv(buf);
			
		}
	}

	void evUserRecvRequest(int channelId){
		log(Category.CHABU_INT, "evUserRunRequest");
		synchronized(this){
			recvChannelRequest.set( channelId );
		}
		nw.evUserRecvRequest();
	}

	void evUserXmitRequest(int channelId){
		log(Category.CHABU_INT, "evUserWriteRequest");
		synchronized(this){
			xmitChannelRequest.set( channelId );
		}
		nw.evUserXmitRequest();
	}

	void logHelper(ILogConsumer.Category cat, String instanceName, String fmt, Object ... args ) {
		ILogConsumer log = this.log;
		
		if( cat == Category.NW_CHABU ) return;
		if( cat == Category.CHABU_INT ) return;
		if( cat == Category.CHANNEL_INT ) return;
//		if( cat == Category.CHABU_USER ) return;
		
		if( log != null ){
			log.log( cat, instanceName, fmt, args);
		}
	}
	private void log(ILogConsumer.Category cat, String fmt, Object ... args ) {
		logHelper( cat, instanceName, fmt, args );
	}

	public boolean evXmit(ByteBuffer buf) {
		log(Category.NW_CHABU, "evXmit()");
		int oldRemaining = -1;
		while( buf.hasRemaining() && oldRemaining != buf.remaining()){
			oldRemaining = buf.remaining();
			
			if( startupTx ){
				startupTx = false;
				xmitProtocolParameterSpecification(buf);
				continue;
			}
			if( startupRx ){
				break;
			}

			while( true ){
				if( !calcNextXmitChannel() ){
					break;
				}
				Channel ch = channels.get(xmitChannelIdx);
				ch.handleXmit( buf );
			}
		}
		return false; // flushing not implemented
	}

	private boolean calcNextXmitChannel() {
		synchronized(this){
			int idx = -1;
			idx = xmitChannelRequest.nextSetBit(xmitChannelIdx);
			if( idx < 0 ){
				idx = xmitChannelRequest.nextSetBit(0);
			}
			if( idx >= 0 ){
				xmitChannelIdx = idx;
				xmitChannelRequest.clear(idx);
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
			idx = recvChannelRequest.nextSetBit(recvChannelIdx);
			if( idx < 0 ){
				idx = recvChannelRequest.nextSetBit(0);
			}
			if( idx >= 0 ){
				recvChannelIdx = idx;
				recvChannelRequest.clear(idx);
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
	
	
	private void xmitProtocolParameterSpecification(ByteBuffer buf) {
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
		log(Category.CHABU_INT, "Protocol info local xmit");
	}

	private ChabuConnectingInfo recvProtocolParameterSpecification(ByteBuffer buf) {
		
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
		log(Category.CHABU_INT, "Protocol info remote recv");
		return info;
		
	}

	public ByteOrder getByteOrder() {
		return infoLocal.byteOrderBigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
	}

}
