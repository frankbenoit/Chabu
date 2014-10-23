package chabu;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.BitSet;

public class Chabu implements INetworkUser {

	private int txBytes;
	private int rxBytes;
	private ArrayList<Channel> channels = new ArrayList<>(256);
	private INetwork nw;
	private boolean startupRx = true;
	private boolean startupTx = true;
	final ByteOrder byteOrder;
	final int maxPayloadSize;
	int maxChannelId;
	private final int byteOrderId;
	
	private BufferPool bufferPool;
	private boolean activated = false;
	private BitSet xmitChannelRequest;
	private int    xmitChannelIdx = 0;
	
	private int   recvIdx   = 0;
	private Block recvBlock = new Block();

	private Block xmitBlock = new Block();
	String instanceName = "";
	
	
	public Chabu( ByteOrder byteOrder, int maxPayloadSize ){
		this.byteOrder = byteOrder;
		this.maxPayloadSize = maxPayloadSize;
		this.byteOrderId = ( ( byteOrder == ByteOrder.BIG_ENDIAN ) ?
				Constants.BYTEORDER_BIG_ENDIAN : Constants.BYTEORDER_LITTLE_ENDIAN );
	}
	
	public void addChannel( Channel channel ){
		Utils.ensure( startupRx && startupTx );
		Utils.ensure( !activated );
		channels.add(channel);
	}
	
	public void activate(){
		Utils.ensure( channels.size() > 0 );
		int bufferCount = 2;
		for( int i = 0; i < channels.size(); i++ ){
			Channel ch = channels.get(i);
			bufferCount += ch.rxBlocks;
			ch.activate(this, i );
		}
		bufferPool = new BufferPool( byteOrder, maxPayloadSize, bufferCount );
		xmitChannelRequest = new BitSet(channels.size());
		xmitChannelRequest.set(0, channels.size() );
		maxChannelId = channels.size() -1;
		xmitBlock.payload = bufferPool.allocBuffer();
		activated = true;
	}
	
	public void evRecv(ByteBuffer buf) {
		System.out.println("Chabu.evRecv() "+instanceName);
		int rxBytes = buf.remaining();
		int oldRemaining = -1;
		while( buf.hasRemaining() && oldRemaining != buf.remaining()){
			oldRemaining = buf.remaining();
			
			if( startupRx ){
				Utils.ensure( activated );
				recvProtocolParameterSpecification(buf);
				continue;
			}

			if( recvIdx == 0 && buf.remaining() >= Constants.HEADER_SIZE ){
				recvBlock.channelId   = buf.get() & 0xFF;
				recvBlock.payloadSize = buf.getShort() & 0xFFFF;
				recvBlock.seq         = buf.getShort() & 0xFFFF;
				recvBlock.arm         = buf.getShort() & 0xFFFF;
				recvIdx               = Constants.HEADER_SIZE;
				log( recvBlock, false );
				Utils.ensure( recvBlock.payloadSize <= maxPayloadSize    );
				Utils.ensure( recvBlock.channelId   <= channels.size() );

				if( recvBlock.payloadSize > 0 && recvBlock.payload == null ){
					recvBlock.payload = bufferPool.allocBuffer();
				}
				
				if( recvBlock.payload != null ) {
					recvBlock.payload.limit( recvBlock.payloadSize );
				}
			}
			if( recvIdx >= Constants.HEADER_SIZE ){
				if( recvBlock.payloadSize > 0 ){
					int limit = buf.limit();
					if( buf.remaining() > recvBlock.payload.remaining() ){
						buf.limit( buf.position() + recvBlock.payload.remaining() );
					}
					recvBlock.payload.put( buf );
					buf.limit( limit );
				}
				if( recvBlock.payloadSize == 0 || !recvBlock.payload.hasRemaining() ){

					Utils.ensure( recvBlock.channelId <= maxChannelId );
					Channel ch = channels.get( recvBlock.channelId );
					
					if( recvBlock.payloadSize > 0 ){
						recvBlock.payload.flip();
					}
					
					// when consuming the buffer, ch.handleRecv will do recvBlock.payload = null;
					ch.handleRecv( recvBlock );
					recvIdx = 0;
				}
			}
		}
		rxBytes -= buf.remaining();
		this.rxBytes += rxBytes;
	}

	void evUserReadRequest(int channelId){
		
	}
	void evUserWriteRequest(int channelId){
		synchronized(this){
			xmitChannelRequest.set( channelId );
		}
	}

	public void evXmit(ByteBuffer buf) {
		System.out.println("Chabu.evXmit() "+instanceName);
		int txBytes = buf.remaining();
		int oldRemaining = -1;
		while( buf.hasRemaining() && oldRemaining != buf.remaining()){
			oldRemaining = buf.remaining();
			
			if( startupTx ){
				xmitProtocolParameterSpecification(buf);
				startupTx = false;
				continue;
			}

			while( !xmitBlock.valid ){
				if( !calcNextXmitChannel() ){
					break;
				}
				Channel ch = channels.get(xmitChannelIdx);
				ch.handleXmit( xmitBlock );
			}
			if( xmitBlock.valid ){
				xmitData(buf);
			}
		}
		txBytes -= buf.remaining();
		this.txBytes = txBytes;
	}

	boolean logPackets = true;
	private void log( Block block, boolean isXmit ){
		if(logPackets){
			System.out.printf("%s-Packet-%s @%d [C:%d P:%-4d S:0x%04X A:0x%04X]\n",
					instanceName            ,
					isXmit ? "Xmit" : "Recv", 
					isXmit ? txBytes : rxBytes,
					block.channelId     , 
					block.payloadSize   , 
					block.seq           , 
					block.arm           );
		}
		
	}
	private void xmitData(ByteBuffer buf) {
		if( xmitBlock.codingIdx < Constants.HEADER_SIZE ){
			if( xmitBlock.codingIdx == 0 && buf.remaining() >= 1 ){
				log( xmitBlock, true );
				buf.put( (byte)xmitBlock.channelId );
				xmitBlock.codingIdx += 1;
			}
			if( xmitBlock.codingIdx == 1 && buf.remaining() >= 2 ){
				buf.putShort( (short)xmitBlock.payloadSize );
				xmitBlock.codingIdx += 2;
			}
			if( xmitBlock.codingIdx == 3 && buf.remaining() >= 2 ){
				buf.putShort( (short)xmitBlock.seq );
				xmitBlock.codingIdx += 2;
			}
			if( xmitBlock.codingIdx == 5 && buf.remaining() >= 2 ){
				buf.putShort( (short)xmitBlock.arm );
				xmitBlock.codingIdx += 2;
			}
		}
		if( xmitBlock.codingIdx >= Constants.HEADER_SIZE ){
			boolean finished = xmitBlock.payloadSize == 0;
			if( !finished ){
				buf.put(xmitBlock.payload);
				if( !xmitBlock.payload.hasRemaining() ){
					finished = true;
					xmitBlock.payload.clear();
				}
			}
			if( finished ){
				xmitBlock.valid = false;
				xmitBlock.codingIdx = 0;
			}
		}
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

	public void setNetwork(INetwork nw) {
		Utils.ensure( this.nw == null && nw != null );
		this.nw = nw;
	}
	
	
	private void xmitProtocolParameterSpecification(ByteBuffer buf) {
		Utils.ensure( activated );
		Utils.ensure( buf.remaining() >= 4 );
		buf.put     ( (byte ) Constants.PROTOCOL_VERSION );
		buf.put     ( (byte ) byteOrderId                );
		buf.putShort( (short) maxPayloadSize               );
		buf.put     ( (byte ) maxChannelId               );
	}

	private void recvProtocolParameterSpecification(ByteBuffer buf) {
		Utils.ensure( recvBlock.codingIdx <= 1 );
		if( recvBlock.codingIdx == 0 && buf.remaining() >= 1 ){
			int version = buf.get() & 0xFF;
			Utils.ensure( version == Constants.PROTOCOL_VERSION, "Protocol version does not match: received V%d but should be V%d", version, Constants.PROTOCOL_VERSION);
			recvBlock.codingIdx = 1;
		}
		if( recvBlock.codingIdx == 1 && buf.remaining() >= 4 ){
			
			int bo   = buf.get()      &   0xFF;
			int mps  = buf.getShort() & 0xFFFF;
			int mcid = buf.get()      &   0xFF;
			recvBlock.codingIdx += 3;
			
			Utils.ensure( bo   == byteOrderId  , "Byte order does not match: recv:%d expt:%d", bo, byteOrderId );
			Utils.ensure( mps  == maxPayloadSize , "Maximum payload size does not match: recv:%d expt:%d", mps, maxPayloadSize );
			Utils.ensure( mcid == maxChannelId , "Maximum channel ID does not match: recv:%d expt:%d", mcid, maxChannelId );
			startupRx = false;
			recvBlock.codingIdx = 0;
		}
	}

	void freeBlock(ByteBuffer buf) {
		bufferPool.freeBuffer(buf);
	}

}
