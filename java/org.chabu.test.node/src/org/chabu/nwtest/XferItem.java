package org.chabu.nwtest;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

@XmlRootElement(name="XferItem")
public class XferItem {
	
	public enum Category{
		REQ, RES, EVT;
	}
	
	@XmlElement(name="Category")
	private Category category = Category.EVT;
	@XmlElement(name="Name")
	private String name = "";
	@XmlElement(name="CallIndex")
	private int callIndex = 0;

	@XmlElementWrapper(name="Parameters")
	@XmlElement(name="Parameter")
	private Parameter[] parameters = new Parameter[0];
	
	public XferItem(){
		
	}
	
	public void setCategory(Category cat) {
		this.category = cat;
	}

	@XmlTransient
	public Category getCategory() {
		return category;
	}
	public void setName(String name) {
		this.name = name;
	}
	@XmlTransient
	public String getName() {
		return name;
	}
	
	public void setCallIndex(int callIndex) {
		this.callIndex = callIndex;
	}
	@XmlTransient
	public int getCallIndex() {
		return callIndex;
	}
	public void setParameters(Parameter[] parameters) {
		this.parameters = parameters;
	}
	@XmlTransient
	public Parameter[] getParameters() {
		return parameters;
	}

	public String getValueString(String path) {
		ParameterValue pv = findParameterValue( path );
		return pv.value;
	}
	public long getValueLong(String path) {
		ParameterValue pv = findParameterValue( path );
		return Long.parseLong(pv.value);
	}
	public int getValueInt(String path) {
		ParameterValue pv = findParameterValue( path );
		return Integer.parseInt(pv.value);
	}
	public double getValueDouble(String path) {
		ParameterValue pv = findParameterValue( path );
		return Double.parseDouble(pv.value);
	}

	private ParameterValue findParameterValue(String path) {
		String[] parts = path.split("/");
		Parameter p = findParameterValue(parameters, parts, 0);
		if( p instanceof ParameterValue ){
			return (ParameterValue)p;
		}
		throw new RuntimeException("Not found: "+path);
	}

	private Parameter findParameterValue(Parameter[] par, String[] parts, int i) {
		String pathPart = parts[i];
		for( Parameter p : par ){
			if( pathPart.equals(p.getName())){
				if( p instanceof ParameterWithChilds ){
					ParameterWithChilds pc = (ParameterWithChilds)p;
					return findParameterValue(pc.getChilds(), parts, i+1);
				}
				return p;
			}
		}
		return null;
	}

	public void addParameter(String name, String value) {
		parameters = Arrays.copyOf(parameters, parameters.length+1);
		parameters[ parameters.length - 1 ] = new ParameterValue( name, value );
	}
	public void addParameter(String name, long value) {
		parameters = Arrays.copyOf(parameters, parameters.length+1);
		parameters[ parameters.length - 1 ] = new ParameterValue( name, value );
	}
	public void addParameter(String name, double value) {
		parameters = Arrays.copyOf(parameters, parameters.length+1);
		parameters[ parameters.length - 1 ] = new ParameterValue( name, value );
	}
	public void addParameter(String name, ParameterWithChilds value ) {
		parameters = Arrays.copyOf(parameters, parameters.length+1);
		parameters[ parameters.length - 1 ] = value;
	}

}
