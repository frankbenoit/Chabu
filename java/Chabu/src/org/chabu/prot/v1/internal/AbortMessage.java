package org.chabu.prot.v1.internal;

public class AbortMessage {
	
	private XmitState    xmitAbortPending = XmitState.IDLE;
	private int          xmitAbortCode    = 0;
	private String       xmitAbortMessage = "";
	
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

	public void setPending(int code, String message) {
		xmitAbortCode    = code;
		xmitAbortMessage = message;
		xmitAbortPending = XmitState.PENDING;
	}
}
