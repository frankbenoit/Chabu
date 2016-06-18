using System;
using System.Collections.Generic;
using System.Linq;

namespace org.chabu.test.director.prot
{
    public class Trace
    {
        private readonly List<TraceItem> traceItems = new List<TraceItem>(100);
        private TraceItemChannel lastTraceItemChannelA;
        private TraceItemChannel lastTraceItemChannelB;

        public int ModifyCount { get; private set; } = 0;
        public int ChannelCount { get; private set; } = 0;
        public long FirstTime { get; private set; } = long.MinValue;

        public void Add(TraceItem item)
        {
            ModifyCount++;
            traceItems.Add(item);

            if (FirstTime == long.MinValue)
            {
                FirstTime = item.Time;
            }

            var chItem = item as TraceItemChannel;
            if (chItem == null) return;

            if (ChannelCount == 0)
            {
                ChannelCount = chItem.Channels.Count;
            }

            if (item.Host == Host.A)
            {
                chItem.UpdateDeltas(lastTraceItemChannelB);
                lastTraceItemChannelB = chItem;
            }
            else
            {
                chItem.UpdateDeltas(lastTraceItemChannelA);
                lastTraceItemChannelA = chItem;
            }
        }

        public TraceItemChannel GetLastTraceItemChannelFor(Host host, Func<TraceItemChannel> defaultValue)
        {
            var idx = traceItems.Count;
            while (idx > 0)
            {
                idx--;
                var item = traceItems[idx];
                var channelItem = item as TraceItemChannel;
                if (channelItem != null && channelItem.Host == host)
                {
                    return channelItem;
                }
            }
            return defaultValue?.Invoke();
        }

        public List<TraceItemChannel> GetLastTraceItemChannelsFor(Host host)
        {
            var res = traceItems
                .Where(x => x.Host == host)
                .OfType<TraceItemChannel>( )
                .ToList();
            return res;
        }
    }
}