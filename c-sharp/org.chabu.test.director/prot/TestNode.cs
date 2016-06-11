using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using org.chabu.test.director.gui;

namespace org.chabu.test.director.prot
{
    public enum Host
    {
        A, B
    }

    public enum Implemenation
    {
        // ReSharper disable once InconsistentNaming
        Java, CSharp, C, VHDL
    }

    public struct SetupResult
    {
        public Implemenation Implemenation;
        public string ChabuProtocolVersion;
    }

    public class TestNode
    {
        public string HostName { get; set; }
        private readonly TestCtx _testCtx;
        private readonly Host _host;
        private readonly ILogContainer _log;
        public NwClient Network { get; set; }

        public TestNode(TestCtx testCtx, Host host, ILogContainer log, string hostName)
        {
            Network = new NwClient();
            HostName = hostName;
            _testCtx = testCtx;
            this._host = host;
            _log = log;
        }

        public async Task<SetupResult> Setup(string directorVersion)
        {
            _log.Add($@"[{_host}] Setup {directorVersion}");

            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "Setup",
                Parameters = new Parameter[]
                {
                    new ParameterValue("ChabuTestDirectorVersion", Constants.DirectorVersion),
                    new ParameterValue("NodeLabel", _host.ToString()),
                }
            };

            var rs = await Network.SendRequestRetrieveResponse(rq);

            ;
            SetupResult res;
            res.ChabuProtocolVersion = rs.GetString("ChabuProtocolVersion");
            res.Implemenation = (Implemenation) Enum.Parse(typeof(Implemenation), rs.GetString("Implementation"));
            return res;
        }

        public async Task BuilderStart(uint applicationVersion, string applicationProtocolName, int recvPacketSize, int priorityCount)
        {
            _log.Add($@"[{_host}] BuilderStart {applicationVersion} {applicationProtocolName} {priorityCount}");
            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "ChabuBuilder.start",
                Parameters = new Parameter[]
                {
                    new ParameterValue("ApplicationVersion", applicationVersion),
                    new ParameterValue("ApplicationProtocolName", applicationProtocolName),
                    new ParameterValue("RecvPacketSize", recvPacketSize),
                    new ParameterValue("PriorityCount", priorityCount),
                }
            };

            await Network.SendRequestRetrieveResponse(rq);
        }

        public async Task BuilderAddChannel(int channelId, int priority)
        {
            _log.Add($@"[{_host}] BuilderAddChannel {channelId} {priority}");
            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "ChabuBuilder.addChannel",
                Parameters = new Parameter[]
                {
                    new ParameterValue("Channel", channelId),
                    new ParameterValue("Priority", priority)
                }
            };

            await Network.SendRequestRetrieveResponse(rq);
        }

        public async Task BuilderBuild()
        {
            _log.Add($@"[{_host}] BuilderBuild" );
            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "ChabuBuilder.build",
            };

            await Network.SendRequestRetrieveResponse(rq);

        }

        public async Task ChabuClose()
        {
            _log.Add($@"[{_host}] ChabuClose");
            _log.Add($@"[{_host}] BuilderBuild");
            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "Chabu.close",
            };

            await Network.SendRequestRetrieveResponse(rq);
        }

        public async Task ChannelXmit(int channel, int amount)
        {
            _log.Add($@"[{_host}] ChannelXmit {channel} {amount}");
            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "Channel.xmit",
                Parameters = new Parameter[]
                {
                    new ParameterValue("Channel", channel),
                    new ParameterValue("Amount", amount)
                }
            };

            await Network.SendRequestRetrieveResponse(rq);
        }

        public async Task ChannelRecv(int channel, int amount)
        {
            _log.Add($@"[{_host}] ChannelRecv {channel} {amount}");
            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "Channel.recv",
                Parameters = new Parameter[]
                {
                    new ParameterValue("Channel", channel),
                    new ParameterValue("Amount", amount)
                }
            };

            await Network.SendRequestRetrieveResponse(rq);
        }

        public class GetStateResult_Channel
        {
            public long recvLimit;
            public long recvPostion;
            public long xmitLimit;
            public long xmitPosition;
        }
        public class GetStateResult
        {
            public List<GetStateResult_Channel> Channels;
        }
        public async Task<GetStateResult> GetState()
        {
            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "GetState",
                Parameters = new Parameter[0]
            };

            XferItem rs = await Network.SendRequestRetrieveResponse(rq);
            GetStateResult result = new GetStateResult();
            var channelCount = rs.GetInt("channelCount");
            result.Channels = new List<GetStateResult_Channel>( channelCount);
            for (int i = 0; i < channelCount; i++)
            {
                result.Channels[i] = new GetStateResult_Channel
                {
                    recvLimit = rs.GetInt($@"channels/{i}/recvLimit"),
                    recvPostion = rs.GetInt($@"channels/{i}/recvPostion"),
                    xmitLimit = rs.GetInt($@"channels/{i}/xmitLimit"),
                    xmitPosition  = rs.GetInt($@"channels/{i}/xmitPosition")
                };
                

            }
            return result;
        }
        public async Task ChannelReset(int channel)
        {
            _log.Add($@"[{_host}] ChannelReset {channel} not yet implemented!!!" );
            await Task.Delay(20);

        }
        public async Task<int> Ping()
        {
            _log.Add($@"[{_host}] Ping  not yet implemented!!!" );
            await Task.Delay(20);
            return 20;
        }

        public async Task ConnectCtrl()
        {
            await Network.ConnectAsync(HostName);

        }
        public async Task Connect(string hostName )
        {
            var idx = hostName.LastIndexOf(':');

            var hostAddr = hostName.Substring(0, idx);

            var portForCtrl = Convert.ToInt32( hostName.Substring(idx+1));
            var portForChabu = portForCtrl + 1;

            _log.Add($@"[{_host}] Connect" );
            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "Connect",
                Parameters = new Parameter[]
                {
                    new ParameterValue("HostName", hostAddr),
                    new ParameterValue("Port", portForChabu)
                }
            };

            await Network.SendRequestRetrieveResponse(rq);

        }
    }
}
