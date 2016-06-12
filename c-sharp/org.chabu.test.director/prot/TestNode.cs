using System;
using System.Collections.Generic;
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
        private readonly TestCtx testCtx;
        public Host Host { get; }
        private readonly ILogContainer log;
        public NwClient Network { get; set; }

        public TestNode(TestCtx testCtx, Host host, ILogContainer log, string hostName)
        {
            Network = new NwClient();
            HostName = hostName;
            this.testCtx = testCtx;
            Host = host;
            this.log = log;
        }

        public async Task<SetupResult> Setup(string directorVersion)
        {
            log.Add($@"[{Host}] Setup {directorVersion}");

            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "Setup",
                Parameters = new Parameter[]
                {
                    new ParameterValue("ChabuTestDirectorVersion", Constants.DirectorVersion),
                    new ParameterValue("NodeLabel", Host.ToString()),
                }
            };

            var rs = await XferAndCheckError(rq);

            
            SetupResult res;
            res.ChabuProtocolVersion = rs.GetString("ChabuProtocolVersion");
            res.Implemenation = (Implemenation) Enum.Parse(typeof(Implemenation), rs.GetString("Implementation"));
            return res;
        }

        public async Task BuilderStart(uint applicationVersion, string applicationProtocolName, int recvPacketSize, int priorityCount)
        {
            log.Add($@"[{Host}] BuilderStart {applicationVersion} {applicationProtocolName} {priorityCount}");
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

            await XferAndCheckError(rq);
        }

        public async Task BuilderAddChannel(int channelId, int priority)
        {
            log.Add($@"[{Host}] BuilderAddChannel {channelId} {priority}");
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

            await XferAndCheckError(rq);
        }

        public async Task BuilderBuild()
        {
            log.Add($@"[{Host}] BuilderBuild" );
            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "ChabuBuilder.build",
            };

            await XferAndCheckError(rq);
        }

        public async Task ChabuClose()
        {
            log.Add($@"[{Host}] Chabu.close");
            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "Chabu.close",
            };

            await XferAndCheckError(rq);
        }

        public async Task Close()
        {
            log.Add($@"[{Host}] Close");
            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "Close",
            };

            await XferAndCheckError(rq);
        }

        public async Task ExpectClose()
        {
            log.Add($@"[{Host}] ExpectClose");
            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "ExpectClose",
            };

            await XferAndCheckError(rq);
        }

        public async Task EnsureClosed()
        {
            log.Add($@"[{Host}] EnsureClosed");
            var rq = new XferItem
            {
                Category = Category.REQ,
                CallIndex = 0,
                Name = "EnsureClosed",
            };

            await XferAndCheckError(rq);
        }

        public async Task ChannelXmit(int channel, int amount)
        {
            log.Add($@"[{Host}] ChannelXmit {channel} {amount}");
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

            await XferAndCheckError(rq);
        }

        public async Task ChannelRecv(int channel, int amount)
        {
            log.Add($@"[{Host}] ChannelRecv {channel} {amount}");
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

            await XferAndCheckError(rq);
        }

        private async Task<XferItem> XferAndCheckError(XferItem rq)
        {
            var result = await Network.SendRequestRetrieveResponse(rq);
            if (result.IsError())
            {
                Console.WriteLine($@"Xfer error: {rq.Name} -> {result.GetErrorMessage()}");
            }
            return result;
        }


        public class GetStateResultChannel
        {
            public long RecvLimit;
            public long RecvPostion;
            public long XmitLimit;
            public long XmitPosition;
        }
        public class GetStateResult
        {
            public List<GetStateResultChannel> Channels;
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

            var rs = await XferAndCheckError(rq);
            var result = new GetStateResult();
            var channelCount = rs.GetInt("channelCount");
            result.Channels = new List<GetStateResultChannel>( channelCount);
            for (var i = 0; i < channelCount; i++)
            {
                result.Channels.Add( new GetStateResultChannel
                {
                    RecvLimit = rs.GetInt($@"channel/{i}/recvLimit"),
                    RecvPostion = rs.GetInt($@"channel/{i}/recvPosition"),
                    XmitLimit = rs.GetInt($@"channel/{i}/xmitLimit"),
                    XmitPosition  = rs.GetInt($@"channel/{i}/xmitPosition")
                });
            }
            return result;
        }
        public async Task ChannelReset(int channel)
        {
            log.Add($@"[{Host}] ChannelReset {channel} not yet implemented!!!" );
            await Task.Delay(20);

        }
        public async Task<int> Ping()
        {
            log.Add($@"[{Host}] Ping  not yet implemented!!!" );
            await Task.Delay(20);
            return 20;
        }

        public async Task ConnectCtrl()
        {
            await Network.ConnectAsync(HostName);

        }
        public void DisconnectCtrl()
        {
            Network.Disconnect();

        }
        public async Task Connect(string hostName )
        {
            var idx = hostName.LastIndexOf(':');

            var hostAddr = hostName.Substring(0, idx);

            var portForCtrl = Convert.ToInt32( hostName.Substring(idx+1));
            var portForChabu = portForCtrl + 1;

            log.Add($@"[{Host}] Connect" );
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

            await XferAndCheckError(rq);

        }
    }
}
