package chabu.tester.dut;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import chabu.INetwork;
import chabu.INetworkUser;
import chabu.Utils;
import chabu.tester.data.ACommand;

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
			ACommand cmd = ACommand.decodeCommand(buf);
			int endPos = buf.position();
			Utils.ensure( endPos - startPos == len );
			scheduler.addCommand(cmd);
		}
		
	}


	@Override
	public void evXmit(ByteBuffer bufferToFill) {
	}

}
