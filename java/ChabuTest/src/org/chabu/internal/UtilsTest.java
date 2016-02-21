package org.chabu.internal;

import static org.junit.Assert.*;

import org.junit.Test;

public class UtilsTest {

	@Test
	public void testAlignUpTo4() {
		assertEquals(  4, Utils.alignUpTo4( 4 ));
		assertEquals(  8, Utils.alignUpTo4( 5 ));
		assertEquals(  8, Utils.alignUpTo4( 6 ));
		assertEquals(  8, Utils.alignUpTo4( 7 ));
		assertEquals(  8, Utils.alignUpTo4( 8 ));
		assertEquals( 12, Utils.alignUpTo4( 9 ));
	}

	@Test
	public void testIsAligned4() {
		assertEquals( true , Utils.isAligned4( 4 ));
		assertEquals( false, Utils.isAligned4( 5 ));
		assertEquals( false, Utils.isAligned4( 6 ));
		assertEquals( false, Utils.isAligned4( 7 ));
		assertEquals( true , Utils.isAligned4( 8 ));
	}

}
