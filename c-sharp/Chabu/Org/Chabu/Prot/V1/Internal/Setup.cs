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
    public class Setup {
        private ChabuSetupInfo infoLocal;
        private ChabuSetupInfo infoRemote;

        private readonly ChabuConnectingValidator connectingValidator;

        /**
         * Have recv the ACCEPT packet
         */
        private RecvState recvAccepted = RecvState.IDLE;

        /**
         * The setup data is completely received.
         */
        private RecvState recvSetupCompleted = RecvState.WAITING;

        private ChabuConnectionAcceptInfo acceptInfo;
        private AbortMessage abortMessage;

        public Setup(ChabuSetupInfo info, AbortMessage abortMessage, ChabuConnectingValidator connectingValidator) {
            this.infoLocal = info;
            this.infoRemote = new ChabuSetupInfo();
            this.abortMessage = abortMessage;
            this.connectingValidator = connectingValidator;
        }

        public void setRemote(ChabuSetupInfo info) {
            infoRemote = info;
            this.recvSetupCompleted = RecvState.RECVED;
        }
        public int getRemoteMaxReceiveSize() {
            return infoRemote.recvPacketSize;
        }
        public bool isValidatorWasChecked() {
            return acceptInfo != null;
        }
        public ChabuConnectionAcceptInfo getAcceptInfo() {
            if (acceptInfo == null) {
                if (connectingValidator != null) {
                    acceptInfo = connectingValidator(infoLocal, infoRemote);
                }
                if (acceptInfo == null) {
                    acceptInfo = new ChabuConnectionAcceptInfo(0, "");
                }
            }
            return acceptInfo;
        }

        public bool isRemoteSetupReceived() {
            return recvSetupCompleted == RecvState.RECVED;
        }
        public ChabuSetupInfo getInfoLocal() {
            return infoLocal;
        }

        internal void checkConnectingValidator() {
            checkConnectingValidatorMaxReceiveSize();
            callApplicationAcceptListener();
        }


        private bool callApplicationAcceptListener() {
            bool isOk = true;
            if (!isValidatorWasChecked()) {

    
                ChabuConnectionAcceptInfo acceptInfo = getAcceptInfo();

                if (acceptInfo != null && acceptInfo.code != 0) {
                    isOk = false;
                    abortMessage.setPending(acceptInfo.code, acceptInfo.message);
                }
            }
            return isOk;
        }

        private void checkConnectingValidatorMaxReceiveSize() {
            int maxReceiveSize = getRemoteMaxReceiveSize();
            if (maxReceiveSize < Constants.MAX_RECV_LIMIT_LOW) {

                string msg = string.Format("MaxReceiveSize too low: 0x%X", maxReceiveSize);

                abortMessage.setPending(ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE_TOO_LOW, msg);

            }
            else if (maxReceiveSize > Constants.MAX_RECV_LIMIT_HIGH) {

                string msg = string.Format("MaxReceiveSize too high 0x%X", maxReceiveSize);

                abortMessage.setPending(ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE_TOO_HIGH, msg);

            }
            else if (!Utils.isAligned4(maxReceiveSize)) {

                string msg = string.Format("MaxReceiveSize is not aligned 0x%X", maxReceiveSize);

                abortMessage.setPending(ChabuErrorCode.SETUP_REMOTE_MAXRECVSIZE_NOT_ALIGNED, msg);

            }
        }

        public void setRemoteAcceptReceived() {
            Utils.ensure(!isRemoteAcceptReceived(), ChabuErrorCode.PROTOCOL_ACCEPT_TWICE, "Recveived ACCEPT twice");
            recvAccepted = RecvState.RECVED;
        }

        public bool isRemoteAcceptReceived() {
            return recvAccepted == RecvState.RECVED;
        }

        public ChabuSetupInfo getInfoRemote() {
            return infoRemote;
        }

    }
}
