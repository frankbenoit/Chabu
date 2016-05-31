package org.chabu.prot.v1.internal;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.chabu.prot.v1.internal.Priorizer;
import org.junit.Before;
import org.junit.Test;
import org.junit.gen5.api.Assertions;

public class PriorizerTest {

	private ArrayList<Integer> requests = new ArrayList<>();
	Priorizer p;

	@Before
	public void setup(){
		requests.clear();
	}
	
	@Test
	public void testNewHasNoRequest() {
		create( 1, 1 );
		pop(2);
		assertRq( "xx" );
	}
	
	@Test
	public void testSimpleRequest() {
		create( 1, 1 );
		rq( 0, "0" );
		pop(2);
		assertRq( "0x" );
	}

	@Test
	public void testFirstRq() {
		create( 3, 3);
		rq( 0, "012" );
		rq( 1, "012" );
		rq( 2, "012" );
		pop(10);
		assertRq( "012012012x" );
	}
	
	@Test
	public void testContinueAfterLastChannel() {
		create( 3, 3);
		rq( 0, "012" );
		pop(1);
		rq( 0, "0" );
		pop(4);
		assertRq( "0120x" );
	}

	@Test
	public void requestTestsArgPriority() throws Exception {
		create( 3, 3);
		Assertions.assertThrows(RuntimeException.class, ()->{
			p.reqest(100, 0);
		});
	}

	private void create( int prio, int channels ) {
		p = new Priorizer( prio, channels );
	}
	
	private void assertRq(String req) {
		char[] chars = req.toCharArray();
		for( int i = 0; i < chars.length; i++ ){
			int ch = chars[i] - '0';
			if( chars[i] == 'x' ) ch = -1;
			if( requests.size() <= i ){
				fail( "Not enough collected requests" );
			}
			assertEquals( ch, (int)requests.get(i) );
		}
		assertEquals( chars.length, requests.size() );
		
	}

	private void rq( int prio, String req ){
		for( char c : req.toCharArray() ){
			int ch = c - '0';
			p.reqest(prio, ch );
		}
	}
	private void pop( int amount ){
		for( int i = 0; i < amount; i++ ){
			requests.add( p.popNextRequest() );
		}
	}
	
	
}
