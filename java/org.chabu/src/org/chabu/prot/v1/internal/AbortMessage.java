package org.chabu.prot.v1.internal;

import org.chabu.prot.v1.ChabuErrorCode;

public class AbortMessage {
	
	private XmitState    xmitAbortPending = XmitState.IDLE;
	private int          xmitAbortCode    = 0;
	private String       xmitAbortMessage = "";
	private final Runnable xmitRequestListener;
	
	public AbortMessage(Runnable xmitRequestListener) {
		this.xmitRequestListener = xmitRequestListener;
	}

	public boolean isPending() {
		return xmitAbortPending == XmitState.PENDING;
	}
	
	public boolean isIdle() {
		return xmitAbortPending == XmitState.IDLE;
	}

	public int getCode() {
		return xmitAbortCode;
	}
	
	public String getMessage() {
		return xmitAbortMessage;
	}
	
	public void setXmitted() {
		xmitAbortCode = 0;
		xmitAbortMessage = "";
		xmitAbortPending = XmitState.XMITTED;
	}

	public void setPending(ChabuErrorCode code, String message) {
		setPending( code.getCode(), message );
	}
	public void setPending(int code, String message) {
		xmitAbortCode    = code;
		xmitAbortMessage = message;
		xmitAbortPending = XmitState.PENDING;
		if( xmitRequestListener != null ){
			xmitRequestListener.run();
		}
	}
}
