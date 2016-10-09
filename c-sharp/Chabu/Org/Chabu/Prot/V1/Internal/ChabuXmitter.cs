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
    using ByteBuffer = global::System.IO.MemoryStream;
    using Runnable = global::System.Action;
    using global::System.Collections.Generic;
    using global::System;

    internal abstract class ChabuXmitter
    {
        internal delegate LoopCtrl LoopCtrlAction();
        private static readonly int SETUP_PROTNAME_MAXLENGTH = 8;
        private static readonly int SETUP_APPNAME_MAXLENGTH = 56;
        private static readonly int ABORT_MSG_MAXLENGTH = 56;
        protected static readonly int PT_MAGIC = 0x77770000;

        internal enum LoopCtrl {
            Break, Continue, None
        }



        internal abstract List<LoopCtrlAction> getActions();
        private readonly Runnable xmitRequestListener;
        private readonly AbortMessage abortMessage;

        internal ByteBuffer xmitBuf = new ByteBuffer(Constants.MAX_RECV_LIMIT_LOW);
        internal PacketType packetType = PacketType.NONE;
        internal ByteChannel loopByteChannel;
        internal XmitState xmitAbort = XmitState.IDLE;

        public ChabuXmitter(AbortMessage abortMessage, Runnable xmitRequestListener) {
            this.abortMessage = abortMessage;
            this.xmitRequestListener = xmitRequestListener;
        }

        protected void runActionsUntilBreak() {
            List<LoopCtrlAction> loopActions = getActions();
            Lwhile: while( true ) {
                foreach (LoopCtrlAction action in loopActions) {
                    LoopCtrl loopCtrl = action.Invoke();
                    if (loopCtrl == LoopCtrl.Break) return;
                    if (loopCtrl == LoopCtrl.Continue) goto Lwhile;
                }
            }
        }

        protected void processXmitNop() {
            checkXmitBufEmptyOrThrow("Cannot xmit NOP, buffer is not empty");
            xmitFillStart(PacketType.NOP);
            xmitFillComplete();
        }

        public void xmit(ByteChannel byteChannel) {
		    this.loopByteChannel = byteChannel;
		    try{
                runActionsUntilBreak();
            }
            finally {
                this.loopByteChannel = null;
            }
        }

        protected LoopCtrl xmitAction_EvalAbort() {
		    if( abortMessage.isPending() ){
                prepareAbort();
                return LoopCtrl.Continue;
            }
		    return LoopCtrl.None;
        }

        protected LoopCtrl xmitAction_RemainingXmitBuf() {
		
		    if( xmitBuf.hasRemaining() ){
                loopByteChannel.write(xmitBuf);
            }
		
		    if( !xmitBuf.hasRemaining() && packetType != PacketType.SEQ ){
                handleNonSeqCompletion();
                packetType = PacketType.NONE;
            }
		    if( xmitBuf.hasRemaining() ){
                callXmitRequestListener();
            }
		
		    return xmitBuf.hasRemaining() ? LoopCtrl.Break : LoopCtrl.None;
        }



        protected abstract void handleNonSeqCompletion();

        protected void xmitFillSetupPacket(ChabuSetupInfo setupInfo) {
            xmitFillStart(PacketType.SETUP);
            xmitFillAddstring(SETUP_PROTNAME_MAXLENGTH, Constants.PROTOCOL_NAME);
            xmitFillAddInt(Constants.PROTOCOL_VERSION);
            xmitFillAddInt(setupInfo.recvPacketSize);
            xmitFillAddInt(setupInfo.applicationVersion);
            xmitFillAddstring(SETUP_APPNAME_MAXLENGTH, setupInfo.applicationProtocolName);
            xmitFillComplete();
        }

        protected virtual void prepareXmitAccept() {
            checkXmitBufEmptyOrThrow("Cannot xmit ACCEPT, buffer is not empty");
            xmitFillStart(PacketType.ACCEPT);
            xmitFillComplete();
        }

        protected void xmitFillAddInt(int value) {
            xmitBuf.putInt(value);
        }

        protected void xmitFillAddstring(int maxLength, string str) {
            byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
            if (bytes.Length > maxLength) {
                byte[] bytes2 = new byte[maxLength];
                System.arraycopy(bytes, 0, bytes2, 0, maxLength);
                bytes = bytes2;
            }
            xmitFillAddstring(bytes);
        }

        protected void xmitFillAddstring(byte[] bytes) {
            xmitBuf.putInt(bytes.Length);
            xmitBuf.put(bytes);
            xmitFillAligned();
        }
        void prepareAbort() {
            checkXmitBufEmptyOrThrow("Cannot xmit ABORT, buffer is not empty");
            xmitFillStart(PacketType.ABORT);
            xmitFillAddInt(abortMessage.getCode());
            xmitFillAddstring(ABORT_MSG_MAXLENGTH, abortMessage.getMessage());
            xmitFillComplete();
            xmitAbort = XmitState.PREPARED;
        }
        internal void xmitFillArmPacket(int channelId, int arm) {
            checkXmitBufEmptyOrThrow("Cannot xmit ACCEPT, buffer is not empty");
            xmitFillStart(PacketType.ARM);
            xmitBuf.putInt(channelId);
            xmitBuf.putInt(arm);
            xmitFillComplete();
        }

        internal void xmitFillStart(PacketType type) {
            packetType = type;
            xmitBuf.clear();
            xmitBuf.putInt(-1);
            xmitBuf.putInt(PT_MAGIC | (int)type);
        }

        protected void xmitFillComplete() {
            xmitFillAligned();
            xmitBuf.putInt(0, xmitBuf.position());
            xmitBuf.flip();
        }
        protected void xmitFillComplete(int packetSize) {
            xmitFillAligned();
            xmitBuf.putInt(0, packetSize);
            xmitBuf.flip();
        }

        private void xmitFillAligned() {
            while (xmitBuf.position() % 4 != 0) {
                xmitBuf.put((byte)0);
            }
        }
        protected void checkXmitBufEmptyOrThrow(string message) {
            if (xmitBuf.hasRemaining()) {
                throw new ChabuException(message);
            }
        }

        public void delayedAbort(int code, string message, params object[] args) {
            Utils.ensure(xmitAbort == XmitState.IDLE,
                    ChabuErrorCode.ASSERT,
                    "Abort is already pending while generating Abort from Validator");

            abortMessage.setPending(code, string.Format(message, args));
            xmitAbort = XmitState.PENDING;

            callXmitRequestListener();

        }

        protected void throwAbort() {
            int code = abortMessage.getCode();
            string msg = abortMessage.getMessage();

            abortMessage.setXmitted();

            throw new ChabuException(ChabuErrorCode.REMOTE_ABORT, code,
                    string.Format("Remote Abort: Code:0x%08X (%d) %s", code, code, msg));
        }



        public void delayedAbort(ChabuErrorCode ec, string message, params object[] args) {
            delayedAbort(ec, message, args);
        }

        protected void callXmitRequestListener() {
            if (xmitRequestListener != null) {
                xmitRequestListener.Invoke();
            }
        }

        public virtual void channelXmitRequestArm(int channelId) {
            throw new Exception("Xmit request for ARM before activation, channel: " + channelId);
        }

        public virtual void channelXmitRequestData(int channelId) {
            throw new Exception("Xmit request for data before activation, channel: " + channelId);
        }




    }
}