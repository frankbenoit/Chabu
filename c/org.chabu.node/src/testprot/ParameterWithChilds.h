/*
 * ParameterWithChilds.h
 *
 *  Created on: 17.07.2016
 *      Author: Frank
 */

#ifndef TESTPROT_PARAMETERWITHCHILDS_H_
#define TESTPROT_PARAMETERWITHCHILDS_H_

#include <vector>
#include <string>
#include <memory>
#include "Parameter.h"

namespace testprot {

class ParameterWithChilds : public Parameter {
public:
	ParameterWithChilds();
	ParameterWithChilds(std::string name);
	virtual ~ParameterWithChilds();
	std::vector<std::shared_ptr<Parameter>> childs;
	void load( pugi::xml_node node );
	virtual std::string toString();
	virtual bool isParameterWithChilds() { return true; }
	virtual ParameterWithChilds& asParameterWithChilds() { return *this; }
	virtual void encodeInto( pugi::xml_node node );

	void addParameterValue( std::string name, int64_t value );
	void addParameter( std::shared_ptr<Parameter> child);
};

} /* namespace testprot */

#endif /* TESTPROT_PARAMETERWITHCHILDS_H_ */
