package org.chabu.nwtest;

//import static org.assertj.core.api.Assertions.*;

import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class XferItemTest {

	private Marshaller marshaller;
	private Unmarshaller unmarshaller;

	@Before
	public void setup() throws JAXBException {
		JAXBContext jaxbContext = JAXBContext.newInstance(XferItem.class, Parameter.class, ParameterValue.class, ParameterWithChilds.class);
		marshaller = jaxbContext.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		unmarshaller = jaxbContext.createUnmarshaller();
	}

	@Test
	public void decode() throws Exception {
		String xml = 
			"<?xml version=\"1.0\" encoding=\"utf-8\"?>\r\n" +
			"<XferItem xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\r\n" +
			"  <Category>REQ</Category>\r\n" +
			"  <Name>Frank</Name>\r\n" +
			"  <CallIndex>3</CallIndex>\r\n" +
			"  <Parameters>\r\n" +
			"    <Parameter xsi:type=\"ParameterValue\">\r\n" +
			"      <Name>MyCommand1</Name>\r\n" +
			"      <Value>text</Value>\r\n" +
			"    </Parameter>\r\n" +
			"    <Parameter xsi:type=\"ParameterValue\">\r\n" +
			"      <Name>MyCommand</Name>\r\n" +
			"      <Value>A Text\r\n" +
			"with line break and &lt;&gt; &lt;![CDATA[Inhalt]]&gt; xml elements\" </Value>\r\n" +
			"    </Parameter>\r\n" +
			"    <Parameter xsi:type=\"ParameterValue\">\r\n" +
			"      <Name>Par1</Name>\r\n" +
			"      <Value>42</Value>\r\n" +
			"    </Parameter>\r\n" +
			"    <Parameter xsi:type=\"ParameterValue\">\r\n" +
			"      <Name>Version</Name>\r\n" +
			"      <Value>68719476736</Value>\r\n" +
			"    </Parameter>\r\n" +
			"    <Parameter xsi:type=\"ParameterValue\">\r\n" +
			"      <Name>Ratio</Name>\r\n" +
			"      <Value>1.34</Value>\r\n" +
			"    </Parameter>\r\n" +
			"    <Parameter xsi:type=\"ParameterWithChilds\">\r\n" +
			"      <Name>channel-0</Name>\r\n" +
			"      <Childs>\r\n" +
			"        <Parameter xsi:type=\"ParameterValue\">\r\n" +
			"          <Name>xmitIdx</Name>\r\n" +
			"          <Value>1234</Value>\r\n" +
			"        </Parameter>\r\n" +
			"        <Parameter xsi:type=\"ParameterValue\">\r\n" +
			"          <Name>recvIdx</Name>\r\n" +
			"          <Value>1234</Value>\r\n" +
			"        </Parameter>\r\n" +
			"      </Childs>\r\n" +
			"    </Parameter>\r\n" +
			"    <Parameter xsi:type=\"ParameterWithChilds\">\r\n" +
			"      <Name>channel-1</Name>\r\n" +
			"      <Childs>\r\n" +
			"        <Parameter xsi:type=\"ParameterValue\">\r\n" +
			"          <Name>xmitIdx</Name>\r\n" +
			"          <Value>1234</Value>\r\n" +
			"        </Parameter>\r\n" +
			"        <Parameter xsi:type=\"ParameterValue\">\r\n" +
			"          <Name>recvIdx</Name>\r\n" +
			"          <Value>1234</Value>\r\n" +
			"        </Parameter>\r\n" +
			"      </Childs>\r\n" +
			"    </Parameter>\r\n" +
			"  </Parameters>\r\n" +
			"</XferItem>";
		XferItem xi = (XferItem)unmarshaller.unmarshal(new StringReader(xml));
		System.out.println(xi);
		Assert.assertEquals("Frank", xi.getName());
		Assert.assertEquals(XferItem.Category.REQ, xi.getCategory());
		Assert.assertEquals(3, xi.getCallIndex());
		Parameter[] p = xi.getParameters();
		Assert.assertEquals(7, p.length);
		Assert.assertEquals("text", xi.getValueString("MyCommand1"));
		Assert.assertEquals(1234, xi.getValueLong("channel-0/recvIdx"));
		
	}
	
	@Test
	public void encode() throws Exception {
		XferItem item = new XferItem();
		item.setCategory( XferItem.Category.EVT );
		item.setName("Frank");
		item.setCallIndex(3);
		item.setParameters( new Parameter[]{
				new ParameterValue("MyCommand1", "text"),
				new ParameterValue("MyCommand", "A Text\r\nwith line break and <> <![CDATA[Inhalt]]> xml elements\" "),
				new ParameterValue("Par1", 42),
				new ParameterValue("Version", 0x10_0000_0000L),
				new ParameterValue("Ratio", 1.34),
				new ParameterWithChilds("channel-0", new Parameter[]{
						new ParameterValue("Ratio", 1.34),
						
				})
		});
		StringWriter sw = new StringWriter();
		marshaller.marshal(item, sw);
		System.out.println(sw.toString());
		//assertThat(this).as("Not yet implemented").isNull();
	}
}
