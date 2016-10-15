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
using System.Threading;

namespace Org.Chabu.Prot.V1.Internal
{
    using PrintWriter = global::System.IO.TextWriter;
    using global::System.IO;
    using ByteBuffer = Org.Chabu.Prot.Util.ByteBuffer;
    [TestClass]
    public class UtilsTest {

	    [TestMethod]
	    public void testAlignUpTo4() {
		    Assert.AreEqual(  4, Utils.alignUpTo4( 4 ));
		    Assert.AreEqual(  8, Utils.alignUpTo4( 5 ));
		    Assert.AreEqual(  8, Utils.alignUpTo4( 6 ));
		    Assert.AreEqual(  8, Utils.alignUpTo4( 7 ));
		    Assert.AreEqual(  8, Utils.alignUpTo4( 8 ));
		    Assert.AreEqual( 12, Utils.alignUpTo4( 9 ));
	    }

	    [TestMethod]
	    public void testIsAligned4() {
		    Assert.AreEqual( true , Utils.isAligned4( 4 ));
		    Assert.AreEqual( false, Utils.isAligned4( 5 ));
		    Assert.AreEqual( false, Utils.isAligned4( 6 ));
		    Assert.AreEqual( false, Utils.isAligned4( 7 ));
		    Assert.AreEqual( true , Utils.isAligned4( 8 ));
	    }
	    [TestMethod]
	    public void printTraceHexData() {
		    StringWriter sw = new StringWriter();
		    ByteBuffer bb = new ByteBuffer(100);
            bb.limit(100);
		    Utils.printTraceHexData(sw, bb, 0, 100);
		    Assert.AreEqual(sw.ToString(),
				    "    00 00 00 00  00 00 00 00  00 00 00 00  00 00 00 00\r\n" + 
				    "    00 00 00 00  00 00 00 00  00 00 00 00  00 00 00 00\r\n" + 
				    "    00 00 00 00  00 00 00 00  00 00 00 00  00 00 00 00\r\n" + 
				    "    00 00 00 00  00 00 00 00  00 00 00 00  00 00 00 00\r\n" + 
				    "    00 00 00 00  00 00 00 00  00 00 00 00  00 00 00 00\r\n" + 
				    "    00 00 00 00  00 00 00 00  00 00 00 00  00 00 00 00\r\n" + 
				    "    00 00 00 00\r\n" + 
				    "    <<\r\n" + 
				    "\r\n");
	    }


        [TestMethod]
        [ExpectedException(typeof(ChabuException),
            "A userId of null was inappropriately allowed.")]
        public void fail()
        { 

            Utils.fail( 32, "");
	    }

        [TestMethod]
        public void ensure()
        {

            Utils.ensure(true, ChabuErrorCode.APPLICATION_VALIDATOR, "");
            Utils.ensure(true, 32, "");

        }

        [TestMethod]
        [ExpectedException(typeof(ChabuException),
            "A userId of null was inappropriately allowed.")]
        public void ensure_fails()
        {

            Utils.ensure(false, 32, "");
        }

        [TestMethod]
	    public void waitOn() {

            UtilsTest that = this;
            Thread t = new Thread(() => {
			    try {
				    Thread.Sleep(100);
			    } catch (ThreadInterruptedException e) {
				    throw new SystemException("interrupted", e);
			    }
			    Utils.notifyAllOn(that);
		    });
		
		    t.Start();
		
		    Utils.waitOn(this);
	    }
	
	    [TestMethod]
        [ExpectedException(typeof(ThreadInterruptedException),
                    "A userId of null was inappropriately allowed.")]
        public void waitOnInterrupts() {
		    Thread current = Thread.CurrentThread;
            UtilsTest that = this;
		    Thread t = new Thread(() => {
			    try {
				    Thread.Sleep(40);
			    } catch (ThreadInterruptedException e) {
				    throw new SystemException("interrupted", e);
			    }
			    lock(that){
				    current.Interrupt();
			    }
		    });

		    t.Start();
		
		    Utils.waitOn(this);
	    }
	
	    [TestMethod]
	    public void waitOnTimed() {
		    Utils.waitOn(this, 10);
	    }
	
	    [TestMethod]
        [ExpectedException(typeof(ThreadInterruptedException),
                    "A userId of null was inappropriately allowed.")]
        public void waitOnTimedInterrupts() {
            UtilsTest that = this;
		    Thread current = Thread.CurrentThread;
		    Thread t = new Thread(() => {
			    try {
				    Thread.Sleep(40);
			    } catch (ThreadInterruptedException e) {
				    throw new SystemException("", e);
			    }
			    lock(that){
				    current.Interrupt();
			    }
		    });
		
		    t.Start();
		
		    Utils.waitOn(this, 200);
	    }
	
	    [TestMethod]
	    public void notifyAllOn() {
		
	    }
    }
}
