package mctcp;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.TreeMap;

public class MctcpConnector {
	
	private final ByteOrder byteOrder = ByteOrder.BIG_ENDIAN;
	private ByteChannel  transferChannel;
	private Block        currentRxBlock;
	private Block        currentTxBlock;
	private Channel      currentTxChannel;
	
	private TreeMap<Integer, Channel> channels = new TreeMap<>();
	private LinkedList<Channel> nextToSend = new LinkedList<>();
	
	void channelReadyToSend(Channel channel) {
		nextToSend.addFirst( channel );
	}
	
	public MctcpConnector( ByteChannel transferChannel, Channel[] channels ){
		this.transferChannel = transferChannel;
		for (int i = 0; i < channels.length; i++) {
			this.channels.put( channels[i].getId(), channels[i] );
		}
		
		for( Integer k : this.channels.keySet() ){
			Channel ch = this.channels.get(k);
			ch.setNetworkConnector( this );
		}
		
	}

	public void doIo(SelectionKey key) throws IOException {

		// always wants to read
		// overflow from reading is prevented by ARM flow control, 
		// so if the channel cannot store the block, this is an error.
		int interrestOps = SelectionKey.OP_READ;

		if( key.isReadable() ){
			if( currentRxBlock == null ){
				currentRxBlock = new Block( byteOrder );
				currentRxBlock.rxReset();
			}
			currentRxBlock.receiveContent( transferChannel );
			if( currentRxBlock.rxCanConsume() ){
				int channelId = currentRxBlock.getChannel();
				Channel channel = channels.get( channelId );
				channel.rxPutBlock( currentRxBlock );
				currentRxBlock = null;
			}
		}
		if( key.isWritable() ){
			if( currentTxBlock == null && !nextToSend.isEmpty() ){
				currentTxChannel = nextToSend.removeLast();
				currentTxBlock = currentTxChannel.txPopBlock();
			}
			if( currentTxBlock != null ){
				boolean completed = currentTxBlock.transmitContent( transferChannel );
				if( completed ){
					currentTxBlock = null;
				}
			}
			if( currentTxBlock != null || !nextToSend.isEmpty() ){
				interrestOps |= SelectionKey.OP_WRITE;
			}
		}
		key.interestOps( interrestOps );

	}



}
