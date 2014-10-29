package chabu.tester.data;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import chabu.Utils;


public abstract class ACommand {
	
	protected ACommand(CommandId commandId) {
		this.commandId = commandId;
	}
	
	public final CommandId commandId;
	
	protected abstract void encode( ByteBuffer buf );
	
	public static void encodeCommand( ByteBuffer buf, ACommand cmd ){
		int pos = buf.position();
		buf.putInt(123); // fill dummy length
		cmd.encode(buf);
		int packetSz = buf.position() - pos - 4;
		//System.out.printf("ACommand.encode packetSz:%s %s\n", packetSz, buf);
		buf.putInt( pos, packetSz ); // now make the length valid
	}
	
	public static ACommand decodeCommand(ByteBuffer buf) {
		//System.out.printf("ACommand.decode %s\n", buf);
		if( buf.remaining() < 4 ) return null;
		
		int pos = buf.position();
		int packetSz = buf.getInt();
		
		//System.out.printf("ACommand.decode pos:%s sz:%s\n", pos, packetSz);
		if( buf.remaining() < packetSz ){
			buf.position(pos);
			return null;
		}
		
		ACommand res = null;
		try{
			int cmd = buf.get();
			if( cmd == CommandId.TIME_BROADCAST.getId() ){
				res = cmdTimeBroadcast   ( buf );
			}
			else if( cmd == CommandId.DUT_APPLICATION_CLOSE.getId() ){
				res = cmdCloseApplication( buf );
			}
			else if( cmd == CommandId.CONNECTION_CLOSE.getId() ){
				res = cmdCloseConnection ( buf );
			}
			else if( cmd == CommandId.CONNECTION_CONNECT.getId() ){
				res = cmdStartConnection ( buf );
			}
			else if( cmd == CommandId.CONNECTION_AWAIT.getId() ){
				res = cmdAwaitConnection ( buf );
			}
			else if( cmd == CommandId.CHANNEL_ADD.getId() ){
				res = cmAddChannel   ( buf );
			}
			else if( cmd == CommandId.CHANNEL_ACTION.getId() ){
				res = cmdActionChannel   ( buf );
			}
			else if( cmd == CommandId.CHANNEL_CREATE_STAT.getId() ){
				res = cmdCreateChannelStat   ( buf );
			}
			else {
				throw Utils.fail( "unknown command id %d", cmd );
			}
		}
		finally{
			if( res != null ){
				Utils.ensure( pos + packetSz + 4 == buf.position(), "%d %d +4 == %s", pos, packetSz, buf );
			}
			else {
				buf.position(pos);
			}
		}
		return res;
	}

	private static ACommand cmdCreateChannelStat(ByteBuffer buf) {
		long time      = buf.getLong();
		int  channelId = buf.get() & 0xFF;
		return new CmdChannelCreateStat(time, channelId );
	}

	private static ACommand cmAddChannel(ByteBuffer buf) {
		long time      = buf.getLong();
		int  channelId = buf.get() & 0xFF;
		int  rxCount   = buf.getInt();
		return new CmdChannelAdd(time, channelId, rxCount );
	}

	private static ACommand cmdActionChannel(ByteBuffer buf) {
		long time     = buf.getLong();
		int channelId = buf.get() & 0xFFFF;
		int txCount   = buf.getInt();
		int rxCount   = buf.getInt();
		return new CmdChannelAction(time, channelId, txCount, rxCount );
	}

	private static ACommand cmdCloseConnection(ByteBuffer buf) {
		long time = buf.getLong();
		return new CmdConnectionClose(time);
	}

	private static ACommand cmdStartConnection(ByteBuffer buf) {
		long   time    = buf.getLong();
		String address = decodeString( buf );
		int    port    = buf.getShort();
		return new CmdConnectionConnect(time, address, port );
	}

	private static ACommand cmdAwaitConnection(ByteBuffer buf) {
		long time = buf.getLong();
		int  port = buf.getShort();
		return new CmdConnectionAwait( time, port );
	}

	private static ACommand cmdCloseApplication(ByteBuffer buf) {
		long time = buf.getLong();
		return new CmdDutApplicationClose( time );
	}

	private static ACommand cmdTimeBroadcast(ByteBuffer buf) {
		long time = buf.getLong();
		return new CmdTimeBroadcast( time );
	}

	private static String decodeString( ByteBuffer bb ){
		int len = bb.getShort();
		byte[] data = new byte[len];
		bb.get( data );
		return new String( data, StandardCharsets.UTF_8 );
	}

	protected static void encodeString(ByteBuffer buf, String string) {
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		buf.putShort((short) bytes.length );
		buf.put( bytes );
	}
	
}
