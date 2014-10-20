package mctcp.tester.client;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import mctcp.INetwork;
import mctcp.INetworkUser;
import mctcp.Utils;
import mctcp.tester.CommandId;

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
		
		if( buf.hasRemaining() ){
			int cmd = buf.get(0);
			if( cmd == CommandId.TIME_BROADCAST.getId() ){
				cmdTimeBroadcast   ( buf );
			}
			else if( cmd == CommandId.CLOSE_APPLICATION.getId() ){
				cmdCloseApplication( buf );
			}
			else if( cmd == CommandId.CLOSE_CONNECTION.getId() ){
				cmdCloseConnection ( buf );
			}
			else if( cmd == CommandId.START_CONNECTION.getId() ){
				cmdStartConnection ( buf );
			}
			else if( cmd == CommandId.AWAIT_CONNECTION.getId() ){
				cmdAwaitConnection ( buf );
			}
			else if( cmd == CommandId.ADD_CHANNEL.getId() ){
				cmAddChannel   ( buf );
			}
			else if( cmd == CommandId.ACTION_CHANNEL.getId() ){
				cmdActionChannel   ( buf );
			}
			else if( cmd == CommandId.CREATE_CHANNEL_STAT.getId() ){
				cmdCreateChannelStat   ( buf );
			}
			else {
				Utils.ensure( false, "unknown command id %d", cmd );
			}
		}
		
	}

	private void cmdCreateChannelStat(ByteBuffer buf) {
		if( buf.remaining() >= 10 ){
			Utils.ensure( buf.get() == CommandId.CREATE_CHANNEL_STAT.getId() );
			Command cmd = new Command();
			cmd.commandId = CommandId.ADD_CHANNEL;
			cmd.time      = buf.getLong();
			cmd.channelId = buf.get() & 0xFF;
			scheduler.addCommand(cmd);
		}
	}

	private void cmAddChannel(ByteBuffer buf) {
		if( buf.remaining() >= 12 ){
			Utils.ensure( buf.get() == CommandId.ADD_CHANNEL.getId() );
			Command cmd = new Command();
			cmd.commandId = CommandId.ADD_CHANNEL;
			cmd.time      = buf.getLong();
			cmd.channelId = buf.get() & 0xFF;
			cmd.rxCount   = buf.getInt();
			scheduler.addCommand(cmd);
		}
	}

	private void cmdActionChannel(ByteBuffer buf) {
		if( buf.remaining() >= 19 ){
			Utils.ensure( buf.get() == CommandId.TIME_BROADCAST.getId() );
			Command cmd = new Command();
			cmd.commandId = CommandId.ACTION_CHANNEL;
			cmd.time      = buf.getLong();
			cmd.channelId = buf.get() & 0xFFFF;
			cmd.txCount   = buf.getInt();
			cmd.rxCount   = buf.getInt();
			scheduler.addCommand(cmd);
		}
	}

	private void cmdCloseConnection(ByteBuffer buf) {
		if( buf.remaining() >= 9 ){
			Utils.ensure( buf.get() == CommandId.TIME_BROADCAST.getId() );
			Command cmd = new Command();
			cmd.commandId = CommandId.CLOSE_CONNECTION;
			cmd.time = buf.getLong();
			scheduler.addCommand(cmd);
		}
	}

	private void cmdStartConnection(ByteBuffer buf) {
		if( buf.remaining() >= 15 ){
			Utils.ensure( buf.get() == CommandId.TIME_BROADCAST.getId() );
			Command cmd = new Command();
			cmd.commandId = CommandId.START_CONNECTION;
			cmd.time = buf.getLong();
			cmd.address = buf.getInt();
			cmd.port    = buf.getShort();
			scheduler.addCommand(cmd);
		}
	}

	private void cmdAwaitConnection(ByteBuffer buf) {
		if( buf.remaining() >= 11 ){
			Utils.ensure( buf.get() == CommandId.TIME_BROADCAST.getId() );
			Command cmd = new Command();
			cmd.commandId = CommandId.AWAIT_CONNECTION;
			cmd.time = buf.getLong();
			cmd.port    = buf.getShort();
			scheduler.addCommand(cmd);
		}
	}

	private void cmdCloseApplication(ByteBuffer buf) {
		if( buf.remaining() >= 9 ){
			Utils.ensure( buf.get() == CommandId.TIME_BROADCAST.getId() );
			Command cmd = new Command();
			cmd.commandId = CommandId.AWAIT_CONNECTION;
			cmd.time = buf.getLong();
			scheduler.addCommand(cmd);
		}
	}

	private void cmdTimeBroadcast(ByteBuffer buf) {
		if( buf.remaining() >= 9 ){
			Utils.ensure( buf.get() == CommandId.TIME_BROADCAST.getId() );
			Command cmd = new Command();
			cmd.commandId = CommandId.TIME_BROADCAST;
			cmd.time = buf.getLong();
			scheduler.addCommand(cmd);
		}
		
	}

	@Override
	public void evXmit(ByteBuffer bufferToFill) {
	}

}
