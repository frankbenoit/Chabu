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
	virtual ~ParameterValue();
	std::string name{""};
	std::string value{""};
	void load( pugi::xml_node node );
	virtual std::string toString();
};

} /* namespace testprot */

#endif /* TESTPROT_PARAMETERVALUE_H_ */
