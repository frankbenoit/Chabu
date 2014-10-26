package chabu.tester.dut;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import chabu.INetwork;
import chabu.INetworkUser;
import chabu.Utils;
import chabu.tester.data.ACommand;
import chabu.tester.data.CmdApplicationClose;
import chabu.tester.data.CmdChannelAction;
import chabu.tester.data.CmdChannelAdd;
import chabu.tester.data.CmdChannelCreateStat;
import chabu.tester.data.CmdConnectionAwait;
import chabu.tester.data.CmdConnectionClose;
import chabu.tester.data.CmdConnectionConnect;
import chabu.tester.data.CmdTimeBroadcast;
import chabu.tester.data.CommandId;

public class ControlConnection implements INetworkUser {

	private Scheduler scheduler;
	
	public ControlConnection() {
		scheduler = new Scheduler();
	}
	
	@Override
	public void setNetwork(INetwork nw) {
	}

	@Override
	public void evRecv(ByteBuffer buf) {
		
		buf.order( ByteOrder.BIG_ENDIAN );
		
		if( buf.remaining() >= 5 ){
			buf.mark();
			int len = buf.getInt();
			if( buf.remaining() < len+4 ){
				buf.reset();
				return;
			}
			int startPos = buf.position();
			int cmd = buf.get(0);
			if( cmd == CommandId.TIME_BROADCAST.getId() ){
				cmdTimeBroadcast   ( buf );
			}
			else if( cmd == CommandId.APPLICATION_CLOSE.getId() ){
				cmdCloseApplication( buf );
			}
			else if( cmd == CommandId.CONNECTION_CLOSE.getId() ){
				cmdCloseConnection ( buf );
			}
			else if( cmd == CommandId.CONNECTION_CONNECT.getId() ){
				cmdStartConnection ( buf );
			}
			else if( cmd == CommandId.CONNECTION_AWAIT.getId() ){
				cmdAwaitConnection ( buf );
			}
			else if( cmd == CommandId.CHANNEL_ADD.getId() ){
				cmAddChannel   ( buf );
			}
			else if( cmd == CommandId.CHANNEL_ACTION.getId() ){
				cmdActionChannel   ( buf );
			}
			else if( cmd == CommandId.CHANNEL_CREATE_STAT.getId() ){
				cmdCreateChannelStat   ( buf );
			}
			else {
				Utils.ensure( false, "unknown command id %d", cmd );
			}
			int endPos = buf.position();
			Utils.ensure( endPos - startPos == len );
		}
		
	}

	private void cmdCreateChannelStat(ByteBuffer buf) {
		long time      = buf.getLong();
		int  channelId = buf.get() & 0xFF;
		ACommand cmd = new CmdChannelCreateStat(time, channelId );
		scheduler.addCommand(cmd);
	}

	private void cmAddChannel(ByteBuffer buf) {
		long time      = buf.getLong();
		int  channelId = buf.get() & 0xFF;
		int  rxCount   = buf.getInt();
		ACommand cmd = new CmdChannelAdd(time, channelId, rxCount );
		scheduler.addCommand(cmd);
	}

	private void cmdActionChannel(ByteBuffer buf) {
		long time     = buf.getLong();
		int channelId = buf.get() & 0xFFFF;
		int txCount   = buf.getInt();
		int rxCount   = buf.getInt();
		ACommand cmd = new CmdChannelAction(time, channelId, txCount, rxCount );
		scheduler.addCommand(cmd);
	}

	private void cmdCloseConnection(ByteBuffer buf) {
		long time = buf.getLong();
		ACommand cmd = new CmdConnectionClose(time);
		scheduler.addCommand(cmd);
	}

	private void cmdStartConnection(ByteBuffer buf) {
		long   time    = buf.getLong();
		String address = extractString( buf );
		int    port    = buf.getShort();
		ACommand cmd = new CmdConnectionConnect(time, address, port );
		scheduler.addCommand(cmd);
	}

	private String extractString( ByteBuffer bb ){
		int len = bb.getShort();
		byte[] data = new byte[len];
		bb.get( data );
		return new String( data, StandardCharsets.UTF_8 );
	}
	private void cmdAwaitConnection(ByteBuffer buf) {
		long time = buf.getLong();
		int  port = buf.getShort();
		ACommand cmd = new CmdConnectionAwait( time, port );
		scheduler.addCommand(cmd);
	}

	private void cmdCloseApplication(ByteBuffer buf) {
		long time = buf.getLong();
		ACommand cmd = new CmdApplicationClose( time );
		scheduler.addCommand(cmd);
	}

	private void cmdTimeBroadcast(ByteBuffer buf) {
		long time = buf.getLong();
		ACommand cmd = new CmdTimeBroadcast( time );
		scheduler.addCommand(cmd);
	}

	@Override
	public void evXmit(ByteBuffer bufferToFill) {
	}

}
