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

namespace Org.Chabu.Prot.V1.Internal
{
    using global::System.Collections.Generic;
    using Moq;
    using Runnable = global::System.Action;

    [TestClass]
    public class ChabuReceiverStartupTest {

	    Setup setup;
	    Mock<AbortMessage> abortMessage;
	    Mock<ChabuChannel> channel;
	    Mock<Runnable> completionListener;
	    List<ChabuChannel> channels = new List<ChabuChannel>();
	    private ChabuReceiverStartup sut;
	    private TestByteChannel byteChannel;

	    [TestInitialize] public void 
	    Setup() {
            abortMessage = new Mock<AbortMessage>();
            channel = new Mock<ChabuChannel>();
            completionListener = new Mock<Runnable>();
		    setup = new Setup(new ChabuSetupInfo(), abortMessage.Object, null );
		    sut = new ChabuReceiverStartup(abortMessage.Object, setup, completionListener.Object);
		    channels.Add( new Mock<ChabuChannel>().Object );
		    byteChannel = new TestByteChannel( 1000, 1000 );
	    }
	
	    private String getStringData( String text ){
		    byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		    int length = Utils.alignUpTo4(bytes.Length);
		    byte[] data = new byte[length+4];
		    data[0] = (byte)(bytes.Length >> 24);
		    data[1] = (byte)(bytes.Length >> 16);
		    data[2] = (byte)(bytes.Length >>  8);
		    data[3] = (byte)(bytes.Length >>  0);
		    System.arraycopy(bytes, 0, data, 4, bytes.Length );
		    String res = TestUtils.toHexString(data, true);
		    return res + " ";
	    }
	    private String getIntData( int v ){
		    return String.Format("{0:X2} {1:X2} {2:X2} {3:X2} ", 
				    ( v >> 24 ) & 0xFF, 
				    ( v >> 16 ) & 0xFF, 
				    ( v >>  8 ) & 0xFF, 
				    ( v >>  0 ) & 0xFF); 
	    }
	    [TestMethod]
	    public void getStringDataIsGood()  {
		    Assert.AreEqual(getStringData("ABC"), "00 00 00 03 41 42 43 00 ");
	    }
	    [TestMethod]
	    public void getSetupRecvDataIsGood()  {
		    Assert.AreEqual(getSetupRecvData("CHABU", Constants.PROTOCOL_VERSION, 1000, 0x12345678, "ABC")
			    ,"00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 " +
				    "55 00 00 00 "+TestUtils.getChabuVersionAsHex()+"00 00 03 E8 12 34 56 78 " + 
				    "00 00 00 03 41 42 43 00 ");
	    }
	    private String getSetupRecvData(String protocolName, int chabuProtVersion, int recvPacketSize, int applProtVersion, String applProtName ) {
		    return getIntData(0x1C + Utils.alignUpTo4(applProtName.length()) + Utils.alignUpTo4(protocolName.length()) ) + 
				    "77 77 00 F0 " + 
				    getStringData( protocolName) + 
				    getIntData(chabuProtVersion) + 
				    getIntData(recvPacketSize) + 
				    getIntData(applProtVersion) + 
				    getStringData( applProtName );
	    }	
	
	    private String getSetupRecvData() {
		    return getSetupRecvData("CHABU", Constants.PROTOCOL_VERSION, 1000, 0x12345678, "ABC");
	    }	

	    private String getAcceptData() {
		    return "00 00 00 08 77 77 00 E1 ";
	    }

	    private void prepareSetupData() {
		    byteChannel.putRecvData( getSetupRecvData());
	    }

	
	    [TestMethod] public void
	    recv_remote_setup()  {
		    prepareSetupData();
		    sut.recv(byteChannel);
		    Assert.AreEqual(setup.getInfoRemote(), new ChabuSetupInfo( 1000, 0x12345678 , "ABC" ));
	    }

	    [TestMethod] public void
	    recv_remote_setup_with_wrong_protocol_name()  {
		    byteChannel.putRecvData(getSetupRecvData("Thabu", Constants.PROTOCOL_VERSION, 1000, 0x12345678, "ABC"));
		    sut.recv(byteChannel);
		    sut.recv(byteChannel);
            abortMessage.Verify( a => a.setPending(ChabuErrorCode.SETUP_REMOTE_CHABU_NAME, It.IsAny<String>()));
	    }	
	
	    [TestMethod] public void
	    recv_remote_setup_with_wrong_protocol_major_version()  {
		    byteChannel.putRecvData(getSetupRecvData("CHABU", Constants.PROTOCOL_VERSION + 0x10000, 1000, 0x12345678, "ABC"));
		    sut.recv(byteChannel);
            abortMessage.Verify(a => a.setPending(ChabuErrorCode.SETUP_REMOTE_CHABU_NAME, It.IsAny<String>()));
	    }	
	
	    [TestMethod] public void
	    recv_remote_setup_with_wrong_protocol_minor_version_ignored()  {
		    byteChannel.putRecvData(getSetupRecvData("CHABU", Constants.PROTOCOL_VERSION + 0x1000, 1000, 0x12345678, "ABC"));
		    sut.recv(byteChannel);
		    abortMessage.Verify(am => am.setPending(
                ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION, It.IsAny<String>()), Times.Never );
	    }	
	
	    [TestMethod] public void
	    recv_remote_setup_with_single_bytes()  {
		    String data = getSetupRecvData();
		
		    while( !data.isEmpty() ){
			    String singleData = data.Substring(0, 3);
			    data = data.Substring(3);
			    byteChannel.putRecvData( singleData );
			    sut.recv(byteChannel);
		    }
		
		    Assert.AreEqual(setup.getInfoRemote(), new ChabuSetupInfo( 1000, 0x12345678 , "ABC" ));
	    }
	
	    [TestMethod]
        [ExpectedException(typeof(SystemException),
            "A userId of null was inappropriately allowed.")]
        public void
	    recv_remote_setup_with_too_long_protocol_name()  {
		    String data = "00 00 00 2C 77 77 00 F0 00 00 00 09 43 48 41 42 " +
				    "55 00 00 00 00 00 00 00 "+TestUtils.getChabuVersionAsHex()+"00 00 03 E8 12 34 56 78 " + 
				    "00 00 00 03 41 42 43 00 ";
		    byteChannel.putRecvData( data );
            sut.recv(byteChannel);
		    //.isInstanceOf(ChabuException.class)
		    //.hasMessageContaining("exceeds max allowed length");
		
	    }
	
	    [TestMethod]
        [ExpectedException(typeof(SystemException),
            "A userId of null was inappropriately allowed.")]
        public void
	    recv_remote_setup_with_too_long_appl_protocol_name()  {
		    String data = "00 00 00 60 77 77 00 F0 00 00 00 05 43 48 41 42 " +
				    "55 00 00 00 "+TestUtils.getChabuVersionAsHex()+"00 00 03 E8 12 34 56 78 " + 
				    "00 00 00 39 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 ";
		    byteChannel.putRecvData( data );
			sut.recv(byteChannel);
		    //.isInstanceOf(ChabuException.class)
		    //.hasMessageContaining("exceeds max allowed length");
		
	    }
	
	    [TestMethod]
        [ExpectedException(typeof(SystemException),
            "A userId of null was inappropriately allowed.")]
        public void
	    recv_remote_setup_with_appl_protocol_name_not_fully_contained_in_recv_buffer()  {
		    String data = "00 00 00 30 77 77 00 F0 00 00 00 05 43 48 41 42 " +
				    "55 00 00 00 "+TestUtils.getChabuVersionAsHex()+"00 00 03 E8 12 34 56 78 " + 
				    "00 00 00 30 41 42 43 00 41 42 43 00 41 42 43 00 41 42 43 00 41 ";
		    byteChannel.putRecvData( data );
			sut.recv(byteChannel);
		    //.isInstanceOf(ChabuException.class)
		    //.hasMessageContaining("exceeds packet length");
		
	    }
	
	    [TestMethod] public void
	    recv_accept()  {
		    byteChannel.putRecvData(
				    "00 00 00 28 77 77 00 F0 00 00 00 05 43 48 41 42 " +
				    "55 00 00 00 "+TestUtils.getChabuVersionAsHex()+"00 00 03 E8 12 34 56 78 " + 
				    "00 00 00 03 41 42 43 00 " + 
				    getAcceptData());
		    sut.recv(byteChannel);
		    Assert.IsTrue(setup.isRemoteAcceptReceived());
		    completionListener.Verify( r => r());
	    }

	    [TestMethod] public void
	    recv_accept_without_setup()  {
		    String data = getAcceptData();
		    byteChannel.putRecvData( data );
		    sut.recv(byteChannel);
            abortMessage.Verify(m => m.setPending(
                ChabuErrorCode.PROTOCOL_ACCEPT_WITHOUT_SETUP,
                It.IsAny<String>()));
	    }
	
	    [TestMethod]
        [ExpectedException(typeof(ChabuException),
            "A userId of null was inappropriately allowed.")]
        public void recv_abort()  {
		    String data = "00 00 00 08 77 77 00 D2 00 02 00 00 00 00 00 03 41 42 43 00";
		    byteChannel.putRecvData( data );
			sut.recv(byteChannel);
		    //.isInstanceOf(ChabuException.class)
		    //.hasMessageContaining("ABC");

	    }
	
    }
}