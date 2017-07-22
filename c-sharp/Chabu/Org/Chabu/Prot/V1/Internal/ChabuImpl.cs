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
using System;
using System.Diagnostics;

namespace Org.Chabu.Prot.V1.Internal
{

    using global::System;
    using global::System.Text;
    using ByteBuffer = Org.Chabu.Prot.Util.ByteBuffer;
    using BitSet = global::System.Collections.BitArray;
    using PrintWriter = global::System.IO.TextWriter;
    using Runnable = global::System.Action;
    using Org.Chabu.Prot.V1;
    using global::System.Collections.Generic;


    /**
    *
    * @author Frank Benoit
    */
    internal sealed class ChabuImpl : Chabu {

        public const int SEQ_MIN_SZ = 20;

        private readonly List<ChabuChannelImpl> channels;
        private readonly Setup setup;
	    private readonly ChabuFactory factory;
	    private readonly SingleEventNotifierFromTwoSources notifierWhenRecvAndXmitCompletedStartup;
        private readonly AbortMessage xmitAbortMessage;
	    private readonly int priorityCount;

        private ChabuXmitter xmitter;
        private ChabuReceiver receiver;

        private Runnable xmitRequestListener;

        public ChabuImpl(ChabuFactory factory, ChabuSetupInfo localSetupInfo, int priorityCount,
                List<ChabuChannelImpl> channels, Runnable xmitRequestListener, ChabuConnectingValidator connectingValidator) {

            this.notifierWhenRecvAndXmitCompletedStartup = new SingleEventNotifierFromTwoSources(eventCompletedStartup);
            this.xmitRequestListener = xmitRequestListener;
            verifyLocalSetup(localSetupInfo);
            xmitAbortMessage = new AbortMessage(xmitRequestListener);
            this.channels = channels;
            this.priorityCount = priorityCount;
            this.factory = factory;

            this.setup = new Setup(localSetupInfo, xmitAbortMessage, connectingValidator);

            this.xmitter = factory.createXmitterStartup(xmitAbortMessage, xmitRequestListener, setup, xmitCompletedStartup);

            this.receiver = factory.createReceiverStartup(xmitAbortMessage, setup, recvCompletedStartup);
            verifyPriorityCount();
            verifyChannels();
        }

        private void verifyPriorityCount() {
            Utils.ensure(priorityCount >= 1 && priorityCount <= 20,
                    ChabuErrorCode.CONFIGURATION_PRIOCOUNT,
                    "Priority count must be in range 1..20, but is {0}", priorityCount);
        }

        private static void verifyLocalSetup(ChabuSetupInfo localSetupInfo) {

            Utils.ensure(localSetupInfo.recvPacketSize >= Constants.MAX_RECV_LIMIT_LOW, ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_TOO_LOW,
                    "maxReceiveSize must be at least 0x100, but is {0}", localSetupInfo.recvPacketSize);

            Utils.ensure(localSetupInfo.recvPacketSize <= Constants.MAX_RECV_LIMIT_HIGH, ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_TOO_HIGH,
                    "maxReceiveSize must be max 0x{0:X}, but is {1}", Constants.MAX_RECV_LIMIT_HIGH, localSetupInfo.recvPacketSize);

            Utils.ensure(Utils.isAligned4(localSetupInfo.recvPacketSize), ChabuErrorCode.SETUP_LOCAL_MAXRECVSIZE_NOT_ALIGNED,
                    "maxReceiveSize must 4-byte aligned: {0}", localSetupInfo.recvPacketSize);

            Utils.ensure(localSetupInfo.applicationProtocolName != null, ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME_NULL,
                    "applicationName must not be null");

            int nameBytes = localSetupInfo.applicationProtocolName.getBytes(StandardCharsets.UTF_8).Length;
            Utils.ensure(nameBytes <= Constants.APV_MAX_LENGTH, ChabuErrorCode.SETUP_LOCAL_APPLICATIONNAME_TOO_LONG,
                    "applicationName must have length at maximum 200 UTF8 bytes, but has {0}", nameBytes);
        }

        private void verifyChannels() {
            Utils.ensure(channels.size() > 0, ChabuErrorCode.CONFIGURATION_NO_CHANNELS, "No channels are set.");

            foreach (ChabuChannelImpl ch in channels) {
                Utils.ensure(ch.Priority < priorityCount, ChabuErrorCode.CONFIGURATION_CH_PRIO,
                        "Channel {0} has higher priority ({1}) as the max {2}",
                        ch.ChannelId, ch.Priority, priorityCount);
            }

        }

        private void eventCompletedStartup() {
            activateAllChannels();
        }

        private void activateAllChannels() {
            for (int i = 0; i < channels.size(); i++) {
                ChabuChannelImpl ch = channels.get(i);
                ch.activate(this, i);
            }
        }

        private void xmitCompletedStartup() {
            xmitter = factory.createXmitterNormal(xmitAbortMessage, xmitRequestListener, priorityCount, channels, (int _priorityCount, int _channelCount) => new Priorizer(_priorityCount, _channelCount), setup.getRemoteMaxReceiveSize());
            notifierWhenRecvAndXmitCompletedStartup.event1();
        }

        private void recvCompletedStartup() {
            receiver = factory.createReceiverNormal(receiver, channels, xmitAbortMessage, setup);
            notifierWhenRecvAndXmitCompletedStartup.event2();
        }

        internal void channelXmitRequestArm(int channelId) {
            xmitter.channelXmitRequestArm(channelId);
        }

        internal void channelXmitRequestData(int channelId) {
            xmitter.channelXmitRequestData(channelId);
        }

        /////////////////////////////////////////////////////////////////////////////////
        // public interface methods

        public void HandleChannel(ByteChannel channel) {
            receiver.recv(channel);
            xmitter.xmit(channel);
        }

        public int getChannelCount() {
            return channels.size();
        }

        public ChabuChannel getChannel(int channelId) {
            return channels.get(channelId);
        }

        public override string ToString() {
            return string.Format("Chabu[ recv:%s xmit:%s ]", receiver, xmitter);
        }

    }
}