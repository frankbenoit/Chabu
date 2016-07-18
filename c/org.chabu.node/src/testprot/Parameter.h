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
#include <pugixml.hpp>

namespace testprot {

class Parameter {
public:
	Parameter();
	virtual ~Parameter();
	virtual std::string toString() = 0;
};
std::string joinParameterListToString( const std::vector<std::shared_ptr<Parameter>>& list, std::string joinStr = ", " );
std::shared_ptr<Parameter> createParam( pugi::xml_node node );

} /* namespace testprot */

#endif /* TESTPROT_PARAMETER_H_ */
