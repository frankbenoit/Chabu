package org.chabu.nwtest.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

import org.chabu.nwtest.Const;
import org.chabu.prot.v1.Chabu;
import org.chabu.prot.v1.ChabuNetworkHandler;
import org.json.JSONException;
import org.json.JSONObject;

final class NetworkThread implements Runnable {

	private ByteBuffer ctrlXmitBuffer = ByteBuffer.allocate(1000);
	private ByteBuffer ctrlRecvBuffer = ByteBuffer.allocate(1000);


	private boolean connectionCompleted = false;
//	private boolean connectionCompletedCtrl = false;
//	private boolean connectionCompletedTest = false;
	private InetSocketAddress remoteAddrCtrl;
	private InetSocketAddress remoteAddrTest;

	Selector selector;
	private Thread thread;
//	private SelectionKey keyTest;
	Chabu chabu;
	boolean goToShutdown = false;
//	private SelectionKey keyCtrl;
	private SocketContext ctxCtrl;
	private SocketContext ctxTest;

	static class SocketContext {
		String        name;
		ByteBuffer    xmitBuf;
		ByteBuffer    recvBuf;
		SocketChannel channel;
		SelectionKey  key;
		Selector      selector;
		boolean       isTest;
		ChabuNetworkHandler chabu;
		boolean             connectionCompleted = false;
		boolean             xmitReq = false;
		
		public String toString() {
			return String.format("%s xmit:%s recv:%s", name, xmitBuf, recvBuf );
		}
	}
	
	public NetworkThread(int port) {
		remoteAddrCtrl = new InetSocketAddress(port);
		remoteAddrTest = new InetSocketAddress(port+1);

		ctrlRecvBuffer.clear();
		ctrlRecvBuffer.limit(0);
		ctrlXmitBuffer.clear();

	}

	public void run() {
		try {
			thread = Thread.currentThread();
			SocketChannel socketCtrl = SocketChannel.open();
			SocketChannel socketTest = SocketChannel.open();

			socketCtrl.configureBlocking(false);
			socketTest.configureBlocking(false);

			selector = Selector.open();

			socketCtrl.connect(remoteAddrCtrl);
			socketTest.connect(remoteAddrTest);

			ctxCtrl = new SocketContext();
			ctxCtrl.name = "Ctrl";
			ctxCtrl.xmitBuf = ctrlXmitBuffer;
			ctxCtrl.recvBuf = ctrlRecvBuffer;
			ctxCtrl.selector = selector;
			ctxCtrl.channel  = socketCtrl;
			ctxCtrl.isTest   = false;
			
			
			ctxTest = new SocketContext();
			ctxTest.name = "Test";
			ctxTest.selector = selector;
			ctxTest.channel  = socketTest;
			ctxTest.isTest   = true;
			
			ctxCtrl.key = socketCtrl.register( selector, SelectionKey.OP_CONNECT, ctxCtrl );
			ctxTest.key = socketTest.register( selector, SelectionKey.OP_CONNECT, ctxTest );
			
			while( selector.isOpen() && !Thread.interrupted() ){

				selector.select(2000);
				System.out.println("nw client select");

				boolean notify = false;
				synchronized (this) {

					Set<SelectionKey> selectedKeys = selector.selectedKeys();

					Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

					while(keyIterator.hasNext()) {

						//get the key itself
						SelectionKey key = keyIterator.next();
						keyIterator.remove();
						
						if(!key.isValid()) {
							continue;
						}

						SocketContext ctx = (SocketContext) key.attachment();
						if( key.isConnectable() ){
							SocketChannel sc = (SocketChannel) key.channel();
							sc.finishConnect();
							key.interestOps( SelectionKey.OP_READ|SelectionKey.OP_WRITE);
							notify = true;		
							System.out.printf("connected %s%n", ctx.isTest );
							ctx.connectionCompleted = true;
						}

						if( ctx != null ){
							if( ctx == ctxTest && ctx.isTest ){
								if( ctx.chabu != null && ( ctx.key.isWritable() || ctx.key.isReadable() )){
									ctx.chabu.handleChannel( ctx.channel );
								}
								else {
									key.interestOps( SelectionKey.OP_READ);
								}
							}
							else {
								
								
								if( ctx.key.isWritable() && ctx.channel.isConnected() ){
									
//								System.out.printf("write %s %s\n", ctx.name, ctx.xmitBuf.position() );
									
									if( ctx.xmitBuf.position() > 0 ){
										ctx.xmitBuf.flip();
										int sz = ctx.channel.write(ctx.xmitBuf);
										if(Const.LOG_TIMING) NwtUtil.log("%s write %5d", ctx.name, sz );
										if( sz > 0 ){
											notify = true;								
										}
										ctx.xmitBuf.compact();
									}
									if( ctx.xmitBuf.position() == 0 ){
										if( !ctx.xmitReq ){
											// nothing more to xmit, remove write interest
											if(Const.LOG_TIMING) NwtUtil.log("%s ~wr", ctx.name );
											ctx.key.interestOps( ctx.key.interestOps() & ~SelectionKey.OP_WRITE );
										}
										ctx.xmitReq = false;
									}
									
								}
								if( ctx.key.isReadable() ){
									ctx.recvBuf.compact();
									int sz = ctx.channel.read(ctx.recvBuf);
									ctx.recvBuf.flip();
									if(Const.LOG_TIMING) NwtUtil.log("%s read  %5d", ctx.name, sz );
									if( sz > 0 ){
										notify = true;								
									}
								}
							}
							
						}
						if( !connectionCompleted && ctxCtrl.connectionCompleted && ctxTest.connectionCompleted ){
							connectionCompleted = true;
							System.out.println("compl");
						}


					}


					if( notify ){
						if( goToShutdown ){
							selector.close();
						}
						this.notifyAll();
					}
				}

			}

		} catch (IOException e) {
			System.out.printf("goToShutdown=%s selector.isOpen=%s\n", goToShutdown, selector.isOpen() );
			e.printStackTrace();
			System.exit(1);
		}
		System.out.println("NetworkThread.run() finished");
	}

	public void interrupt() {
		thread.interrupt();			
	}


	public boolean isConnectionCompleted() {
		return connectionCompleted;
	}
	JSONObject ctrlXfer( JSONObject req ){
		return ctrlXfer( req, false );
	}
	JSONObject ctrlXfer( JSONObject req, boolean close ){
		synchronized(this){
			if( close ){
				goToShutdown = true;
			}
			ctrlXmitBuffer.put( StandardCharsets.UTF_8.encode(req.toString()) );
			ctxCtrl.key.interestOps( ctxCtrl.key.interestOps() | SelectionKey.OP_WRITE );
			selector.wakeup();
			while( ctrlXmitBuffer.position() > 0 ){
//				System.out.printf("ctrlXfer wait %s \n", ctrlXmitBuffer.position());
				doWait();
			}
		}
		//System.out.printf("ctrlXfer xmitted\n");
		if( close ){
			return null;
		}
		JSONObject res = null;
		synchronized(this){
			while( (res = tryGetResponse()) == null ){
				//System.out.printf("ctrlXfer try get response wait\n");
				doWait();
			}
		}
		//System.out.printf("ctrlXfer got response %s\n", res);
		return res;
	}

	private JSONObject tryGetResponse() {
		JSONObject res = null;
		int pos = ctrlRecvBuffer.position();
		try{
			String respStr = StandardCharsets.UTF_8.decode(ctrlRecvBuffer).toString();
			//System.out.printf("tryGetResponse test: %s\n", respStr );
			if( !respStr.isEmpty() ){
				res = new JSONObject( respStr );
			}
		}
		catch( JSONException e ){
			ctrlRecvBuffer.position(pos);
		}
		return res;
	}
	public void doWait(){
		try{
			this.wait();
			System.out.println("nw client wait");
		}
		catch( InterruptedException e ){
			throw new RuntimeException(e);
		}
	}

	public void setTestWriteRequest(){
		synchronized(this){
			if( Const.LOG_TIMING ) NwtUtil.log("Test +wr" );
			int interestOps = ctxTest.key.interestOps();
			if(( interestOps & SelectionKey.OP_WRITE ) == 0 ){
				ctxTest.key.interestOps( interestOps | SelectionKey.OP_WRITE );
			}
			ctxTest.xmitReq = true;
			selector.wakeup();
		}
	}
	public void setChabu(Chabu chabu) {
		ctxTest.chabu = chabu;
		this.chabu = chabu;
		setTestWriteRequest();
	}
}