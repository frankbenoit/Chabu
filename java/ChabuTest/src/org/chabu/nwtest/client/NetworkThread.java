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

import org.chabu.IChabu;
import org.json.JSONException;
import org.json.JSONObject;

final class NetworkThread implements Runnable {

	private ByteBuffer ctrlXmitBuffer = ByteBuffer.allocate(1000);
	private ByteBuffer ctrlRecvBuffer = ByteBuffer.allocate(1000);
	private ByteBuffer testXmitBuffer = ByteBuffer.allocate(1000);
	private ByteBuffer testRecvBuffer = ByteBuffer.allocate(1000);


	private boolean connectionCompleted = false;
	private boolean connectionCompletedCtrl = false;
	private boolean connectionCompletedTest = false;
	private InetSocketAddress remoteAddr;

	Selector selector;
	private Thread thread;
	private SelectionKey keyTest;
	IChabu chabu;
	boolean goToShutdown = false;

	public NetworkThread(int port) {
		remoteAddr = new InetSocketAddress(port);

		ctrlRecvBuffer.clear();
		ctrlRecvBuffer.limit(0);

		ctrlXmitBuffer.clear();
		ctrlXmitBuffer.put((byte)0);

		testRecvBuffer.clear();
		testRecvBuffer.limit(0);

		testXmitBuffer.clear();
		testXmitBuffer.put((byte)1);

	}

	public void run() {
		try {
			thread = Thread.currentThread();
			SocketChannel socketCtrl = SocketChannel.open();
			SocketChannel socketTest = SocketChannel.open();

			socketCtrl.configureBlocking(false);
			socketTest.configureBlocking(false);

			selector = Selector.open();

			socketCtrl.connect(remoteAddr);
			socketTest.connect(remoteAddr);

			SelectionKey keyCtrl = socketCtrl.register( selector, SelectionKey.OP_WRITE|SelectionKey.OP_CONNECT, "Ctrl" );
			keyTest = socketTest.register( selector, SelectionKey.OP_WRITE|SelectionKey.OP_CONNECT, "Test" );
			while( selector.isOpen() && !Thread.interrupted() ){

				selector.select(500);
				
				boolean notify = false;
				synchronized (this) {

					if( socketCtrl.isConnected() ){
						if( ctrlXmitBuffer.position() > 0 ){
							ctrlXmitBuffer.flip();
							int sz = socketCtrl.write(ctrlXmitBuffer);
							if( sz > 0 ){
//								System.out.printf("write ctrl %s\n", sz );
								notify = true;								
							}
							if( !connectionCompletedCtrl && !ctrlXmitBuffer.hasRemaining() ){
								connectionCompletedCtrl = true;
							}
							ctrlXmitBuffer.compact();
						}
						if( ctrlXmitBuffer.position() > 0 ){
							keyCtrl.interestOps( keyCtrl.interestOps() | SelectionKey.OP_WRITE );
						}
						else {
							keyCtrl.interestOps( keyCtrl.interestOps() & ~SelectionKey.OP_WRITE );
						}

					}

					if( socketTest.isConnected() ){

						if( chabu != null ){
							chabu.evXmit( testXmitBuffer );
						}

						if( testXmitBuffer.position() > 0 ){
							testXmitBuffer.flip();
							int sz = socketTest.write(testXmitBuffer);
							if( sz > 0 ){
//								System.out.printf("write test %s\n", sz );
								notify = true;								
							}
							if( !connectionCompletedTest && !testXmitBuffer.hasRemaining() ){
								connectionCompletedTest = true;
							}
							testXmitBuffer.compact();
						}
						if( testXmitBuffer.position() > 0 ){
							keyTest.interestOps( keyTest.interestOps() | SelectionKey.OP_WRITE );
						}
						else {
							keyTest.interestOps( keyTest.interestOps() & ~SelectionKey.OP_WRITE );
						}
					}

					if( !connectionCompleted && connectionCompletedCtrl && connectionCompletedTest ){
						connectionCompleted = true;
						System.out.println("compl");
					}

					selector.selectNow();
					Set<SelectionKey> selectedKeys = selector.selectedKeys();

					Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

					//						System.out.println("selector run "+ selectedKeys.size());
					//						
					//						for( SelectionKey k : selector.keys()){
					//			                System.out.printf("keytest >>> %s%s%s%s%s %s%s%s%s %s\n",
					//			                		k.isValid()       ? "V" : " ",
					//			                		k.isAcceptable()  ? "A" : " ",
					//			                		k.isConnectable() ? "C" : " ",
					//			                		k.isWritable()    ? "W" : " ",
					//			                		k.isReadable()    ? "R" : " ",
					//			    					(k.interestOps() & SelectionKey.OP_ACCEPT  ) != 0 ? "a" : " ",
					//			    					(k.interestOps() & SelectionKey.OP_CONNECT ) != 0 ? "c" : " ",
					//			    					(k.interestOps() & SelectionKey.OP_WRITE   ) != 0 ? "w" : " ",
					//			    					(k.interestOps() & SelectionKey.OP_READ    ) != 0 ? "r" : " ",
					//			                		k.attachment()
					//			                				);
					//						}


					while(keyIterator.hasNext()) {

						//get the key itself
						SelectionKey key = keyIterator.next();
						keyIterator.remove();
						if(!key.isValid()) {
							continue;
						}

//						System.out.printf("Client %s%s%s%s%s %s%s%s%s %s\n",
//								key.isValid()       ? "V" : " ",
//								key.isAcceptable()  ? "A" : " ",
//								key.isConnectable() ? "C" : " ",
//								key.isWritable()    ? "W" : " ",
//								key.isReadable()    ? "R" : " ",
//								(key.interestOps() & SelectionKey.OP_ACCEPT  ) != 0 ? "a" : " ",
//								(key.interestOps() & SelectionKey.OP_CONNECT ) != 0 ? "c" : " ",
//								(key.interestOps() & SelectionKey.OP_WRITE   ) != 0 ? "w" : " ",
//								(key.interestOps() & SelectionKey.OP_READ    ) != 0 ? "r" : " ",
//								key.attachment()
//								);

						if( key.isConnectable() ){
							SocketChannel sc = (SocketChannel) key.channel();
							sc.finishConnect();
							key.interestOps( SelectionKey.OP_READ|SelectionKey.OP_WRITE);
							notify = true;								
						}

						if( key.isReadable() && selector.isOpen() ){
							if( key.channel() == socketCtrl ){
								ctrlRecvBuffer.compact();
								int sz = socketCtrl.read(ctrlRecvBuffer);
								ctrlRecvBuffer.flip();
//								System.out.printf("ctrl read %d\n", sz );
								if( sz > 0 ){
									notify = true;								
								}
							}
							if( key.channel() == socketTest ){
								testRecvBuffer.compact();
								int sz = socketTest.read(testRecvBuffer);
								testRecvBuffer.flip();
//								System.out.printf("test read %d\n", sz );
								if( sz > 0 ){
									notify = true;								
								}
								if( chabu != null && testRecvBuffer.hasRemaining() ){
									chabu.evRecv( testRecvBuffer );
								}
							}
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
	}

	public void interrupt() {
		thread.interrupt();			
	}

	public void selectorWakeup() {
		selector.wakeup();
	}

	public boolean isConnectionCompleted() {
		return connectionCompleted;
	}
	JSONObject ctrlXfer( JSONObject req ){
		return ctrlXfer( req, false );
	}
	JSONObject ctrlXfer( JSONObject req, boolean close ){
		synchronized(this){
			ctrlXmitBuffer.put( StandardCharsets.UTF_8.encode(req.toString()) );
			if( close ){
				goToShutdown = true;
			}
			selectorWakeup();
			while( ctrlXmitBuffer.position() > 0 ){
				//System.out.printf("ctrlXfer wait %s \n", ctrlXmitBuffer.position());
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
		}
		catch( InterruptedException e ){
			throw new RuntimeException(e);
		}
	}

	public void setTestWriteRequest(){
		synchronized(this){
			keyTest.interestOps(SelectionKey.OP_READ|SelectionKey.OP_WRITE);
		}
	}
	public void setChabu(IChabu chabu) {
		this.chabu = chabu;
	}
}