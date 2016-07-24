/*
 * ParameterValue.h
 *
 *  Created on: 17.07.2016
 *      Author: Frank
 */

#ifndef TESTPROT_PARAMETERVALUE_H_
#define TESTPROT_PARAMETERVALUE_H_

#include "Parameter.h"

#include <pugixml.hpp>
#include <string>

namespace testprot {

class ParameterValue : public Parameter {
public:
	ParameterValue();
	ParameterValue(std::string name, std::string value);
	virtual ~ParameterValue();
	std::string value{""};
	void load( pugi::xml_node node );
	virtual std::string toString();
	virtual bool isParameterValue() { return true; }
	virtual ParameterValue& asParameterValue() { return *this; }
	virtual void encodeInto( pugi::xml_node node );
};

} /* namespace testprot */

#endif /* TESTPROT_PARAMETERVALUE_H_ */
