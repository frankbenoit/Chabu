package mctcp.user;

import java.io.IOException;

import mctcp.IChannel;
import mctcp.Utils;

import org.junit.Test;

public class TcpSingle {

	boolean canStop;
	class TramsitState {
		long  idx;
	}

	@Test
	public void test() throws IOException {
		final long AMOUNT = 1000_000_003L;
		final TestData td = new TestData();
		final TestDataFlow tdf = td.createFlow("t");
		canStop = false;
		{
			final TramsitState ts = new TramsitState();
			final TcpServer s = TcpServer.startServer(2000);
			s.getChannel().setHandler((IChannel ch) -> {
				// sending
				if( ch.txGetBuffer().position() == 0 && ts.idx >= AMOUNT ){
					System.out.printf("Send completed\n" );
					ch.close();
					return;
				}
				if( ts.idx < AMOUNT ){
					long diff = AMOUNT - ts.idx;
					if( ch.txGetBuffer().remaining() > diff ){
						ch.txGetBuffer().limit( ch.txGetBuffer().position() + (int)diff );
					}
					long sz = tdf.copySendData(ch.txGetBuffer());
					ts.idx += sz;
//					System.out.printf("send %s\n", sz );
					ch.registerWaitForWrite();
				}
			});
			s.start();
		}
		{
			final TramsitState ts = new TramsitState();
			final TcpServer c = TcpServer.startClient("localhost", 2000);
			c.getChannel().setHandler( (IChannel ch) -> {
				long sz = tdf.checkRecvData(ch.rxGetBuffer());
				ts.idx += sz;
//				System.out.printf("Recv %s\n", sz );
				if( ts.idx >= AMOUNT ){
					System.out.println("Recv completed");
					ch.close();
					canStop = true;
					c.shutDown();
					Utils.notifyAllOn(this);
					return;
				}
			});
			c.start();
		}
		while( !canStop ){
			Utils.waitOn(this);
		}
		tdf.printStats();
		System.out.println("Main finished");
	}

}
