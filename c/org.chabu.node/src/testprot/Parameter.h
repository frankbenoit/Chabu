/*
 * Parameter.h
 *
 *  Created on: 17.07.2016
 *      Author: Frank
 */

#ifndef TESTPROT_PARAMETER_H_
#define TESTPROT_PARAMETER_H_

#include <vector>
#include <memory>
#include <string>
#include <stdexcept>
#include <pugixml.hpp>

namespace testprot {

class ParameterValue;
class ParameterWithChilds;

class Parameter {
public:
	std::string name{""};
	Parameter();
	Parameter( std::string name );
	virtual ~Parameter();
	virtual std::string toString() = 0;
	virtual bool isParameterValue() { return false; }
	virtual bool isParameterWithChilds() { return false; }
	virtual ParameterValue& asParameterValue() { throw std::runtime_error("wrong type"); }
	virtual ParameterWithChilds& asParameterWithChilds() { throw std::runtime_error("wrong type"); }
	virtual void encodeInto( pugi::xml_node node ) = 0;
};
std::string joinParameterListToString( const std::vector<std::shared_ptr<Parameter>>& list, std::string joinStr = ", " );
std::shared_ptr<Parameter> createParam( pugi::xml_node node );

} /* namespace testprot */

#endif /* TESTPROT_PARAMETER_H_ */
