package org.chabu.prot.v1.internal;

import org.chabu.prot.v1.ChabuConnectingValidator;
import org.chabu.prot.v1.ChabuConnectionAcceptInfo;
import org.chabu.prot.v1.ChabuErrorCode;
import org.chabu.prot.v1.ChabuSetupInfo;

public class Setup {

	private ChabuSetupInfo infoLocal;
	private ChabuSetupInfo infoRemote;
	private ChabuConnectingValidator val;
	
	/**
	 * Have recv the ACCEPT packet
	 */
	private RecvState recvAccepted = RecvState.IDLE;

	/**
	 * The setup data is completely received.
	 */
	private RecvState recvSetupCompleted = RecvState.WAITING;

	private ChabuConnectionAcceptInfo acceptInfo = null;
	private ConnectionAccepter connectionAccepter;
	private Aborter aborter;
	
	public Setup(Aborter aborter, ConnectionAccepter connectionAccepter){
		this.aborter = aborter;
		this.connectionAccepter = connectionAccepter;
	}
	
	public void setLocal(ChabuSetupInfo info) {
		infoLocal = info;
	}
	public void setRemote(ChabuSetupInfo info) {
		infoRemote = info;
		this.recvSetupCompleted = RecvState.RECVED;
	}
	public int getRemoteMaxReceiveSize() {
		return infoRemote.maxReceiveSize;
	}
	public boolean isValidatorWasChecked() {
		return acceptInfo != null;
	}
	public ChabuConnectionAcceptInfo getAcceptInfo() {
		if( acceptInfo == null ){
			if( val == null ){
				acceptInfo = new ChabuConnectionAcceptInfo( 0, "" );
			}
			else {
				acceptInfo = val.isAccepted(infoLocal, infoRemote);
			}
		}
		return acceptInfo;
	}
	public void setValidator(ChabuConnectingValidator val) {
		this.val = val;
	}
	public ChabuConnectingValidator getValidator() {
		return val;
	}

	public boolean isRemoteSetupReceived() {
		return recvSetupCompleted == RecvState.RECVED;
	}
	public ChabuSetupInfo getInfoLocal() {
		return infoLocal;
	}
	
	void checkConnectingValidator() {
		checkConnectingValidatorMaxReceiveSize();

		boolean isOk = callApplicationAcceptListener();
		if( !isOk ) {
			return;
		}

		connectionAccepter.acceptConnection(getRemoteMaxReceiveSize());
	}


	private boolean callApplicationAcceptListener() {
		boolean isOk = true;
		if( !isValidatorWasChecked() ){
			ChabuConnectionAcceptInfo acceptInfo = getAcceptInfo();
			if( acceptInfo != null && acceptInfo.code != 0 ){
				isOk = false;
				aborter.delayedAbort(acceptInfo.code, acceptInfo.message );
			}
		}
		return isOk;
	}

	private void checkConnectingValidatorMaxReceiveSize() {
		int maxReceiveSize = getRemoteMaxReceiveSize();
		if( maxReceiveSize < Constants.MAX_RECV_LIMIT_LOW ){
			
			String msg = String.format( "MaxReceiveSize too low: 0x%X", maxReceiveSize);
			
			aborter.delayedAbort(ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE_TOO_LOW.getCode(), msg);
			
		}
		else if( maxReceiveSize > Constants.MAX_RECV_LIMIT_HIGH ){
			
			String msg = String.format( "MaxReceiveSize too high 0x%X", maxReceiveSize);
			
			aborter.delayedAbort(ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE_TOO_HIGH.getCode(), msg);
			
		}
		else if( !Utils.isAligned4( maxReceiveSize ) ){
			
			String msg = String.format( "MaxReceiveSize is not aligned 0x%X", maxReceiveSize);
			
			aborter.delayedAbort(ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE_NOT_ALIGNED.getCode(), msg);
			
		}
	}

	public void setRemoteAcceptReceived() {
		Utils.ensure( !isRemoteAcceptReceived(), ChabuErrorCode.PROTOCOL_ACCEPT_TWICE, "Recveived ACCEPT twice" );
		recvAccepted = RecvState.RECVED;
	}
	
	public boolean isRemoteAcceptReceived() {
		return recvAccepted == RecvState.RECVED;
	}

}
