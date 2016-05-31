package org.chabu;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.chabu.prot.v1.Chabu;
import org.chabu.prot.v1.ChabuBuilder;
import org.chabu.prot.v1.ChabuConnectionAcceptInfo;
import org.chabu.prot.v1.ChabuErrorCode;
import org.chabu.prot.v1.ChabuException;
import org.chabu.prot.v1.internal.Constants;
import org.chabu.prot.v1.internal.Setup;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.sun.deploy.uitoolkit.impl.fx.Utils;

@SuppressWarnings("unused")
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSetupConnection {

	private final String APPLNAME_56 = "" 
			+ "12345678901234567890123456789012345678901234567890"
			+ "123456"
			;
	
	static final int MAX_RECV_SIZE_LOW = 0x100;
	static final int MAX_RECV_SIZE_HIGH = 0x1000_0000;
	String CHABU_VERSION_HEX;
	
	@Before
	public void setup() {
		CHABU_VERSION_HEX = ""; 
		for( int i = 0; i < 4; i++ ){
			CHABU_VERSION_HEX = String.format("%02X %s", (Constants.PROTOCOL_VERSION >>> (8*i)) & 0xFF, CHABU_VERSION_HEX); 
		}
	}

	@Test
	public void LocalConnectionInfo_MaxReceiveSize_too_low() throws Exception {
		
		assertEquals( MAX_RECV_SIZE_LOW, Constants.MAX_RECV_LIMIT_LOW);
		
		// too low
		assertException( ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_TOO_LOW, ()->{
			ChabuBuilder .start( 0x123, "ABC", 0 , 3);
		});
		assertException( ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_TOO_LOW, ()->{
			ChabuBuilder .start( 0x123, "ABC", MAX_RECV_SIZE_LOW-1, 3);
		});
	}		

	@Test
	public void LocalConnectionInfo_MaxReceiveSize_not_aligned() throws Exception {
		
		assertException( ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_NOT_ALIGNED, ()->{
			ChabuBuilder .start( 0x123, "ABC", MAX_RECV_SIZE_LOW+2 , 3);
		});
	}		
	
	@Test
	public void LocalConnectionInfo_MaxReceiveSize_too_high() throws Exception {
		
		assertEquals( MAX_RECV_SIZE_HIGH, Constants.MAX_RECV_LIMIT_HIGH);

		assertException( ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_TOO_HIGH, ()->{
			ChabuBuilder .start( 0x123, "ABC", MAX_RECV_SIZE_HIGH+4, 3);
		});
		assertException( ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_TOO_HIGH, ()->{
			ChabuBuilder .start( 0x123, "ABC", 7*MAX_RECV_SIZE_HIGH, 3);
		});
		ChabuBuilder .start( 0x123, "ABC", MAX_RECV_SIZE_HIGH, 3);
	}		
	
	@Test
	public void LocalConnectionInfo_ApplicationName() throws Exception {


		assertException( ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME_NULL, ()->{
			ChabuBuilder .start( 0x123, null, 0x100, 3);
		});
		
		assertException( ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME_TOO_LONG, ()->{
			ChabuBuilder .start( 0x123, APPLNAME_56 + "-", 0x100, 3);
		});

		ChabuBuilder .start(0x123, APPLNAME_56, 0x100, 3);
		ChabuBuilder .start(0x123, "", 0x100, 3);
		
		Chabu chabu = ChabuBuilder
				.start(0x123, "", 0x100, 3)
				.setConnectionValidator((local, remote) -> {
					System.out.println("Local  "+local.applicationName);
					System.out.println("Remote "+remote.applicationName);
						return null;
				})
				.addChannel(0, 0x100, 0, new TestChannelUser())
				.build();
	}		

	@Test
	public void LocalConnectionInfo_PriorityCount() {
		
		// Prio count <= 0
		assertException( ChabuErrorCode.CONFIGURATION_PRIOCOUNT, ()->{
			ChabuBuilder.start( 0x123, "", 0x100, 0)
				.addChannel( 0, 20, 0, new TestChannelUser())
				.build();
			
		});		

		// Prio count > 20
		assertException( ChabuErrorCode.CONFIGURATION_PRIOCOUNT, ()->{
			ChabuBuilder.start( 0x123, "", 0x100, 21)
				.addChannel( 0, 20, 0, new TestChannelUser())
				.build();
		});
	}		
	
	@Test
	public void LocalConnectionInfo_ChannelConfig() throws Exception {
		
		ChabuBuilder
			.start( 0x123, "", 0x100, 3)
			.addChannel( 0, 20, 0, new TestChannelUser());
		
		assertException( ChabuErrorCode.CONFIGURATION_CH_ID, ()->{
			ChabuBuilder
				.start(0x123, "", 0x100, 3)
				.addChannel( 1, 20, 0, new TestChannelUser());
		});
		
		assertException( ChabuErrorCode.CONFIGURATION_CH_RECVSZ, ()->{
			ChabuBuilder
			.start(0x123, "", 0x100, 3)
			.addChannel( 0, 0, 0, new TestChannelUser());
		});

		assertException( ChabuErrorCode.CONFIGURATION_CH_PRIO, ()->{
			ChabuBuilder
			.start(0x123, "", 0x100, 3)
			.addChannel( 0, 20, -1 /* >= 0 */, new TestChannelUser());
		});
		assertException( ChabuErrorCode.CONFIGURATION_CH_PRIO, ()->{
			ChabuBuilder
			.start(0x123, "", 0x100, 3 /* limit */)
			.addChannel( 0, 20, 3 /* not < 3 */, new TestChannelUser())
			.build(); // << will be tested in here
		});
		
		assertException( ChabuErrorCode.CONFIGURATION_CH_USER, ()->{
			ChabuBuilder
			.start(0x123, "", 0x100, 3)
			.addChannel( 0, 20, 0, null /*!!*/);
		});
		
		assertException( ChabuErrorCode.CONFIGURATION_NO_CHANNELS, ()->{
			ChabuBuilder
			.start(0x123, "", 0x100, 3)
			.build();
		});
		
	}		
	
	@Test
	public void LocalConnectionInfo_ConnectionValidator() throws Exception {

		ChabuBuilder
			.start(0x123, "ABC", 0x100, 3)
			.addChannel( 0, 20, 0, new TestChannelUser())
			.setConnectionValidator( (local, remote) -> null )
			.build();
		
		assertException( ChabuErrorCode.CONFIGURATION_VALIDATOR, ()->{
			ChabuBuilder
			.start(0x123, "ABC", 0x100, 3)
			.addChannel( 0, 20, 0, new TestChannelUser())
			.setConnectionValidator( null )
			.build();
			fail();
		});

		assertException( ChabuErrorCode.CONFIGURATION_VALIDATOR, ()->{
			ChabuBuilder
			.start(0x123, "ABC", 0x100, 3)
			.addChannel( 0, 20, 0, new TestChannelUser())
			.setConnectionValidator( (local, remote) -> null )
			.setConnectionValidator( (local, remote) -> null )
			.build();
		});

		{
			
			Chabu chabu = ChabuBuilder
					.start(0x123, "ABC", 0x100, 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.setConnectionValidator( (local, remote) -> {
						return new ChabuConnectionAcceptInfo( ChabuErrorCode.APPLICATION_VALIDATOR.getCode() + 0x77, "To Test");
					})
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			
			r.wireRx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 "+CHABU_VERSION_HEX+"00 00 01 00 00 00 01 23 00 00 00 03 41 41 41 00");
			r.wireTx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 "+CHABU_VERSION_HEX+"00 00 01 00 00 00 01 23 00 00 00 03 41 42 43 00");
			r.wireRx("00 00 00 08 77 77 00 E1");
			assertException( 0x1000077, ()->{
				r.wireTx("00 00 00 18 77 77 00 D2 01 00 00 77 00 00 00 07 54 6F 20 54 65 73 74 00");
			});
		}
	}		

	private void assertException( ChabuErrorCode ec, ThrowingCallable r ){
		try{
			r.call();
			fail("An Exception shall be thrown");
		}
		catch( ChabuException e ){
			if( e.getRemoteCode() == 0 ){
				if( ec.getCode() != e.getCode()) e.printStackTrace();
				assertEquals( ec.getCode(), e.getCode());
			}
			else {
				if( ec.getCode() != e.getRemoteCode()) e.printStackTrace();
				assertEquals( ec.getCode(), e.getRemoteCode());
			}
		}
		catch( Throwable e ){
			throw new RuntimeException(e);
		}
	}
	private void assertException( int code, ThrowingCallable r ){
		try{
			r.call();
			fail();
		}
		catch( ChabuException e ){
			if( e.getRemoteCode() == 0 ){
				assertEquals( code, e.getCode());
			}
			else {
				assertEquals( code, e.getRemoteCode());
			}
		}
		catch( Throwable e ){
			throw new RuntimeException(e);
		}
	}
	@Test
	public void RemoteConnectionInfo_ChabuVersion() throws Exception {

		{
			Chabu chabu = ChabuBuilder
					.start(0x123, "ABC", 0x100, 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			//                                                                    <--------PV
			r.wireRx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 FF FF 00 01 00 00 01 00 00 00 01 23 00 00 00 03 41 41 41 00");
			r.wireTx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 "+CHABU_VERSION_HEX+"00 00 01 00 00 00 01 23 00 00 00 03 41 42 43 00");
			r.wireRx("00 00 00 08 77 77 00 E1");
			assertException( ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION, ()->{
				r.wireTx(
						"00 00 00 48 77 77 00 D2 00 1F 00 02 00 00 00 37 " +
						"43 68 61 62 75 20 50 72 6F 74 6F 63 6F 6C 20 56 " +
						"65 72 73 69 6F 6E 3A 20 65 78 70 74 20 30 78 30 " +
						"30 30 31 30 30 30 30 20 72 65 63 76 20 30 78 46 " +
						"46 46 46 30 30 30 31 00");
			});
		}

	}		
	
	@Test
	public void RemoteConnectionInfo_MaxReceiveSize_too_low() throws Exception {

		{
			Chabu chabu = ChabuBuilder
					.start(0x123, "ABC", 0x100, 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			
			r.wireRx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 "+CHABU_VERSION_HEX+"00 00 00 FC 00 00 01 23 00 00 00 03 41 41 41 00");
			r.wireTx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 "+CHABU_VERSION_HEX+"00 00 01 00 00 00 01 23 00 00 00 03 41 42 43 00");
			r.wireRx("00 00 00 08 77 77 00 E1");
			assertException( ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE_TOO_LOW, ()->{
				r.wireTx("00 00 00 2C 77 77 00 D2 00 20 00 03 00 00 00 1C 4D 61 78 52 65 63 65 69 76 65 53 69 7A 65 20 74 6F 6F 20 6C 6F 77 3A 20 30 78 46 43");
			});
		}
	}		
	@Test
	public void RemoteConnectionInfo_MaxReceiveSize_not_aligned() throws Exception {
		
		{
			Chabu chabu = ChabuBuilder
					.start(0x123, "ABC", 0x100, 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			
			r.wireRx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 "+CHABU_VERSION_HEX+"00 00 01 01 00 00 01 23 00 00 00 03 41 41 41 00");
			r.wireTx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 "+CHABU_VERSION_HEX+"00 00 01 00 00 00 01 23 00 00 00 03 41 42 43 00");
			r.wireRx("00 00 00 08 77 77 00 E1");
			assertException( ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE_NOT_ALIGNED, ()->{
				r.wireTx("00 00 00 34 77 77 00 D2 00 20 00 05 00 00 00 23 4D 61 78 52 65 63 65 69 76 65 53 69 7A 65 20 69 73 20 6E 6F 74 20 61 6C 69 67 6E 65 64 20 30 78 31 30 31 00");
			});
		}
	}		
	@Test
	public void RemoteConnectionInfo_MaxReceiveSize_too_high() throws Exception {
		
		{
			Chabu chabu = ChabuBuilder
					.start(0x123, "ABC", 0x100, 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			
			r.wireRx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 "+CHABU_VERSION_HEX+"10 00 00 04 00 00 01 23 00 00 00 03 41 41 41 00");
			r.wireTx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 "+CHABU_VERSION_HEX+"00 00 01 00 00 00 01 23 00 00 00 03 41 42 43 00");
			r.wireRx("00 00 00 08 77 77 00 E1");
			assertException( ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE_TOO_HIGH, ()->{
				r.wireTx("00 00 00 34 77 77 00 D2 00 20 00 04 00 00 00 22 4D 61 78 52 65 63 65 69 76 65 53 69 7A 65 20 74 6F 6F 20 68 69 67 68 20 30 78 31 30 30 30 30 30 30 34 00 00");
			});
		}
	}		
	
	@Test
	public void RemoteConnectionInfo_Validator() throws Exception {

		{
			Chabu chabu = ChabuBuilder
					.start(0x123, "ABC", 0x100, 3)
					.addChannel( 0, 20, 0, new TestChannelUser())
					.build();
			
			TraceRunner r = TraceRunner.test( chabu );
			
			r.wireRx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 "+CHABU_VERSION_HEX+"00 00 00 FF 00 00 01 23 00 00 00 03 41 41 41 00");
			assertException( 0x123, () -> {
				r.wireTx("00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 55 00 00 00 "+CHABU_VERSION_HEX+"00 00 01 00 00 00 01 23 00 00 00 03 41 42 43 00");
				//r.wireTx("00 0D F0 01 01 00 00 00 01 23 00 03 41 42 43");
				
				// Send an Abort
				// 7 + len
				r.wireRx("00 00 00 14 77 77 00 D2 00 00 01 23 "+TestUtils.text2LengthAndHex("bla"));
			});
		}
	}		
	
}
