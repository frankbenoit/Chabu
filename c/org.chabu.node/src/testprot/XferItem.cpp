/*
 * XferItem.cpp
 *
 *  Created on: 17.07.2016
 *      Author: Frank
 */

#include "XferItem.h"
#include <boost/format.hpp>

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
} /* namespace testprot */
