/*
 * ParameterWithChilds.h
 *
 *  Created on: 17.07.2016
 *      Author: Frank
 */

#ifndef TESTPROT_PARAMETERWITHCHILDS_H_
#define TESTPROT_PARAMETERWITHCHILDS_H_

#include <vector>
#include <memory>
#include "Parameter.h"

namespace testprot {

class ParameterWithChilds : public Parameter {
public:
	ParameterWithChilds();
	virtual ~ParameterWithChilds();
	std::vector<std::shared_ptr<Parameter>> childs;
	void load( pugi::xml_node node );
	virtual std::string toString();
	virtual bool isParameterWithChilds() { return true; }
	virtual ParameterWithChilds& asParameterWithChilds() { return *this; }
	virtual void encodeInto( pugi::xml_node node );
};

} /* namespace testprot */

#endif /* TESTPROT_PARAMETERWITHCHILDS_H_ */
