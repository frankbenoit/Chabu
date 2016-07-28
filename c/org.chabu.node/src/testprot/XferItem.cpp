/*
 * XferItem.cpp
 *
 *  Created on: 17.07.2016
 *      Author: Frank
 */

#include "XferItem.h"
#include <boost/format.hpp>
#include <boost/lambda/lambda.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/algorithm/string.hpp>

#include "ParameterValue.h"
#include "ParameterWithChilds.h"

namespace testprot {

using std::string;
using std::string;

XferItem::XferItem() {
}

XferItem::~XferItem() {
}

void XferItem::load( pugi::xml_node node ){
	this->name      = node.child_value("Name");
	this->callIndex = std::atoi( node.child_value("CallIndex") );
	string enumName = node.child_value("Category");
	if( enumName == "REQ") this->category = Category::REQ;
	if( enumName == "RES") this->category = Category::RES;
	if( enumName == "EVT") this->category = Category::EVT;
	auto params = node.child("Parameters");
	if( params ){
		for (auto param = params.child("Parameter"); param; param = param.next_sibling("Parameter")){
			parameters.push_back( createParam(param));
		}
	}
}

void XferItem::encodeInto( pugi::xml_node node ){
	auto item = node.append_child("XferItem");
	item.append_child("Name").text().set(name.c_str());
	item.append_attribute("xmlns:xsi").set_value("http://www.w3.org/2001/XMLSchema-instance");
	item.append_attribute("xmlns:xsd").set_value("http://www.w3.org/2001/XMLSchema");

	{
		auto callIndexStr = boost::lexical_cast<std::string>(callIndex);
		item.append_child("CallIndex").text().set( callIndexStr.c_str() );
	}

	switch( this->category ){
	case Category::REQ:
		item.append_child("Category").text().set("REQ");
		break;
	case Category::RES:
		item.append_child("Category").text().set("RES");
		break;
	case Category::EVT:
		item.append_child("Category").text().set("EVT");
		break;
	}

	auto params = item.append_child("Parameters");
	for( auto it = parameters.begin(); it != parameters.end(); it++ ){
		auto p = *it;
		p->encodeInto(params);
	}
}

string XferItem::toString(){
	string parametersStr = joinParameterListToString( parameters, ",\n" );
	string categoryStr;
	switch( category ){
	case Category::REQ: categoryStr = "REQ"; break;
	case Category::RES: categoryStr = "RES"; break;
	case Category::EVT: categoryStr = "EVT"; break;
	}
	auto fmt = boost::format("XferItem{name=%s callIndex=%s category=%s parameters={%s}}") % name % callIndex % categoryStr % parametersStr;
	return fmt.str();
}


std::string XferItem::getValueString(std::string path) {
	ParameterValue pv = findParameterValue( path );
	return pv.value;
}
int64_t XferItem::getValueLong(std::string path) {
	ParameterValue pv = findParameterValue( path );
	return boost::lexical_cast<int64_t>(pv.value);
}
int32_t XferItem::getValueInt(std::string path) {
	ParameterValue pv = findParameterValue( path );
	return boost::lexical_cast<int32_t>(pv.value);
}
double XferItem::getValueDouble(std::string path) {
	ParameterValue pv = findParameterValue( path );
	return boost::lexical_cast<double>(pv.value);
}

ParameterValue& XferItem::findParameterValue(std::string path) {
	std::vector<std::string> parts;
	boost::split( parts, path, boost::lambda::_1 == '/');
	Parameter* p = findParameterValue(parameters, parts, 0);
	if( p && p->isParameterValue()){
		return p->asParameterValue();
	}
	throw std::runtime_error("Not found: "+path);
}
Parameter* XferItem::findParameterValue(std::vector<std::shared_ptr<Parameter>> par, std::vector<std::string> parts, int i) {
	std::string pathPart = parts[i];
	for( auto it = par.begin(); it != par.end(); it++ ){
		Parameter& p = **it;
		if( pathPart == p.name){
			if( p.isParameterWithChilds() ){
				ParameterWithChilds& pc = p.asParameterWithChilds();
				return findParameterValue(pc.childs, parts, i+1);
			}
			return &p;
		}
	}
	return nullptr;
}

void XferItem::addParameter(std::string name, std::string value) {
	auto p = std::shared_ptr<Parameter>{ new ParameterValue( name, value ) };
	parameters.push_back( p );
}
void XferItem::addParameter(std::string name, int64_t value) {
	auto p = std::shared_ptr<Parameter>{ new ParameterValue( name, boost::lexical_cast<std::string>(value) ) };
	parameters.push_back( p );
}
void XferItem::addParameter(std::string name, double value) {
	auto p = std::shared_ptr<Parameter>{ new ParameterValue( name, boost::lexical_cast<std::string>(value) ) };
	parameters.push_back( p );
}
void XferItem::addParameter( std::shared_ptr<Parameter> child) {
	parameters.push_back( child );
}
void XferItem::addParameters(std::vector<std::shared_ptr<Parameter>> parameters){
	for( auto it = parameters.begin(); it != parameters.end(); it++ ){
		this->parameters.push_back( *it );
	}
}

} /* namespace testprot */
