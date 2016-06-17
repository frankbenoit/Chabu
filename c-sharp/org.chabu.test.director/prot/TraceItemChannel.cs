using System.Collections.Generic;
using System.Diagnostics;

namespace org.chabu.test.director.prot
{
    public class TraceItemChannel : TraceItem
    {
        public readonly List<ChannelState> Channels = new List<ChannelState>();

        public void UpdateDeltas(TraceItemChannel lastItem)
        {
            if (lastItem == null) return;
            var deltaMillis = (int)(( Time - lastItem.Time ) * 1000 / Stopwatch.Frequency);
            for (var i = 0; i < Channels.Count; i++)
            {
                Channels[i].UpdateDeltas(lastItem.Channels[i], deltaMillis );
            }
        }
    }
}