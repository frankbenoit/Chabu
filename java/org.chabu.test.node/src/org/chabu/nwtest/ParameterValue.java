package org.chabu.nwtest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="ParameterValue")
public class ParameterValue extends Parameter {
	
	@XmlElement(name="Value")
	public String value;
	
	public ParameterValue(){
		
	}

	public ParameterValue(String name, String value){
		super(name);
		this.value = value;
	}
	public ParameterValue(String name, long value){
		super(name);
		this.value = Long.toString(value);
	}
	public ParameterValue(String name, double value){
		super(name);
		this.value = Double.toString(value);
	}
}