package org.chabu.nwtest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;

@XmlType(name="ParameterWithChilds")
public class ParameterWithChilds extends Parameter {
	
	public ParameterWithChilds() {
	}
	public ParameterWithChilds(String name, Parameter[] childs) {
		super(name);
		this.parameters = childs;
	}

	@XmlElementWrapper(name="Childs")
	@XmlElement(name="Parameter")
	private Parameter[] parameters = new Parameter[0];

	
	@XmlTransient
	public Parameter[] getChilds(){
		return parameters;
	}
}