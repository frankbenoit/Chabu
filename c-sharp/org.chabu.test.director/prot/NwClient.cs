using System;
using System.Diagnostics.Contracts;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading.Tasks;
using System.Xml.Serialization;

namespace org.chabu.test.director.prot
{
    public delegate void StatusEventHandler(object sender, XferItem statusEvent);
    public class NwClient
    {
        private TcpClient tcpClient;
        private NetworkStream stream;

        public NwClient()
        {
        }


        public async Task ConnectAsync(string host)
        {
            tcpClient = new TcpClient();
            var idx = host.LastIndexOf(':');
            Contract.Assert( idx > 0 );
            var hostPart = host.Substring(0, idx);
            var port =Convert.ToInt32(host.Substring(idx+1));
            Console.WriteLine($@"connecting to {hostPart}:{port}");
            await tcpClient.ConnectAsync(hostPart, port );
            stream = tcpClient.GetStream();
        }

        public void Disconnect()
        {
            stream.Close();
            tcpClient.Close();
            stream = null;
            tcpClient = null;
        }
        private MemoryStream GenerateStreamFromString(string value)
        {
            return new MemoryStream(Encoding.UTF8.GetBytes(value ?? ""));
        }
        public async Task<XferItem> SendRequestRetrieveResponse(XferItem req)
        {
            var ser = new XmlSerializer(typeof(XferItem));
            var ms = new MemoryStream(10000);
            ser.Serialize(ms, req);
            var reqStr = Encoding.UTF8.GetString(ms.ToArray());
            //Console.WriteLine(@"sending: {0}", reqStr);
            var respStr = await SendRequestRetrieveResponse(reqStr);
            //Console.WriteLine(@"received: {0}", respStr);
            var resp = (XferItem)ser.Deserialize(GenerateStreamFromString(respStr));
            return resp;
        }

        private async Task<string> SendRequestRetrieveResponse(string text)
        {
            await SendText(text);
            var response = await ReceiveText();
            return response;
        }

        private async Task SendText(string text)
        {
            var size = Encoding.UTF8.GetByteCount(text);
            var bytes = new byte[size+4];
            bytes[0] = (byte)(size >> 24);
            bytes[1] = (byte)(size >> 16);
            bytes[2] = (byte)(size >> 8);
            bytes[3] = (byte)size;
            Encoding.UTF8.GetBytes(text, 0, text.Length, bytes, 4 );
            await stream.WriteAsync(bytes, 0, bytes.Length);
        }

        private async Task<string> ReceiveText()
        {
            var sizeBuffer = new byte[4];
            await stream.ReadAsync(sizeBuffer, 0, sizeBuffer.Length);
            var size = IPAddress.NetworkToHostOrder(BitConverter.ToInt32(sizeBuffer, 0));

            var responseBuffer = new byte[size];
            await stream.ReadAsync(responseBuffer, 0, responseBuffer.Length);
            var responseText = Encoding.UTF8.GetString(responseBuffer);

            return responseText;
        }
    }
}
