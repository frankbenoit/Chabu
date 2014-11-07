package chabu.tester;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import chabu.tester.data.ACmdScheduled;
import chabu.tester.data.CmdChannelAction;
import chabu.tester.data.CmdChannelCreateStat;
import chabu.tester.data.CmdConnectionAwait;
import chabu.tester.data.CmdConnectionClose;
import chabu.tester.data.CmdConnectionConnect;
import chabu.tester.data.CmdDutApplicationClose;
import chabu.tester.data.CmdDutConnect;
import chabu.tester.data.CmdDutDisconnect;
import chabu.tester.data.CmdSetupActivate;
import chabu.tester.data.CmdSetupChannelAdd;

public class SimpleTest implements ITestTask {

	private static final int PORT_CHABU = 9197;

	static class DutCmd {
		DutId    dut;
		ACmdScheduled cmd;
	}
	ArrayList<DutCmd> cmdList = new ArrayList<>(100);
	
	private void addToList(DutId dut, ACmdScheduled cmd) {
		DutCmd dc = new DutCmd();
		dc.dut = dut;
		dc.cmd = cmd;
		cmdList.add( dc );
	}
	private void enqueueList(ChabuTestNw nw) throws IOException {
		
		Collections.sort( cmdList, ( DutCmd d1, DutCmd d2 ) -> {
			return Long.compare(d1.cmd.schedTime, d2.cmd.schedTime);
		});
		
		for( DutCmd dc : cmdList ){
			nw.addCommand( dc.dut, dc.cmd );
		}
		cmdList.clear();
	}
	@Override
	public synchronized void task(ChabuTestNw nw) throws Exception {
		long st = System.nanoTime();
		nw.addCommand( DutId.A, new CmdDutConnect( st, "localhost", Constants.PORT_DUT0 ));
		nw.addCommand( DutId.B, new CmdDutConnect( st, "localhost", Constants.PORT_DUT1 ));
		nw.flush(DutId.ALL);
		nw.addCommand( DutId.A, new CmdSetupChannelAdd( st, 0, 2000, 2000, 237, 645 ));
		nw.addCommand( DutId.B, new CmdSetupChannelAdd( st, 0, 2000, 2000, 645, 237 ));
		nw.addCommand( DutId.A, new CmdSetupChannelAdd( st, 1, 2000, 2000, 728, 967 ));
		nw.addCommand( DutId.B, new CmdSetupChannelAdd( st, 1, 2000, 2000, 967, 728 ));
		nw.addCommand( DutId.ALL, new CmdSetupActivate( st, true, 1000 ));
		st += 400*MSEC;
		
		nw.addCommand( DutId.A, new CmdConnectionAwait( st, PORT_CHABU ));
		nw.addCommand( DutId.B, new CmdConnectionConnect( st, "localhost", PORT_CHABU ));
		nw.flush(DutId.ALL);
		waitUntil( System.nanoTime()+1000*MSEC );
		st = System.nanoTime();
		int millis1 = 0;
		addToList( DutId.A, new CmdChannelAction( st+millis1*MSEC, 0, 0, 1000_000 ));
		addToList( DutId.A, new CmdChannelAction( st+millis1*MSEC, 1, 0, 1000_000 ));
		addToList( DutId.B, new CmdChannelAction( st+millis1*MSEC, 0, 0, 1000_000 ));
		addToList( DutId.B, new CmdChannelAction( st+millis1*MSEC, 1, 0, 1000_000 ));

		for( int i = 0; i < 6; i++ ){
			addToList( DutId.A, new CmdChannelAction( st+millis1*MSEC, 0, 100000, 0 ));
			addToList( DutId.A, new CmdChannelAction( st+millis1*MSEC, 1, 100000, 0 ));
			addToList( DutId.B, new CmdChannelAction( st+millis1*MSEC, 0, 100000, 0 ));
			addToList( DutId.B, new CmdChannelAction( st+millis1*MSEC, 1, 100000, 0 ));

			millis1 += 200;
			addToList( DutId.A, new CmdChannelAction( st+millis1*MSEC, 0, 100000, 0 ));
			addToList( DutId.B, new CmdChannelAction( st+millis1*MSEC, 0, 100000, 0 ));
			millis1 += 400;
		}
		int millis2 = 0;
		while( millis2 <= millis1+500 ){
			addToList( DutId.ALL, new CmdChannelCreateStat( st+millis2*MSEC, 0 ));
			addToList( DutId.ALL, new CmdChannelCreateStat( st+millis2*MSEC, 1 ));
			millis2 += 50;
		}
		enqueueList(nw);
		nw.flush(DutId.ALL);
		
//		waitUntil( st+2000*MSEC );
		waitUntil( st+millis2*MSEC );
		
		nw.addCommand( DutId.ALL, new CmdConnectionClose( System.nanoTime() ) );
		nw.addCommand( DutId.ALL, new CmdDutApplicationClose( System.nanoTime() ) );
		nw.flush(DutId.ALL);
		System.out.println("disconnecting");
		nw.addCommand( DutId.A, new CmdDutDisconnect( System.nanoTime() ) );
		nw.addCommand( DutId.B, new CmdDutDisconnect( System.nanoTime() ) );
		System.out.println("--- Actions finished ---");

	}
	private void waitUntil(long nanoTs) throws InterruptedException {
		long diff = (nanoTs - System.nanoTime()) / MSEC;
		if( diff > 0 ){
			wait( diff );
		}
	}

	@Override
	public int getChannelCount() {
		return 2;
	}
}
