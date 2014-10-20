package mctcp.tester.client;

import java.util.ArrayDeque;

import mctcp.Utils;
import mctcp.tester.CommandId;

public class Scheduler {

	ArrayDeque<Command> commands = new ArrayDeque<>(100);
	
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
					
					Command cmd = commands.element();
					long diff = System.nanoTime() - timeOffset + cmd.time;
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
	private void execute(Command cmd) {
		switch( cmd.commandId ){
		case TIME_BROADCAST:
			Utils.ensure(false);
			break;
		case CLOSE_APPLICATION:
			break;
		case AWAIT_CONNECTION:
			break;
		case START_CONNECTION:
			break;
		case CLOSE_CONNECTION:
			break;
		case ADD_CHANNEL:
			break;
		case ACTION_CHANNEL:
			break;
		case CREATE_CHANNEL_STAT:
			break;
		}
	}

	public void addCommand( Command cmd ){
		synchronized(thread){
			if( cmd.commandId == CommandId.TIME_BROADCAST ){
				timeOffset = cmd.time - System.nanoTime();
				thread.notifyAll();
			}
			else {
				commands.add( cmd );
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
