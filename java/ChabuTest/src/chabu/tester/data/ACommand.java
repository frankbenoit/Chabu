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
		buf.putInt(0); // fill dummy length
		cmd.encode(buf);
		buf.putInt( pos, buf.position() - pos - 4 ); // now make the length valid
	}
	
	public static ACommand decodeCommand(ByteBuffer buf) {
		int cmd = buf.get(0);
		if( cmd == CommandId.TIME_BROADCAST.getId() ){
			return cmdTimeBroadcast   ( buf );
		}
		else if( cmd == CommandId.DUT_APPLICATION_CLOSE.getId() ){
			return cmdCloseApplication( buf );
		}
		else if( cmd == CommandId.CONNECTION_CLOSE.getId() ){
			return cmdCloseConnection ( buf );
		}
		else if( cmd == CommandId.CONNECTION_CONNECT.getId() ){
			return cmdStartConnection ( buf );
		}
		else if( cmd == CommandId.CONNECTION_AWAIT.getId() ){
			return cmdAwaitConnection ( buf );
		}
		else if( cmd == CommandId.CHANNEL_ADD.getId() ){
			return cmAddChannel   ( buf );
		}
		else if( cmd == CommandId.CHANNEL_ACTION.getId() ){
			return cmdActionChannel   ( buf );
		}
		else if( cmd == CommandId.CHANNEL_CREATE_STAT.getId() ){
			return cmdCreateChannelStat   ( buf );
		}
		else {
			throw Utils.fail( "unknown command id %d", cmd );
		}
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
