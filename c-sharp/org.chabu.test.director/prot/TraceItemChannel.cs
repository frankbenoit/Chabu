using System.Collections.Generic;

namespace org.chabu.test.director.prot
{
    public class TraceItemChannel : TraceItem
    {
        public readonly List<ChannelState> Channels = new List<ChannelState>();

        public void UpdateDeltas(TraceItemChannel lastItem)
        {
            if (lastItem == null) return;
            for (var i = 0; i < Channels.Count; i++)
            {
                Channels[i].UpdateDeltas(lastItem.Channels[i]);
            }
        }
    }
}