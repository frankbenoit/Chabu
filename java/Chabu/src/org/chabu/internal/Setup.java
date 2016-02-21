package org.chabu.internal;

import org.chabu.ChabuConnectingValidator;
import org.chabu.ChabuConnectionAcceptInfo;
import org.chabu.ChabuErrorCode;
import org.chabu.ChabuSetupInfo;

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
	
	public void setLocal(ChabuSetupInfo info) {
		infoLocal = info;
	}
	public void setRemote(ChabuSetupInfo info) {
		infoRemote = info;
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

	public void setRecvSetupCompleted(RecvState recvSetupCompleted) {
		this.recvSetupCompleted = recvSetupCompleted;
	}
	public boolean isRemoteSetupReceived() {
		return recvSetupCompleted == RecvState.RECVED;
	}
	public ChabuSetupInfo getInfoLocal() {
		return infoLocal;
	}
	
	void checkConnectingValidator(ChabuXmitter xmitter) {
		if( isCheckConnectingValidatorNeeded(xmitter) ){
			
			checkConnectingValidatorMaxReceiveSize(xmitter);

			boolean isOk = callApplicationAcceptListener(xmitter);
			if( !isOk ) {
				return;
			}
	
			xmitter.ensureXmitBufMatchesReceiveSize(getRemoteMaxReceiveSize());
			xmitter.prepareXmitAccept();
		}
	}


	private boolean callApplicationAcceptListener(ChabuXmitter xmitter) {
		boolean isOk = true;
		if( !isValidatorWasChecked() ){
			ChabuConnectionAcceptInfo acceptInfo = getAcceptInfo();
			if( acceptInfo != null && acceptInfo.code != 0 ){
				isOk = false;
				xmitter.delayedAbort(acceptInfo.code, acceptInfo.message );
			}
		}
		return isOk;
	}

	private void checkConnectingValidatorMaxReceiveSize(ChabuXmitter xmitter) {
		int maxReceiveSize = getRemoteMaxReceiveSize();
		if( maxReceiveSize < 0x100 || maxReceiveSize >= 0x10000 ){
			xmitter.delayedAbort(ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE.getCode(), 
				String.format("MaxReceiveSize must be on range 0x100 .. 0xFFFF bytes, "
						+ "but SETUP from remote contained 0x%02X", 
						maxReceiveSize));
		}
		Utils.ensure( ( maxReceiveSize & 3 ) == 0, ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE, 
				"maxReceiveSize must 4-byte aligned: %s", maxReceiveSize );
	}

	private boolean isCheckConnectingValidatorNeeded(ChabuXmitter xmitter) {
		return !xmitter.isAcceptedXmitted() 
				&& isRemoteSetupReceived()
				&& xmitter.isStartupXmitted();
	}
	public void setRemoteAcceptReceived() {
		Utils.ensure( !isRemoteAcceptReceived(), ChabuErrorCode.PROTOCOL_ACCEPT_TWICE, 
				"Recveived ACCEPT twice" );
		recvAccepted = RecvState.RECVED;
	}
	public boolean isRemoteAcceptReceived() {
		return recvAccepted == RecvState.RECVED;
	}

}
