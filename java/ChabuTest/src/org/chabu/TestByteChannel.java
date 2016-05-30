package org.chabu;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;

import org.chabu.prot.v1.internal.ByteBufferUtils;

public class TestByteChannel implements ByteChannel {
	private ByteBuffer readData;
	private ByteBuffer writeData;
	
	public TestByteChannel() {
	}
	public TestByteChannel(int recvCapacity, int xmitCapacity) {
		readData = ByteBuffer.allocate(recvCapacity);
		writeData = ByteBuffer.allocate(xmitCapacity);
		writeData.limit(0);
	}
	
//	public void setReadData(ByteBuffer readData) {
//		this.readData = readData;
//	}
	public void putRecvData(ByteBuffer readData) {
		this.readData.put(readData);
	}
	
	public void putRecvData( String hexString ){
		byte[] data = TestUtils.hexStringToByteArray(hexString);
		readData.put(data);
	}

	public void resetXmitRecording(int length) {
		this.writeData.clear();
		writeData.limit(length);
	}
	public ByteBuffer getWriteData() {
		return writeData;
	}
	
	@Override
	public boolean isOpen() {
		return true;
	}
	@Override
	public void close() throws IOException {
	}
	@Override
	public int read(ByteBuffer dst) throws IOException {
		readData.flip();
		int res = ByteBufferUtils.transferRemaining(readData, dst);
		readData.compact();
		return res;
	}
	@Override
	public int write(ByteBuffer src) throws IOException {
		int count = 0;
		while(writeData.hasRemaining() && src.hasRemaining() ){
			writeData.put(src.get());
			count++;
		}
		return count;
	}
}