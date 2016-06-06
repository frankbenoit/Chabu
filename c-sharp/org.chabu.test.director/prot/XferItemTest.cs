using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using System.Xml.Serialization;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace org.chabu.test.director.prot
{
    [TestClass]
    public class XferItemTest
    {
        [TestMethod]
        public void Decode()
        {
            string xml = @"<?xml version=""1.0"" encoding=""UTF-16"" standalone=""yes"" ?>
<XferItem>
    <Category>RES</Category>
    <Name>Setup</Name>
    <CallIndex>0</CallIndex>
    <Parameters>
        <Parameter xsi:type=""ParameterValue"" xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance"" >
            <Name>IsError</Name>
            <Value>1</Value>
        </Parameter>
        <Parameter xsi:type=""ParameterValue"" xmlns:xsi=""http://www.w3.org/2001/XMLSchema-instance"">
            <Name>Message</Name>
            <Value>Unknown Command</Value>
        </Parameter>
    </Parameters>
</XferItem>";
            Console.WriteLine(xml);
            XmlSerializer ser = new XmlSerializer(typeof(XferItem));
            ser.Deserialize(GenerateStreamFromString(xml));
        }

        [TestMethod]
        public void EncodeDecode()
        {
            var cr = new XferItem
            {
                Name = "Frank",
                Category = Category.REQ,
                CallIndex = 3,
                Parameters = new Parameter[]
                {
                    new ParameterValue("MyCommand1", "text"),
                    new ParameterValue("MyCommand",
                        "A Text\r\nwith line break and <> <![CDATA[Inhalt]]> xml elements\" "),
                    new ParameterValue("Par1", 42),
                    new ParameterValue("Version", 0x1000000000),
                    new ParameterValue("Ratio", 1.34),
                    new ParameterWithChilds("channel-0", new Parameter[]
                    {
                        new ParameterValue("xmitIdx", 1234),
                        new ParameterValue("recvIdx", 1234),
                    }),
                    new ParameterWithChilds("channel-1", new Parameter[]
                    {
                        new ParameterValue("xmitIdx", 1234),
                        new ParameterValue("recvIdx", 1234),
                    }),
                }
            };
            var ser = new XmlSerializer(typeof(XferItem));
            var ms = new MemoryStream(10000);
            ser.Serialize(ms, cr);
            XferItem res;
            using (var textWriter = new StringWriter())
            {
                ser.Serialize(textWriter, cr);
                Console.WriteLine(textWriter.ToString());
                res = (XferItem) ser.Deserialize(GenerateStreamFromString(textWriter.ToString()));
            }

            Assert.AreEqual(Category.REQ, res.Category);
            Assert.AreEqual("Frank", res.Name);
            Assert.AreEqual(3, res.CallIndex);
            Assert.AreEqual("text", res.GetString("MyCommand1"));
            Assert.AreEqual(1234, res.GetInt("channel-0/xmitIdx"));

        }
        private MemoryStream GenerateStreamFromString(string value)
        {
            var buffer = Encoding.Unicode.GetBytes(value ?? "");
            return new MemoryStream(buffer);
        }
    }
}
