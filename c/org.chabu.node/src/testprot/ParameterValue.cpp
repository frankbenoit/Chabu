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
	// TODO Auto-generated constructor stub

}

ParameterValue::~ParameterValue()
{
	// TODO Auto-generated destructor stub
}
void ParameterValue::load( pugi::xml_node node ){
	name = node.child_value("Name");
	value = node.child_value("Value");
}
string ParameterValue::toString(){
	return (boost::format("ParameterValue{name=%s, value=%s}") % name % value ).str();
}

} /* namespace testprot */
