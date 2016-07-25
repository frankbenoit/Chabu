/*
 * ParameterWithChilds.cpp
 *
 *  Created on: 17.07.2016
 *      Author: Frank
 */

#include "ParameterWithChilds.h"
#include <boost/format.hpp>

namespace testprot {

using std::string;
ParameterWithChilds::ParameterWithChilds()
{
	// TODO Auto-generated constructor stub

}

ParameterWithChilds::~ParameterWithChilds()
{
	// TODO Auto-generated destructor stub
}
void ParameterWithChilds::load( pugi::xml_node node ){
	name = node.child_value("Name");
	auto params = node.child("Childs");
	if( params ){
		for (auto param = params.child("Parameter"); param; param = param.next_sibling("Parameter")){
			childs.push_back( createParam(param));
		}
	}

}

string ParameterWithChilds::toString(){
	string childsStr = joinParameterListToString( childs );
	return (boost::format("ParameterWithChilds{name=%s childs={%s}}") % name % childsStr ).str();
}

void ParameterWithChilds::encodeInto( pugi::xml_node node ){
	auto item = node.append_child("Parameter");
	item.append_attribute("xsi:type").set_value("ParameterWithChilds");
	item.append_child("Name").text().set(name.c_str());
	auto childItem = item.append_child("Childs");
	for( auto it = childs.begin(); it != childs.end(); it++ ){
		auto c = *it;
		c->encodeInto( childItem );
	}
}


} /* namespace testprot */
