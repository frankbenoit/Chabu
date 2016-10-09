/*******************************************************************************
 * The MIT License (MIT)
 * Copyright (c) 2015 Frank Benoit, Stuttgart, Germany <keinfarbton@gmail.com>
 * 
 * See the LICENSE.md or the online documentation:
 * https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g/edit#heading=h.2kvlhpr5zi2u
 * 
 * Contributors:
 *     Frank Benoit - initial API and implementation
 *******************************************************************************/

namespace Org.Chabu.Prot.V1.Internal
{

    import java.nio.charset.StandardCharsets;

import org.chabu.prot.v1.ChabuErrorCode;
import org.chabu.prot.v1.ChabuException;
import org.chabu.prot.v1.ChabuSetupInfo;

public class ChabuReceiverStartup extends ChabuReceiver {

	private readonly Runnable completedStartupListener;
	private readonly Setup setup;

	public ChabuReceiverStartup(AbortMessage localAbortMessage, Setup setup, Runnable completedStartup) {
		super(null, localAbortMessage);
		this.completedStartupListener = completedStartup;
		this.setup = setup;
	}

	@Override
	protected void processRecvSetup() {
		
		/// when is startupRx set before?
		Utils.ensure( !setup.isRemoteSetupReceived(), ChabuErrorCode.PROTOCOL_SETUP_TWICE, "Recveived SETUP twice" );
//		Utils.ensure( activated, ChabuErrorCode.NOT_ACTIVATED, "While receiving the SETUP block, org.chabu was not activated." );

		String pn = getRecvString(8);
		int pv = recvBuf.getInt();
		int rs = recvBuf.getInt();
		int av = recvBuf.getInt();
		String an = getRecvString(56);

		if( !Constants.PROTOCOL_NAME.equals(pn) ) {
			localAbortMessage.setPending( ChabuErrorCode.SETUP_REMOTE_CHABU_NAME.getCode(), 
					String.format("Chabu protocol name mismatch. Expected %s, received %s", Constants.PROTOCOL_NAME, pn ));
			return;
		}
		

		ChabuSetupInfo info = new ChabuSetupInfo( rs, av, an );

		setup.setRemote(info);

		if(( pv >>> 16 ) != (Constants.PROTOCOL_VERSION >>> 16 )) {
			localAbortMessage.setPending( ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION.getCode(), String.format("Chabu Protocol Version: expt 0x%08X recv 0x%08X", 
					Constants.PROTOCOL_VERSION, pv ));
			return;
		}
				
		setup.checkConnectingValidator();
	}

	@Override
	protected void processRecvAccept() {
		if( !setup.isRemoteSetupReceived()){
			localAbortMessage.setPending(ChabuErrorCode.PROTOCOL_ACCEPT_WITHOUT_SETUP, "Accept was received before a Setup packet.");
		}
		setup.setRemoteAcceptReceived();
		cancelCurrentReceive = true;
		completedStartupListener.run();
	}

	@Override
	protected void processRecvAbort() {
		
		int code =  recvBuf.getInt();
		String message = getRecvString(56);
		
		throw new ChabuException( ChabuErrorCode.REMOTE_ABORT, code, String.format("Recveived ABORT Code=0x%08X: %s", code, message ));
	}

	private String getRecvString(int maxByteCount){
		
		int len = recvBuf.getInt();
		if( len > maxByteCount ){
			throw new ChabuException(String.format("Chabu string length (%d) exceeds max allowed length (%d)",
					len, maxByteCount ));
		}
		if( recvBuf.remaining() < len ){
			throw new ChabuException(String.format("Chabu string length exceeds packet length len:%d data-remaining:%d",
					len, recvBuf.remaining() ));
		}
			
		byte[] bytes = new byte[len];
		recvBuf.get( bytes );
		while( (len & 3) != 0 ){
			len++;
			recvBuf.get();
		}
		return new String( bytes, StandardCharsets.UTF_8 );
	}
	
	@Override
	public String toString() {
		return recvBuf.toString();
	}
}
