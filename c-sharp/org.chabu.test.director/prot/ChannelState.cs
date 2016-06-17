namespace org.chabu.test.director.prot
{
    public class ChannelState
    {
        public int ChannelId;
        public long RecvLimit;
        public long RecvPosition;
        public long XmitLimit;
        public long XmitPosition;

        public int RecvLimitDelta;
        public int RecvPositionDelta;
        public int XmitLimitDelta;
        public int XmitPositionDelta;

        public void UpdateDeltas(ChannelState oldState)
        {
            RecvPositionDelta = (int)(RecvPosition - oldState.RecvPosition);
            XmitPositionDelta = (int)(XmitPosition - oldState.XmitPosition);
            RecvLimitDelta = (int)(RecvLimit - oldState.RecvLimit);
            XmitLimitDelta = (int)(XmitLimit - oldState.XmitLimit);
        }
    }
}