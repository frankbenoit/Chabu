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
    using global::System.Collections.Generic;
    using Runnable = global::System.Action;

    internal class ChabuXmitterStartup : ChabuXmitter {

        /**
	     * The startup data is completely sent.
	     */
        private XmitState xmitStartupCompleted = XmitState.PENDING;

        /**
	     * Have sent the ACCEPT packet
	     */
        private volatile XmitState xmitAccepted = XmitState.IDLE;

        private Setup setup;

        private readonly List<LoopCtrlAction> actionsSetupRun;

        private Runnable startupCompletedListener;


        public ChabuXmitterStartup(AbortMessage abortMessage, Runnable xmitRequestListener, Setup setup, Runnable startupCompletedListener)
            : base (abortMessage, xmitRequestListener)
        {

            actionsSetupRun = new List<LoopCtrlAction>
            {
                xmitAction_RemainingXmitBuf,
                xmitAction_EvalStartup     ,
                xmitAction_EvalAbort       ,
                xmitAction_EvalAccept      ,
                xmitAction_End             ,
            };

            this.setup = setup;
            this.startupCompletedListener = startupCompletedListener;
            xmitBuf.order(ByteOrder.BIG_ENDIAN);
            xmitBuf.clear().limit(0);

            processXmitSetup();


        }


        internal override List<LoopCtrlAction> getActions() {
            return actionsSetupRun;
        }

        protected override void handleNonSeqCompletion() {
            switch (packetType) {
                case PacketType.SETUP:
                    xmitStartupCompleted = XmitState.XMITTED;
                    break;

                case PacketType.ACCEPT:
                    xmitAccepted = XmitState.XMITTED;
                    break;

                case PacketType.ABORT:
                    xmitAbort = XmitState.XMITTED;
                    throwAbort();
                    break;

                default: break;
            }
            packetType = PacketType.NONE;
        }

        LoopCtrl xmitAction_EvalStartup()  {
		    if( xmitStartupCompleted == XmitState.PENDING ){
                processXmitSetup();
                return LoopCtrl.Continue;
            }
		    return LoopCtrl.None;
        }

        LoopCtrl xmitAction_EvalAccept()  {
	        if( xmitStartupCompleted != XmitState.XMITTED ){
                return LoopCtrl.Break;
            }
	        if( xmitAccepted == XmitState.IDLE ){
                if (setup.isValidatorWasChecked() && setup.getAcceptInfo().code == (int)ChabuErrorCode.OK_NOERROR) {
                    prepareXmitAccept();
                    return LoopCtrl.Continue;
                }
                else {
                    return LoopCtrl.None;
                }
            }
		    else if( xmitAccepted == XmitState.PREPARED ){
                startupCompletedListener();
                return LoopCtrl.Continue;
            }
		    else if( xmitAccepted == XmitState.XMITTED ){
                startupCompletedListener();
                return LoopCtrl.Break;
            }
	        else {
                Utils.fail((int)ChabuErrorCode.ASSERT, "shall not be here");
                return LoopCtrl.None;
            }

        }

        LoopCtrl xmitAction_End()  {
		        return LoopCtrl.Break;
        }

        /**
         * Put on the buffer the needed org.chabu protocol informations: org.chabu version, 
         * byte order, payloadsize, channel count
         * 
         * These values must be set previous to infoLocal
         * 
         */
        void processXmitSetup() {
            checkXmitBufEmptyOrThrow("Cannot xmit SETUP, buffer is not empty");
            checkLocalAppNameLength();
            xmitFillSetupPacket(setup.getInfoLocal());
            xmitStartupCompleted = XmitState.PREPARED;
        }

        protected override void prepareXmitAccept() {
            base.prepareXmitAccept();
            xmitAccepted = XmitState.PREPARED;
        }

        private void checkLocalAppNameLength() {
            byte[] anlBytes = setup.getInfoLocal().applicationProtocolName.getBytes(StandardCharsets.UTF_8);
            Utils.ensure(anlBytes.Length <= Constants.APV_MAX_LENGTH,
                    ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME_TOO_LONG,
                    "SETUP the local application name must be less than 200 UTF8 bytes, but is {0} bytes.",
                    anlBytes.Length);
        }

        public override string ToString() {
            return xmitBuf.ToString();
        }
    }
}