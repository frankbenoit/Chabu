using System;

namespace org.chabu.test.director.prot
{
    public class ChannelState
    {
        public int ChannelId;
        public long RecvLimit;
        public long RecvPosition;
        public long XmitLimit;
        public long XmitPosition;

        public long RecvLimitSpeedKbps;
        public long RecvPositionSpeedKbps;
        public long XmitLimitSpeedKbps = 0;
        public long XmitPositionSpeedKbps = 0;

        private static long Kbps(long deltaCount, double deltaSeconds)
        {
            return (long)Math.Round(deltaCount/deltaSeconds);
        }

        public void UpdateDeltas(ChannelState oldState, double deltaSeconds )
        {
            RecvPositionSpeedKbps = Kbps(RecvPosition - oldState.RecvPosition, deltaSeconds);
            XmitPositionSpeedKbps = Kbps(XmitPosition - oldState.XmitPosition, deltaSeconds);
            RecvLimitSpeedKbps = Kbps(RecvLimit - oldState.RecvLimit, deltaSeconds);
            XmitLimitSpeedKbps = Kbps(XmitLimit - oldState.XmitLimit, deltaSeconds);
        }
    }
}