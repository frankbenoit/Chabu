using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using org.chabu.test.director.gui;

namespace org.chabu.test.director.prot
{
    public class TraceItem
    {
        public Host Host { get; set; }
        public long Time { get; set; }
    }

    public class TraceItemChannel : TraceItem
    {
        public List<TestNode.GetStateResultChannel> Channels { get; set; }
    }

    public enum EventType
    {
            
    }

    public class TraceItemEvent : TraceItem
    {

    }

    public class Trace
    {
        private readonly List<TraceItem> traceItems = new List<TraceItem>(100);

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

        public void Add(TraceItem item)
        {
            traceItems.Add(item);
        }
    }

    public class TestCtx
    {
        private readonly ILogContainer log;
        public TestNode HostA { get; }
        public TestNode HostB { get; }
        public Trace Trace { get; }

        public TestCtx(ILogContainer log, string hostAaddr, string hostBaddr )
        {
            Trace = new Trace();
            this.log = log;
            HostA = new TestNode( this, Host.A, log, hostAaddr);
            HostB = new TestNode(this, Host.B, log, hostBaddr);
            
        }



        public async Task PauseWithoutBackgroundActions(int ms)
        {
            log.Add(@"PauseWithoutBackgroundActions {0}", ms);
            await Task.Delay(ms);
        }

        public async Task Pause(int ms)
        {
            await Pause(ms, _ => false);
        }
        public async Task Pause(int ms, Func<TestCtx, bool> cancel )
        {
            log.Add(@"Pause {0}", ms);
            var isCancelled = false;
            const int pausingStepMs = 200;
            var stopwatch = Stopwatch.StartNew();

            while (stopwatch.ElapsedMilliseconds < ms)
            {
                await GetAndTraceStates();

                isCancelled = cancel.Invoke(this);
                if (isCancelled)
                {
                    break;
                }

                if (ms - stopwatch.ElapsedMilliseconds > pausingStepMs)
                {
                    await Task.Delay(pausingStepMs);
                }
                else
                {
                    break;
                }
            }

            if (!isCancelled)
            {
                var remainingMs = ms - stopwatch.ElapsedMilliseconds;
                if (remainingMs > 0)
                {
                    await Task.Delay((int)remainingMs);
                }
            }

            stopwatch.Stop();
        }

        private async Task GetAndTraceStates()
        {
            var results = await Task.WhenAll(HostA.GetState(), HostB.GetState());
            Trace.Add( new TraceItemChannel
            {
                Time = Stopwatch.GetTimestamp(),
                Host = Host.A,
                Channels = results[0].Channels
            });
            Trace.Add( new TraceItemChannel
            {
                Time = Stopwatch.GetTimestamp(),
                Host = Host.B,
                Channels = results[1].Channels
            });

            var cha = Trace.GetLastTraceItemChannelFor(Host.A, null)?.Channels[0];
            var chb = Trace.GetLastTraceItemChannelFor(Host.B, null)?.Channels[0];
            Console.WriteLine($@"Stats {cha?.RecvPostion} {chb?.RecvPostion}");
        }
        public async Task ConnectFrom(Host host)
        {
            log.Add(@"Connecting {0}", host);
            var localNode = host == Host.A ? HostA : HostB;
            var remoteNode = host == Host.B ? HostA : HostB;
            await localNode.Connect( remoteNode.HostName );
            await Task.Delay(200);

        }
    }
}
