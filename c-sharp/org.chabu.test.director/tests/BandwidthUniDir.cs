using org.chabu.test.director.prot;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using StructureMap;

namespace org.chabu.test.director.tests
{
    public class BandwidthUniDir : ITest
    {
        private readonly int amount;
        public string Name { get; }
        public string Description { get; }

        public BandwidthUniDir( int amount )
        {
            this.amount = amount;
            Name = this.GetType().Name + $@" {Utils.GetSizeAsString(amount)}";
            Description = @"Test the bandwidth as an unidirectional connection on a single channel";
        }

        public async Task Setup(TestNode tn)
        {
            var res = await tn.Setup(Constants.DirectorVersion);
            Console.WriteLine($@"Setup: {res.ChabuProtocolVersion} {res.Implemenation}");

            await tn.BuilderStart(0x123, "BW", 10020, 3);
            await tn.BuilderAddChannel(0, 2);
            await tn.BuilderBuild();
        }

        public async Task Close(TestNode tn)
        {
            await tn.ChabuClose();
        }

        public async Task Run(TestCtx ctx)
        {
            var hostA = ctx.HostA;
            var hostB = ctx.HostB;

            await Task.WhenAll(
                Setup(hostA),
                Setup(hostB));

            await ctx.ConnectFrom(Host.A);

            await hostB.ChannelRecv(0, amount);
            await hostA.ChannelXmit(0, amount);

            var durationMs = await ctx.Pause(50000, _ => _.Trace.GetLastTraceItemChannelFor(Host.B, null)?.Channels[0]?.RecvPosition == amount );
            var xmitted = ctx.Trace.GetLastTraceItemChannelFor(Host.B, null).Channels[0].RecvPosition;
            var speedKbps = xmitted / durationMs * 1000;
            var speedStr = Utils.GetSizeAsString((long)speedKbps);
            var amountStr = Utils.GetSizeAsString(xmitted
                );
            ctx.Log($@"Overall duration {durationMs} ms, amount {amountStr}, speed: {speedStr}/s");
            await ctx.HostB.ExpectClose();
            await ctx.HostA.Close();
            await ctx.Pause(100);
            await ctx.HostB.EnsureClosed();

        }


    }
    
}


