using System.Threading.Tasks;

namespace org.chabu.test.director.prot
{
    public class TestRunner 
    {
        private readonly ITest test;
        private readonly TestCtx ctx;

        public TestRunner(ITest test, TestCtx ctx )
        {
            this.test = test;
            this.ctx = ctx;
        }

        public async Task Run()
        {

            await Task.WhenAll(ctx.HostA.ConnectCtrl(), ctx.HostB.ConnectCtrl());

            await test.Run( ctx );

            ctx.HostA.DisconnectCtrl();
            ctx.HostB.DisconnectCtrl();
        }

        public Trace GetTrace()
        {
            return ctx.Trace;
        }
    }


}
