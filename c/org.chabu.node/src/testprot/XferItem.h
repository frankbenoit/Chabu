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

enum class Category { REQ, RES, EVT };

class XferItem {
public:
	enum Category category = Category::EVT;
	std::string name{""};
	int callIndex = 0;
	std::vector<std::shared_ptr<Parameter>> parameters;


	XferItem();
	virtual ~XferItem();

	void load( pugi::xml_node node );
	std::string toString();
};


} /* namespace testprot */

#endif /* TESTPROT_XFERITEM_H_ */
