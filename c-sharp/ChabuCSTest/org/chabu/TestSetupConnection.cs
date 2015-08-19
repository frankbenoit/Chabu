using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace org.chabu
{


    [TestClass]
    public class TestSetupConnection 
    {
        delegate void Runnable();

	    private const String APPLNAME_200 = "" 
			    + "12345678901234567890123456789012345678901234567890"
			    + "12345678901234567890123456789012345678901234567890"
			    + "12345678901234567890123456789012345678901234567890"
			    + "12345678901234567890123456789012345678901234567890"
			    ;
	    
        [TestMethod]
        public void LocalConnectionInfo_MaxReceiveSize()  {
		
		    // too low
		    assertException( ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE, () => {
			    ChabuBuilder .start( 0x123, "ABC", 0 , 3);
		    });
		    assertException( ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE, () => {
			    ChabuBuilder .start( 0x123, "ABC", 0x100-1, 3);
		    });

	    }		

	    [TestMethod]
        public void LocalConnectionInfo_ApplicationName()  {


		    assertException( ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME, ()=>{
			    ChabuBuilder .start( 0x123, null, 0x100, 3);
		    });
		
		    assertException( ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME, ()=>{
			    ChabuBuilder .start( 0x123, APPLNAME_200 + "-", 0x100, 3);
		    });

		    ChabuBuilder .start(0x123, APPLNAME_200, 0x100, 3);
		    ChabuBuilder .start(0x123, "", 0x100, 3);
		
		    Chabu chabu = ChabuBuilder
				    .start(0x123, "", 0x100, 3)
				    .setConnectionValidator((local, remote) => {
					    Console.WriteLine("Local  "+local.applicationName);
                        Console.WriteLine("Remote " + remote.applicationName);
						    return null;
				    })
				    .addChannel(0, 0x100, 0, new TestChannelUser())
				    .build();
	    }		

	    [TestMethod]
        public void LocalConnectionInfo_PriorityCount() {
		
		    // Prio count <= 0
		    assertException( ChabuErrorCode.CONFIGURATION_PRIOCOUNT, ()=>{
			    ChabuBuilder.start( 0x123, "", 0x100, 0);
		    });		

		    // Prio count > 20
		    assertException( ChabuErrorCode.CONFIGURATION_PRIOCOUNT, ()=>{
			    ChabuBuilder.start( 0x123, "", 0x100, 21);
		    });
	    }		
	
	    [TestMethod]
        public void LocalConnectionInfo_ChannelConfig()  {
		
		    ChabuBuilder
			    .start( 0x123, "", 0x100, 3)
			    .addChannel( 0, 20, 0, new TestChannelUser());
		
		    assertException( ChabuErrorCode.CONFIGURATION_CH_ID, ()=>{
			    ChabuBuilder
				    .start(0x123, "", 0x100, 3)
				    .addChannel( 1, 20, 0, new TestChannelUser());
		    });
		
		    assertException( ChabuErrorCode.CONFIGURATION_CH_RECVSZ, ()=>{
			    ChabuBuilder
			    .start(0x123, "", 0x100, 3)
			    .addChannel( 0, 0, 0, new TestChannelUser());
		    });

		    assertException( ChabuErrorCode.CONFIGURATION_CH_PRIO, ()=>{
			    ChabuBuilder
			    .start(0x123, "", 0x100, 3)
			    .addChannel( 0, 20, -1 /* >= 0 */, new TestChannelUser());
		    });
		    assertException( ChabuErrorCode.CONFIGURATION_CH_PRIO, ()=>{
			    ChabuBuilder
			    .start(0x123, "", 0x100, 3 /* limit */)
			    .addChannel( 0, 20, 3 /* not < 3 */, new TestChannelUser())
			    .build(); // << will be tested in here
		    });
		
		    assertException( ChabuErrorCode.CONFIGURATION_CH_USER, ()=>{
			    ChabuBuilder
			    .start(0x123, "", 0x100, 3)
			    .addChannel( 0, 20, 0, null /*!!*/);
		    });
		
		    assertException( ChabuErrorCode.CONFIGURATION_NO_CHANNELS, ()=>{
			    ChabuBuilder
			    .start(0x123, "", 0x100, 3)
			    .build();
		    });
		
	    }		
	
	    [TestMethod]
        public void LocalConnectionInfo_ConnectionValidator()  {

		    ChabuBuilder
			    .start(0x123, "ABC", 0x100, 3)
			    .addChannel( 0, 20, 0, new TestChannelUser())
			    .setConnectionValidator( (local, remote) => null )
			    .build();
		
		    assertException( ChabuErrorCode.CONFIGURATION_VALIDATOR, ()=>{
			    ChabuBuilder
			    .start(0x123, "ABC", 0x100, 3)
			    .addChannel( 0, 20, 0, new TestChannelUser())
			    .setConnectionValidator( null )
			    .build();
			    Assert.Fail();
		    });

		    assertException( ChabuErrorCode.CONFIGURATION_VALIDATOR, ()=>{
			    ChabuBuilder
			    .start(0x123, "ABC", 0x100, 3)
			    .addChannel( 0, 20, 0, new TestChannelUser())
			    .setConnectionValidator( (local, remote) => null )
			    .setConnectionValidator( (local, remote) => null )
			    .build();
		    });

		    {
			
			    Chabu chabu = ChabuBuilder
					    .start(0x123, "ABC", 0x100, 3)
					    .addChannel( 0, 20, 0, new TestChannelUser())
					    .setConnectionValidator( (local, remote) => {
						    return new ChabuConnectionAcceptInfo( 0x177, "To Test");
					    })
					    .build();
			
			    TraceRunner r = TraceRunner.test( chabu );
			
			    r.wireRx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 00 00 00 01 00 00 01 00 00 00 01 23 00 00 00 03 41 41 41 00");
			    r.wireTx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 00 00 00 01 00 00 01 00 00 00 01 23 00 00 00 03 41 42 43 00");
			    r.wireRx("00 00 00 08 77 77 00 E1");
			    assertException( 0x177, ()=>{
				    r.wireTx(20, "00 00 00 08 77 77 00 E1");
			    });
		    }
	    }		

	    private void assertException( ChabuErrorCode ec, Runnable r ){
		    try{
			    r();
			    Assert.Fail("An Exception shall be thrown");
		    }
		    catch( ChabuException e ){
			    if( e.getRemoteCode() == 0 ){
				    if( (int)ec != e.getCode()) Console.Error.WriteLine(e.StackTrace);
                    Assert.AreEqual((int)ec, e.getCode());
			    }
			    else {
                    if ((int)ec != e.getRemoteCode()) Console.Error.WriteLine(e.StackTrace);
                    Assert.AreEqual((int)ec, e.getRemoteCode());
			    }
		    }
	    }
	    private void assertException( int code, Runnable r ){
		    try{
			    r();
                Assert.Fail();
		    }
		    catch( ChabuException e ){
			    if( e.getRemoteCode() == 0 ){
				    Assert.AreEqual( code, e.getCode());
			    }
			    else {
                    Assert.AreEqual(code, e.getRemoteCode());
			    }
		    }
	    }
	    [TestMethod]
        public void RemoteConnectionInfo_ChabuVersion()  {

		{
			Chabu chabu = ChabuBuilder
					.start(0x123, "ABC", 0x100, 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			//                                                                    <--------PV
			r.wireRx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 00 01 00 01 00 00 01 00 00 00 01 23 00 00 00 03 41 41 41 00");
			r.wireTx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 00 00 00 01 00 00 01 00 00 00 01 23 00 00 00 03 41 42 43 00");
			r.wireRx("00 00 00 08 77 77 00 E1");
			assertException( ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION, ()=>{
				r.wireTx( 20, "00 00 00 08 77 77 00 E1");
			});
		}

	}		
	
	[TestMethod]
        public void RemoteConnectionInfo_MaxReceiveSize()  {

		{
			Chabu chabu = ChabuBuilder
					.start(0x123, "ABC", 0x100, 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			
			r.wireRx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 00 00 00 01 00 00 00 FF 00 00 01 23 00 00 00 03 41 41 41 00");
			r.wireTx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 00 00 00 01 00 00 01 00 00 00 01 23 00 00 00 03 41 42 43 00");
			r.wireRx("00 00 00 08 77 77 00 E1");
			assertException( ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE, ()=>{
				r.wireTx(20, "00 00 00 08 77 77 00 E1");
			});
		}
	}		
	
	[TestMethod]
        public void RemoteConnectionInfo_Validator()  {

		{
			Chabu chabu = ChabuBuilder
					.start(0x123, "ABC", 0x100, 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			
			r.wireRx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 00 00 00 01 00 00 00 FF 00 00 01 23 00 00 00 03 41 41 41 00");
			assertException( 0x123, () => {
				r.wireTx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 00 00 00 01 00 00 01 00 00 00 01 23 00 00 00 03 41 42 43 00");
				//r.wireTx("00 0D F0 01 01 00 00 00 01 23 00 03 41 42 43");
				
				// Send an Abort
				// 7 + len
				r.wireRx("00 00 00 14 77 77 00 D2 00 00 01 23 "+TestUtils.test2LengthAndHex("bla"));
			});
		}
	}		
	
}
}