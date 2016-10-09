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


    public class ChabuXmitterStartup : ChabuXmitter {

        /**
	     * The startup data is completely sent.
	     */
        private XmitState xmitStartupCompleted = XmitState.PENDING;

        /**
	     * Have sent the ACCEPT packet
	     */
        private volatile XmitState xmitAccepted = XmitState.IDLE;

        private Setup setup;

        private readonly ArrayList<LoopCtrlAction> actionsSetupRun = new ArrayList<>();

        private Runnable startupCompletedListener;

	    {
		    actionsSetupRun.add( this::xmitAction_RemainingXmitBuf   );
		    actionsSetupRun.add( this::xmitAction_EvalStartup        );
		    actionsSetupRun.add( this::xmitAction_EvalAbort          );
		    actionsSetupRun.add( this::xmitAction_EvalAccept         );
		    actionsSetupRun.add( this::xmitAction_End                );
	    }

        public ChabuXmitterStartup(AbortMessage abortMessage, Runnable xmitRequestListener, Setup setup, Runnable startupCompletedListener) {
            super(abortMessage, xmitRequestListener);
            this.setup = setup;
            this.startupCompletedListener = startupCompletedListener;
            xmitBuf.order(ByteOrder.BIG_ENDIAN);
            xmitBuf.clear().limit(0);

            processXmitSetup();


        }


        @Override
            protected ArrayList<LoopCtrlAction> getActions() {
            return actionsSetupRun;
        }

        @Override
            protected void handleNonSeqCompletion() {
            switch (packetType) {
                case SETUP:
                    xmitStartupCompleted = XmitState.XMITTED;
                    break;

                case ACCEPT:
                    xmitAccepted = XmitState.XMITTED;
                    break;

                case ABORT:
                    xmitAbort = XmitState.XMITTED;
                    throwAbort();
                    break;

                default: break;
            }
            packetType = PacketType.NONE;
        }

        LoopCtrl xmitAction_EvalStartup() throws IOException {
		        if( xmitStartupCompleted == XmitState.PENDING ){
                processXmitSetup();
                return LoopCtrl.Continue;
            }
		        return LoopCtrl.None;
        }

        LoopCtrl xmitAction_EvalAccept() throws IOException {
		        if( xmitStartupCompleted != XmitState.XMITTED ){
                return LoopCtrl.Break;
            }
		        if( xmitAccepted == XmitState.IDLE ){
                if (setup.isValidatorWasChecked() && setup.getAcceptInfo().code == ChabuErrorCode.OK_NOERROR.getCode()) {
                    prepareXmitAccept();
                    return LoopCtrl.Continue;
                }
                else {
                    return LoopCtrl.None;
                }
            }
		        else if( xmitAccepted == XmitState.PREPARED ){
                startupCompletedListener.run();
                return LoopCtrl.Continue;
            }
		        else if( xmitAccepted == XmitState.XMITTED ){
                startupCompletedListener.run();
                return LoopCtrl.Break;
            }
		        else {
                Utils.fail(ChabuErrorCode.ASSERT, "shall not be here");
                return LoopCtrl.None;
            }

        }

        LoopCtrl xmitAction_End() throws IOException {
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

        @Override
            protected void prepareXmitAccept() {
            super.prepareXmitAccept();
            xmitAccepted = XmitState.PREPARED;
        }

        private void checkLocalAppNameLength() {
            byte[] anlBytes = setup.getInfoLocal().applicationProtocolName.getBytes(StandardCharsets.UTF_8);
            Utils.ensure(anlBytes.length <= Constants.APV_MAX_LENGTH,
                    ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME_TOO_LONG,
                    "SETUP the local application name must be less than 200 UTF8 bytes, but is %s bytes.",
                    anlBytes.length);
        }

        @Override
            public String toString() {
            return xmitBuf.toString();
        }
    }
}