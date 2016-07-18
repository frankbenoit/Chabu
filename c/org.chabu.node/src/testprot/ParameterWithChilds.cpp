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

} /* namespace testprot */
