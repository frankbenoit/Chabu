/*
 * ParameterValue.cpp
 *
 *  Created on: 17.07.2016
 *      Author: Frank
 */

#include "ParameterValue.h"

#include <boost/format.hpp>

namespace testprot {

using std::string;
ParameterValue::ParameterValue()
{
}

ParameterValue::ParameterValue(std::string name, std::string value)
: Parameter( name )
, value ( value )
{
}

ParameterValue::~ParameterValue()
{
}

void ParameterValue::load( pugi::xml_node node ){
	name = node.child_value("Name");
	value = node.child_value("Value");
}
string ParameterValue::toString(){
	return (boost::format("ParameterValue{name=%s, value=%s}") % name % value ).str();
}

void ParameterValue::encodeInto( pugi::xml_node node ){
	auto item = node.append_child("Parameter");
	item.append_attribute("xsi:type").set_value("ParameterValue");
	item.append_child("Name").text().set(name.c_str());
	item.append_child("Value").text().set(value.c_str());
}
} /* namespace testprot */
