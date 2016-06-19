using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading.Tasks;
using org.chabu.test.director.gui;

namespace org.chabu.test.director.prot
{

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
        public async Task<double> Pause(int ms, Func<TestCtx, bool> cancel )
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
            return stopwatch.ElapsedMilliseconds;
        }

        private void AddTraceItem(Host host, TestNode.GetStateResult result)
        {
            var item = new TraceItemChannel
            {
                Time = Stopwatch.GetTimestamp(),
                Host = host
                //Channels = results[0].Channels
            };
            var channelId = 0;
            foreach (var ch in result.Channels)
            {
                item.Channels.Add(new ChannelState
                {
                    ChannelId = channelId,
                    RecvPosition = ch.RecvPostion,
                    XmitPosition = ch.XmitPosition,
                    RecvLimit = ch.RecvLimit,
                    XmitLimit = ch.XmitLimit,
                });
                channelId++;
            }
            Trace.Add(item);

        }
        private async Task GetAndTraceStates()
        {
            var results = await Task.WhenAll(HostA.GetState(), HostB.GetState());
            AddTraceItem( Host.A, results[0] );
            AddTraceItem( Host.B, results[1] );
            var cha = Trace.GetLastTraceItemChannelFor(Host.A, null)?.Channels[0];
            var chb = Trace.GetLastTraceItemChannelFor(Host.B, null)?.Channels[0];
            Console.WriteLine($@"Stats {cha?.RecvPosition} {chb?.RecvPosition}");
        }
        public async Task ConnectFrom(Host host)
        {
            log.Add(@"Connecting {0}", host);
            var localNode = host == Host.A ? HostA : HostB;
            var remoteNode = host == Host.B ? HostA : HostB;
            await localNode.Connect( remoteNode.HostName );
            await Task.Delay(200);

        }

        public void Log(string s)
        {
            log.Add(@"{0}", s);
        }
    }
}
