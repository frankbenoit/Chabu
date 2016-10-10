/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit, Stuttgart, Germany <fr@nk-benoit.de>
 * 
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 * 
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using System.Collections.Generic;

namespace Org.Chabu.Prot.V1.Internal
{

    [TestClass]
    public class PriorizerTest {

	    private List<int> requests = new List<int>();
	    Priorizer p;

        [TestInitialize]
        public void setup(){
		    requests.Clear();
	    }
	
	    [TestMethod]
	    public void testNewHasNoRequest() {
		    create( 1, 1 );
		    pop(2);
		    assertRq( "xx" );
	    }
	
	    [TestMethod]
	    public void testSimpleRequest() {
		    create( 1, 1 );
		    rq( 0, "0" );
		    pop(2);
		    assertRq( "0x" );
	    }

	    [TestMethod]
	    public void testFirstRq() {
		    create( 3, 3);
		    rq( 0, "012" );
		    rq( 1, "012" );
		    rq( 2, "012" );
		    pop(10);
		    assertRq( "012012012x" );
	    }
	
	    [TestMethod]
	    public void testContinueAfterLastChannel() {
		    create( 3, 3);
		    rq( 0, "012" );
		    pop(1);
		    rq( 0, "0" );
		    pop(4);
		    assertRq( "0120x" );
	    }

	    [TestMethod]
        [ExpectedException(typeof(ArgumentException),
            "A userId of null was inappropriately allowed.")]
        public void requestTestsArgPriority() {
		    create( 3, 3);
		    p.request(100, 0);
	    }

	    private void create( int prio, int channels ) {
		    p = new Priorizer( prio, channels );
	    }
	
	    private void assertRq(String req) {
		    char[] chars = req.ToCharArray();
		    for( int i = 0; i < chars.Length; i++ ){
			    int ch = chars[i] - '0';
			    if( chars[i] == 'x' ) ch = -1;
			    if( requests.Count <= i ){
				    throw new SystemException( "Not enough collected requests" );
			    }
			    Assert.AreEqual( ch, requests[i] );
		    }
		    Assert.AreEqual( chars.Length, requests.Count );
	    }

	    private void rq( int prio, String req ){
		    foreach( char c in req.ToCharArray() ){
			    int ch = c - '0';
			    p.request(prio, ch );
		    }
	    }
	    private void pop( int amount ){
		    for( int i = 0; i < amount; i++ ){
			    requests.Add( p.popNextRequest() );
		    }
	    }
	
	
    }
}