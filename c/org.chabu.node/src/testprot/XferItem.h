/*
 * XferItem.h
 *
 *  Created on: 17.07.2016
 *      Author: Frank
 */

#ifndef TESTPROT_XFERITEM_H_
#define TESTPROT_XFERITEM_H_

#include <memory>
#include <string>
#include <vector>
#include <pugixml.hpp>

#include "Parameter.h"

namespace testprot {

class ParameterValue;
class ParameterWithChilds;

enum class Category { REQ, RES, EVT };

class XferItem {
public:
	testprot::Category category = testprot::Category::EVT;
	std::string name{""};
	int callIndex = 0;
	std::vector<std::shared_ptr<Parameter>> parameters;


	XferItem();
	virtual ~XferItem();

	void load( pugi::xml_node node );
	void encodeInto( pugi::xml_node node );
	std::string toString();


	std::string getValueString(std::string path) ;
	int64_t getValueLong(std::string path) ;
	int32_t getValueInt(std::string path) ;
	double getValueDouble(std::string path) ;
	void addParameter(std::string name, std::string value) ;
	void addParameter(std::string name, int64_t value) ;
	void addParameter(std::string name, double value) ;
	void addParameter( std::shared_ptr<Parameter> child) ;
	void addParameters(std::vector<std::shared_ptr<Parameter>> parameters);
private:
	ParameterValue& findParameterValue(std::string path) ;
	Parameter* findParameterValue(std::vector<std::shared_ptr<Parameter>> par, std::vector<std::string> parts, int i) ;

};


} /* namespace testprot */

#endif /* TESTPROT_XFERITEM_H_ */
