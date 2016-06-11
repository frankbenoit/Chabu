package org.chabu.nwtest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

public abstract class Parameter {
	
	@XmlElement(name="Name")
	private String name = "";
	
	public Parameter() {
	}

	public Parameter(String name) {
		this.name = name;
	}
	
	@XmlTransient
	public String getName() {
		return name;
	}
}