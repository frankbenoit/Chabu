using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using StructureMap;

namespace org.chabu.test.director.prot
{
    class TestRunner 
    {
        private readonly ITest _test;
        private readonly TestCtx _ctx;
        private readonly string _hostA;
        private readonly string _hostB;

        public TestRunner(ITest test, TestCtx ctx, string hostA, string hostB )
        {
            _test = test;
            _ctx = ctx;
            _hostA = hostA;
            _hostB = hostB;
        }

        public async Task Run()
        {

            await Task.WhenAll(_ctx.HostA.ConnectCtrl(), _ctx.HostB.ConnectCtrl());

            await _test.Run( _ctx );

            _ctx.HostA.DisconnectCtrl();
            _ctx.HostB.DisconnectCtrl();
        }
    }


}
