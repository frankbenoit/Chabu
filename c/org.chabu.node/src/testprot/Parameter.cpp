/*
 * Parameter.cpp
 *
 *  Created on: 17.07.2016
 *      Author: Frank
 */

#include "Parameter.h"
#include "ParameterValue.h"
#include "ParameterWithChilds.h"

#include <pugixml.hpp>
#include <memory>
#include <vector>
#include <utility>
#include <iostream>

namespace testprot {

using std::shared_ptr;
using std::vector;
using std::string;

Parameter::Parameter() {
}

Parameter::~Parameter() {
}

std::shared_ptr<Parameter> createParam( pugi::xml_node node ){
	string type = node.attribute("xsi:type").value();
	if( type == "ParameterValue"){
		auto res = new ParameterValue();
		res->load(node);
		return shared_ptr<Parameter>{ res };
	}
	else if( type == "ParameterWithChilds"){
		auto res = new ParameterWithChilds();
		res->load(node);
		return shared_ptr<Parameter>{ res };
	}
	else {
		throw std::runtime_error("unknown xml element, where a Parameter subtype was expected.");
	}
}

string joinParameterListToString( const vector<shared_ptr<Parameter>>& list, string joinStr ){
	string childsStr;
	for (auto it = list.begin(); it != list.end(); it++) {
		if (!childsStr.empty()){
			childsStr.append(joinStr);
		}
		childsStr.append((*it)->toString());
	}
	return childsStr;
}

} /* namespace testprot */
