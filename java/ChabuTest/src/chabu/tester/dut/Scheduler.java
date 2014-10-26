package chabu.tester.dut;

import java.util.ArrayDeque;

import chabu.Utils;
import chabu.tester.data.ACmdScheduled;
import chabu.tester.data.ACommand;
import chabu.tester.data.CmdTimeBroadcast;
import chabu.tester.data.CommandId;

public class Scheduler {

	ArrayDeque<ACmdScheduled> commands = new ArrayDeque<>(100);
	
	private long timeOffset;
	private Thread thread;

	private boolean shutdown = false;
	
	public Scheduler(){
		thread = new Thread( this::threadFunc, "Scheduler");
		thread.start();
	}
	
	private void threadFunc(){
		synchronized( thread ){
			try{
				while( !shutdown ){

					if( commands.isEmpty() ){
						thread.wait();
						continue;
					}
					
					ACmdScheduled cmd = commands.element();
					long diff = System.nanoTime() - timeOffset + cmd.schedTime;
					long diffMillis = diff / 1_000_000;
					if( diffMillis <= 0 ){
						execute( commands.remove() );
					}
					else {
						thread.wait( diffMillis );
					}
				}
			}
			catch( InterruptedException e ){
				e.printStackTrace();
			}
			finally{
				
			}
		}
		
	}
	private void execute(ACmdScheduled cmd) {
		switch( cmd.commandId ){
		case TIME_BROADCAST:
			Utils.ensure(false);
			break;
		case APPLICATION_CLOSE:
			break;
		case CONNECTION_AWAIT:
			break;
		case CONNECTION_CONNECT:
			break;
		case CONNECTION_CLOSE:
			break;
		case CHANNEL_ADD:
			break;
		case CHANNEL_ACTION:
			break;
		case CHANNEL_CREATE_STAT:
			break;
		}
	}

	public void addCommand( ACommand cmd ){
		synchronized(thread){
			if( cmd.commandId == CommandId.TIME_BROADCAST ){
				Utils.ensure( cmd instanceof CmdTimeBroadcast );
				CmdTimeBroadcast tb = (CmdTimeBroadcast)cmd;
				timeOffset = tb.time - System.nanoTime();
				thread.notifyAll();
			}
			else {
				Utils.ensure( cmd instanceof ACmdScheduled );
				commands.add( (ACmdScheduled)cmd );
				if( commands.size() ==  1){
					thread.notifyAll();
				}
			}
		}
	}

	public void shutDown() throws InterruptedException{
		shutdown = true;
		if( thread != null ){
			thread.join();
		}
	}
}
