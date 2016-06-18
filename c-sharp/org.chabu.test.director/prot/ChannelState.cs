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

        public int RecvLimitSpeedKbps;
        public int RecvPositionSpeedKbps;
        public int XmitLimitSpeedKbps;
        public int XmitPositionSpeedKbps;

        private static int Kbps(long deltaCount, double deltaSeconds)
        {
            return (int) Math.Round(deltaCount/deltaSeconds);
        }

        public void UpdateDeltas(ChannelState oldState, double deltaSeconds )
        {
            RecvPositionSpeedKbps = Kbps(RecvPosition - oldState.RecvPosition, deltaSeconds);
            Console.WriteLine($@"kbps{RecvPositionSpeedKbps} {RecvPosition} {oldState.RecvPosition} {deltaSeconds}");
            XmitPositionSpeedKbps = Kbps(XmitPosition - oldState.XmitPosition, deltaSeconds);
            RecvLimitSpeedKbps = Kbps(RecvLimit - oldState.RecvLimit, deltaSeconds);
            XmitLimitSpeedKbps = Kbps(XmitLimit - oldState.XmitLimit, deltaSeconds);
        }
    }
}