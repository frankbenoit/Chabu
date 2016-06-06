using System;
using System.Threading.Tasks;
using org.chabu.test.director.gui;

namespace org.chabu.test.director.prot
{
    public class TestCtx
    {
        private readonly ILogContainer _log;
        public TestNode HostA { get; }
        public TestNode HostB { get; }

        public TestCtx(ILogContainer log, string hostAaddr, string hostBaddr )
        {
            _log = log;
            HostA = new TestNode( this, Host.A, log, hostAaddr);
            HostB = new TestNode(this, Host.B, log, hostBaddr);
        }

        public async Task Pause(int ms)
        {
            _log.Add(@"Pause {0}", ms);
            await Task.Delay(ms);
        }

        public async Task ConnectFrom(Host host)
        {
            _log.Add(@"Connecting {0}", host);
            var localNode = host == Host.A ? HostA : HostB;
            var remoteNode = host == Host.B ? HostA : HostB;
            await localNode.Connect( remoteNode.HostName );
            await Task.Delay(200);

        }
    }
}
