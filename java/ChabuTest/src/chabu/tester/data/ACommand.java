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
				res = CmdTimeBroadcast.createTimeBroadcast(buf);
			}
			else if( cmd == CommandId.DUT_APPLICATION_CLOSE.getId() ){
				res = CmdDutApplicationClose.createDutApplicationClose(buf);
			}
			else if( cmd == CommandId.CONNECTION_CLOSE.getId() ){
				res = CmdDutApplicationClose.createDutApplicationClose(buf);
			}
			else if( cmd == CommandId.CONNECTION_CONNECT.getId() ){
				res = CmdConnectionConnect.createConnectionStart(buf);
			}
			else if( cmd == CommandId.CONNECTION_AWAIT.getId() ){
				res = CmdConnectionAwait.createConnectionAwait(buf);
			}
			else if( cmd == CommandId.CHANNEL_ADD.getId() ){
				res = CmdChannelAdd.createChannelAdd(buf);
			}
			else if( cmd == CommandId.CHANNEL_ACTION.getId() ){
				res = CmdChannelAction.createChannelAction(buf);
			}
			else if( cmd == CommandId.CHANNEL_CREATE_STAT.getId() ){
				res = CmdChannelCreateStat.createChannelCreateStat(buf);
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

	/**
	 * Decode String from a leading length field as short and then the string as UTF-8 coded bytes.
	 * 
	 * @param bb
	 * @return
	 */
	protected static String decodeString( ByteBuffer bb ){
		int len = bb.getShort();
		byte[] data = new byte[len];
		bb.get( data );
		return new String( data, StandardCharsets.UTF_8 );
	}

	private static final byte[] nullBytes = new byte[0]; 
	
	/**
	 * Encode String to a leading length field as short and then the string as UTF-8 coded bytes.
	 * @param buf
	 * @param string
	 */
	protected static void encodeString(ByteBuffer buf, String string) {
		
		byte[] bytes = nullBytes;
		if( string != null ) {
			bytes = string.getBytes(StandardCharsets.UTF_8);
		}
				
		buf.putShort((short) bytes.length );
		buf.put( bytes );
	}
	
}
