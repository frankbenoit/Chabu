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

namespace Org.Chabu.Prot.V1.Internal
{
    using global::System.Text;
    using Runnable = global::System.Action;

    internal class ChabuReceiverStartup : ChabuReceiver {

        private readonly Runnable completedStartupListener;
        private readonly Setup setup;

        public ChabuReceiverStartup(AbortMessage localAbortMessage, Setup setup, Runnable completedStartup)
            : base(null, localAbortMessage)
        {
            this.completedStartupListener = completedStartup;
            this.setup = setup;
        }

        protected override void processRecvSetup() {

            /// when is startupRx set before?
            Utils.ensure(!setup.isRemoteSetupReceived(), ChabuErrorCode.PROTOCOL_SETUP_TWICE, "Recveived SETUP twice");
            //		Utils.ensure( activated, ChabuErrorCode.NOT_ACTIVATED, "While receiving the SETUP block, org.chabu was not activated." );

            string pn = getRecvString(8);
            int pv = recvBuf.getInt();
            int rs = recvBuf.getInt();
            int av = recvBuf.getInt();
            string an = getRecvString(56);

            if (!Constants.PROTOCOL_NAME.equals(pn)) {
                localAbortMessage.setPending(ChabuErrorCode.SETUP_REMOTE_CHABU_NAME,
                        string.Format("Chabu protocol name mismatch. Expected {0}, received {1}", Constants.PROTOCOL_NAME, pn));
                return;
            }


            ChabuSetupInfo info = new ChabuSetupInfo(rs, av, an);

            setup.setRemote(info);

            if ((pv >> 16) != (Constants.PROTOCOL_VERSION >> 16)) {
                localAbortMessage.setPending(ChabuErrorCode.SETUP_REMOTE_CHABU_VERSION, string.Format("Chabu Protocol Version: expt 0x{0:X8} recv 0x{1:X8}",
                        Constants.PROTOCOL_VERSION, pv));
                return;
            }

            setup.checkConnectingValidator();
        }

        protected override void processRecvAccept() {
            if (!setup.isRemoteSetupReceived()) {
                localAbortMessage.setPending(ChabuErrorCode.PROTOCOL_ACCEPT_WITHOUT_SETUP, "Accept was received before a Setup packet.");
            }
            setup.setRemoteAcceptReceived();
            cancelCurrentReceive = true;
            completedStartupListener();
        }

        protected override void processRecvAbort() {

            int code = recvBuf.getInt();
            string message = getRecvString(56);

            throw new ChabuException(ChabuErrorCode.REMOTE_ABORT, code, string.Format("Recveived ABORT Code=0x{0:X8}: {1}", code, message));
        }

        private string getRecvString(int maxByteCount) {

            int len = recvBuf.getInt();
            if (len > maxByteCount) {
                throw new ChabuException(string.Format("Chabu string length ({0}) exceeds max allowed length ({1})",
                        len, maxByteCount));
            }
            if (recvBuf.remaining() < len) {
                throw new ChabuException(string.Format("Chabu string length exceeds packet length len:{0} data-remaining:{1}",
                        len, recvBuf.remaining()));
            }

            byte[] bytes = new byte[len];
            recvBuf.get(bytes);
            while ((len & 3) != 0) {
                len++;
                recvBuf.get();
            }
            return Encoding.UTF8.GetString(bytes);
        }

        public override string ToString() {
            return recvBuf.ToString();
        }
    }
}
