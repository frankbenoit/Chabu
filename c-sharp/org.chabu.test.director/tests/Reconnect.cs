using org.chabu.test.director.prot;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using StructureMap;

namespace org.chabu.test.director.tests
{
    public class Reconnect : ITest
    {
        public string Name { get; }
        public string Description { get; }

        public Reconnect()
        {
            Name = this.GetType().Name;
            Description = @"Several connect/close cycles";
        }

        public async Task Setup(TestNode tn)
        {
            var res = await tn.Setup(Constants.DirectorVersion);
            Console.WriteLine($@"Setup: {tn.Host} {res.ChabuProtocolVersion} {res.Implemenation}");

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
            await ctx.Pause(100);
            await ctx.HostB.ExpectClose();
            await ctx.HostA.Close();
            await ctx.Pause(300);
            await ctx.HostB.EnsureClosed();


        }
    }
}


