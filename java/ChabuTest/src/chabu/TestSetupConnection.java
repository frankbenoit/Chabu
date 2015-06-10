package chabu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

@SuppressWarnings("unused")
public class TestSetupConnection {

	private final String APPLNAME_200 = "" 
			+ "12345678901234567890123456789012345678901234567890"
			+ "12345678901234567890123456789012345678901234567890"
			+ "12345678901234567890123456789012345678901234567890"
			+ "12345678901234567890123456789012345678901234567890"
			;
	@Test
	public void LocalConnectionInfo_MaxReceivePayloadSize() throws Exception {
		
		ChabuSetupInfo ci = new ChabuSetupInfo();
		ci.applicationName = "ABC";
		ci.applicationVersion = 0x123;

		// too low
		ci.maxReceivePayloadSize = 0;
		assertException( ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE, ()->{
			ChabuBuilder .start(ci, new TestNetwork(), 3);
		});
		ci.maxReceivePayloadSize = 0x100-1;
		assertException( ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE, ()->{
			ChabuBuilder .start(ci, new TestNetwork(), 3);
		});
		// too much
		ci.maxReceivePayloadSize = 0xFFFF+1;
		assertException( ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE, ()->{
			ChabuBuilder .start(ci, new TestNetwork(), 3);
		});
		

	}		

	@Test
	public void LocalConnectionInfo_ApplicationName() throws Exception {

		ChabuSetupInfo ci = new ChabuSetupInfo();

		ci.applicationVersion = 0x123;
		ci.maxReceivePayloadSize = 0x100;

		ci.applicationName = null;
		assertException( ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME, ()->{
			ChabuBuilder .start(ci, new TestNetwork(), 3);
		});
		
		ci.applicationName = APPLNAME_200 + "-";
		assertException( ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME, ()->{
			ChabuBuilder .start(ci, new TestNetwork(), 3);
		});

		ci.applicationName = APPLNAME_200;
		ChabuBuilder .start(ci, new TestNetwork(), 3);
		ci.applicationName = "";
		ChabuBuilder .start(ci, new TestNetwork(), 3);
		
		IChabu chabu = ChabuBuilder
				.start(ci, new TestNetwork(), 3)
				.setConnectionValidator((local, remote) -> {
					System.out.println("Local  "+remote.applicationName);
					System.out.println("Remote "+remote.applicationName);
						return null;
				})
				.addChannel(0, 0x100, 0, new TestChannelUser())
				.build();
	}		

	@Test
	public void LocalConnectionInfo_NetworkNull() {

		ChabuSetupInfo ci = new ChabuSetupInfo();
		
		ci.applicationVersion = 0x123;
		ci.maxReceivePayloadSize = 0x100;
		ci.applicationName = "";

		assertException( ChabuErrorCode.CONFIGURATION_NETWORK, ()->{
			ChabuBuilder.start(ci, null, 3);
		});
	}		
	
	
	@Test
	public void LocalConnectionInfo_PriorityCount() {
		
		ChabuSetupInfo ci = new ChabuSetupInfo();
		
		ci.applicationVersion = 0x123;
		ci.maxReceivePayloadSize = 0x100;
		ci.applicationName = "";
		
		// Prio count <= 0
		assertException( ChabuErrorCode.CONFIGURATION_PRIOCOUNT, ()->{
			ChabuBuilder.start(ci, new TestNetwork(), 0);
		});		
		// Prio count > 20
		assertException( ChabuErrorCode.CONFIGURATION_PRIOCOUNT, ()->{
			ChabuBuilder.start(ci, new TestNetwork(), 21);
		});
	}		
	
	@Test
	public void LocalConnectionInfo_ChannelConfig() throws Exception {
		
		ChabuSetupInfo ci = new ChabuSetupInfo();
		
		ci.applicationVersion = 0x123;
		ci.maxReceivePayloadSize = 0x100;
		ci.applicationName = "";

		ChabuBuilder
			.start(ci, new TestNetwork(), 3)
			.addChannel( 0, 20, 0, new TestChannelUser());
		
		assertException( ChabuErrorCode.CONFIGURATION_CH_ID, ()->{
			ChabuBuilder
				.start(ci, new TestNetwork(), 3)
				.addChannel( 1, 20, 0, new TestChannelUser());
		});
		
		assertException( ChabuErrorCode.CONFIGURATION_CH_RECVSZ, ()->{
			ChabuBuilder
			.start(ci, new TestNetwork(), 3)
			.addChannel( 0, 0, 0, new TestChannelUser());
		});

		assertException( ChabuErrorCode.CONFIGURATION_CH_PRIO, ()->{
			ChabuBuilder
			.start(ci, new TestNetwork(), 3)
			.addChannel( 0, 20, -1 /* >= 0 */, new TestChannelUser());
		});
		assertException( ChabuErrorCode.CONFIGURATION_CH_PRIO, ()->{
			ChabuBuilder
			.start(ci, new TestNetwork(), 3 /* limit */)
			.addChannel( 0, 20, 3 /* not < 3 */, new TestChannelUser())
			.build(); // << will be tested in here
		});
		
		assertException( ChabuErrorCode.CONFIGURATION_CH_USER, ()->{
			ChabuBuilder
			.start(ci, new TestNetwork(), 3)
			.addChannel( 0, 20, 0, null /*!!*/);
		});
		
		assertException( ChabuErrorCode.CONFIGURATION_NO_CHANNELS, ()->{
			ChabuBuilder
			.start(ci, new TestNetwork(), 3)
			.build();
		});
		
	}		
	
	@Test
	public void LocalConnectionInfo_ConnectionValidator() throws Exception {
		ChabuSetupInfo ci = new ChabuSetupInfo();
		
		ci.applicationVersion = 0x123;
		ci.maxReceivePayloadSize = 0x100;
		ci.applicationName = "ABC";

		ChabuBuilder
			.start(ci, new TestNetwork(), 3)
			.addChannel( 0, 20, 0, new TestChannelUser())
			.setConnectionValidator( (local, remote) -> null )
			.build();
		
		assertException( ChabuErrorCode.CONFIGURATION_VALIDATOR, ()->{
			ChabuBuilder
			.start(ci, new TestNetwork(), 3)
			.addChannel( 0, 20, 0, new TestChannelUser())
			.setConnectionValidator( null )
			.build();
			fail();
		});

		assertException( ChabuErrorCode.CONFIGURATION_VALIDATOR, ()->{
			ChabuBuilder
			.start(ci, new TestNetwork(), 3)
			.addChannel( 0, 20, 0, new TestChannelUser())
			.setConnectionValidator( (local, remote) -> null )
			.setConnectionValidator( (local, remote) -> null )
			.build();
		});

		{
			
			IChabu chabu = ChabuBuilder
					.start(ci, new TestNetwork(), 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.setConnectionValidator( (local, remote) -> {
						return new ChabuConnectionAcceptInfo( 0x177, "To Test");
					})
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			
			r.wireRx("00 0D F0 01 01 00 00 00 01 23 00 03 41 41 41");
			r.wireTx("00 0D F0 01 01 00 00 00 01 23 00 03 41 42 43");
			r.wireRx("00 01 E1");
			assertException( 0x177, ()->{
				r.wireTx("00 01 E1");
			});
		}
	}		

	private void assertException( ChabuErrorCode ec, Runnable r ){
		try{
			r.run();
			fail();
		}
		catch( ChabuException e ){
			assertEquals( ec.getCode(), e.getCode());
		}
	}
	private void assertException( int code, Runnable r ){
		try{
			r.run();
			fail();
		}
		catch( ChabuException e ){
			assertEquals( code, e.getCode());
		}
	}
	@Test
	public void RemoteConnectionInfo_ChabuVersion() throws Exception {
		ChabuSetupInfo ci = new ChabuSetupInfo();
		
		ci.applicationVersion = 0x123;
		ci.maxReceivePayloadSize = 0x100;
		ci.applicationName = "ABC";


		{
			IChabu chabu = ChabuBuilder
					.start(ci, new TestNetwork(), 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			
			r.wireRx("00 0D F0 02 01 00 00 00 01 23 00 03 41 41 41");
			r.wireTx("00 0D F0 01 01 00 00 00 01 23 00 03 41 42 43");
			r.wireRx("00 01 E1");
			assertException( ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION, ()->{
				r.wireTx("00 01 E1");
			});
		}
		{
			IChabu chabu = ChabuBuilder
					.start(ci, new TestNetwork(), 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			
			r.wireTx("00 0D F0 01 01 00 00 00 01 23 00 03 41 42 43");
			r.wireRx("00 0D F0 02 01 00 00 00 01 23 00 03 41 41 41");
			r.wireRx("00 01 E1");
			assertException( ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION, ()->{
				r.wireTx("00 01 E1");
			});
		}
		{
			IChabu chabu = ChabuBuilder
					.start(ci, new TestNetwork(), 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			
			r.wireTx("00 0D F0 01 01 00 00 00 01 23 00 03 41 42 43");
			r.wireRx("00 0D F0 02 01 00 00 00 01 23 00 03 41 41 41");
			//r.wireRx("00 01 E1");
			assertException( ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION, ()->{
				r.wireTx("00 01 E1");
			});
		}
	}		
	
	@Test
	public void RemoteConnectionInfo_MaxReceivePayloadSize() throws Exception {
		ChabuSetupInfo ci = new ChabuSetupInfo();
		
		ci.applicationVersion = 0x123;
		ci.maxReceivePayloadSize = 0x100;
		ci.applicationName = "ABC";


		{
			IChabu chabu = ChabuBuilder
					.start(ci, new TestNetwork(), 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			
			r.wireRx("00 0D F0 01 00 FF 00 00 01 23 00 03 41 41 41");
			r.wireTx("00 0D F0 01 01 00 00 00 01 23 00 03 41 42 43");
			r.wireRx("00 01 E1");
			assertException( ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE, ()->{
				r.wireTx("00 01 E1");
			});
		}
	}		
	
	@Test
	public void RemoteConnectionInfo_Validator() throws Exception {
		ChabuSetupInfo ci = new ChabuSetupInfo();
		
		ci.applicationVersion = 0x123;
		ci.maxReceivePayloadSize = 0x100;
		ci.applicationName = "ABC";


		{
			IChabu chabu = ChabuBuilder
					.start(ci, new TestNetwork(), 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			
			r.wireRx("00 0D F0 01 00 FF 00 00 01 23 00 03 41 41 41");
			r.wireTx("00 0D F0 01 01 00 00 00 01 23 00 03 41 42 43");
			// 7 + len
			assertException( ChabuErrorCode.REMOTE_ABORT, ()->{
				r.wireRx("00 0A D2 00 00 01 23 "+TestUtils.test2LengthAndHex("bla"));
			});
		}
	}		
	
}
