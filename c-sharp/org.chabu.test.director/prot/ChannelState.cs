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

        public void UpdateDeltas(ChannelState oldState, int deltaMillis )
        {
            RecvPositionSpeedKbps = (int)(RecvPosition - oldState.RecvPosition) * 1000 / deltaMillis;
            XmitPositionSpeedKbps = (int)(XmitPosition - oldState.XmitPosition) * 1000 / deltaMillis;
            RecvLimitSpeedKbps = (int)(RecvLimit - oldState.RecvLimit) * 1000 / deltaMillis;
            XmitLimitSpeedKbps = (int)(XmitLimit - oldState.XmitLimit) * 1000 / deltaMillis;
        }
    }
}