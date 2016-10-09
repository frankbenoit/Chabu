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
using System;
using System.Diagnostics;

namespace Org.Chabu.Prot.V1.Internal
{
    using Runnable = global::System.Action;

    public class AbortMessage {

        private XmitState xmitAbortPending = XmitState.IDLE;
        private int xmitAbortCode = 0;
        private String xmitAbortMessage = "";
        private readonly Runnable xmitRequestListener;
	
	    public AbortMessage(Runnable xmitRequestListener) {
            this.xmitRequestListener = xmitRequestListener;
        }

        public bool isPending() {
            return xmitAbortPending == XmitState.PENDING;
        }

        public bool isIdle() {
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
            setPending(code, message);
        }
        public void setPending(int code, String message) {
            xmitAbortCode = code;
            xmitAbortMessage = message;
            xmitAbortPending = XmitState.PENDING;
            if (xmitRequestListener != null) {
                xmitRequestListener.Invoke();
            }
        }
    }
}