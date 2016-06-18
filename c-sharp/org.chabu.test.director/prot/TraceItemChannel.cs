using System.Collections.Generic;
using System.Diagnostics;

namespace org.chabu.test.director.prot
{
    public class TraceItemChannel : TraceItem
    {
        public readonly List<ChannelState> Channels = new List<ChannelState>();
        public long PreviousTime { get; private set; } = long.MinValue;
        public void UpdateDeltas(TraceItemChannel lastItem)
        {
            if (lastItem == null) return;
            PreviousTime = lastItem.Time;
            var deltaSeconds = (Time - PreviousTime) / (double) Stopwatch.Frequency;
            for (var i = 0; i < Channels.Count; i++)
            {
                Channels[i].UpdateDeltas(lastItem.Channels[i], deltaSeconds );
            }
        }
    }
}