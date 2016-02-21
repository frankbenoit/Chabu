package org.chabu.internal;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;

public class ByteBufferUtilsTest {

	private static final int TRG_PATTERN = 0xA5;
	private static final int SRC_PATTERN = 0xF3;

	
	@Test
	public void testTransferUntilTargetPos() {
		ByteBuffer src = ByteBuffer.allocate(100);
		ByteBuffer trg = ByteBuffer.allocate(100);
		
		bbSetup( src, SRC_PATTERN, 0, 100 );
		bbSetup( trg, TRG_PATTERN, 0, 100 );
		
		ByteBufferUtils.transferUntilTargetPos(src, trg, 8 );
		
		bbTestPosLim( trg, 8, 100 );
		bbTestContent( trg, 0, 8 );
	}

	@Test
	public void testTransferUntilTargetPos_TargetTooSmall() {
		ByteBuffer src = ByteBuffer.allocate(100);
		ByteBuffer trg = ByteBuffer.allocate(100);
		
		bbSetup( src, SRC_PATTERN, 0, 100 );
		bbSetup( trg, TRG_PATTERN, 10, 12 );
		
		ByteBufferUtils.transferUntilTargetPos(src, trg, trg.position()+8 );
		
		bbTestPosLim( trg, 12, 12 );
		bbTestContent( trg, 10, 12 );
	}
	
	@Test
	public void testTransferUntilTargetPos_SourceTooSmall() {
		ByteBuffer src = ByteBuffer.allocate(100);
		ByteBuffer trg = ByteBuffer.allocate(100);
		
		bbSetup( src, SRC_PATTERN, 0, 5 );
		bbSetup( trg, TRG_PATTERN, 10, 20 );
		
		ByteBufferUtils.transferUntilTargetPos(src, trg, trg.position()+8 );
		
		bbTestPosLim( trg, 15, 20 );
		bbTestContent( trg, 10, 15 );
	}
	
	@Test
	public void testTransferRemaining_SourceTooSmall() {
		ByteBuffer src = ByteBuffer.allocate(100);
		ByteBuffer trg = ByteBuffer.allocate(100);
		
		bbSetup( src, SRC_PATTERN, 0, 5 );
		bbSetup( trg, TRG_PATTERN, 0, 20 );
		
		ByteBufferUtils.transferRemaining(src, trg );
		
		bbTestPosLim( trg, 5, 20 );
		bbTestContent( trg, 0, 5 );
	}
	
	@Test
	public void testTransferRemaining_TargetTooSmall() {
		ByteBuffer src = ByteBuffer.allocate(100);
		ByteBuffer trg = ByteBuffer.allocate(100);
		
		bbSetup( src, SRC_PATTERN, 0, 20 );
		bbSetup( trg, TRG_PATTERN, 5, 10 );
		
		ByteBufferUtils.transferRemaining(src, trg );
		
		bbTestPosLim( trg, 10, 10 );
		bbTestContent( trg, 5, 10 );
	}
	
	private void bbTestPosLim(ByteBuffer trg, int pos, int limit ) {
		assertEquals( pos, trg.position());
		assertEquals( limit, trg.limit());
	}

	private void bbTestContent(ByteBuffer bb, int startPos, int endPos) {
		int i = 0;
		for( ; i < startPos; i++ ){
			byte b = bb.get( i );
			assertEquals(String.format("Index %d", i), TRG_PATTERN, b & 0xFF );
		}
		for( ; i < endPos && i < bb.limit(); i++ ){
			byte b = bb.get( i );
			assertEquals(String.format("Index %d", i), SRC_PATTERN, b & 0xFF );
		}
		for( ; i < bb.limit(); i++ ){
			byte b = bb.get( i );
			assertEquals(String.format("Index %d", i), TRG_PATTERN, b & 0xFF );
		}
	}

	private void bbSetup(ByteBuffer src, int fillPattern, int pos, int limit) {
		for( int i = 0; i < src.capacity(); i++ ){
			src.put( i, (byte)fillPattern );
		}
		src.position(pos);
		src.limit(limit);
	}

}
