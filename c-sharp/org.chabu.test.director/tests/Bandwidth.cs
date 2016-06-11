using org.chabu.test.director.prot;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using StructureMap;

namespace org.chabu.test.director.tests
{
    public class Bandwidth : ITest
    {
        public string Name { get; }
        public string Description { get; }

        public Bandwidth()
        {
            Name = this.GetType().Name;
            Description = @"Test the bandwidth as an unidirectional and a bidirectional connection on a single channel";
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

            const int amount = 100 * 1024 * 1024;
            await hostB.ChannelRecv(0, amount);
            await hostA.ChannelXmit(0, amount);

            await ctx.Pause(5000);

            await Task.WhenAll(
                Close(hostA),
                Close(hostB));

        }
    }
}


