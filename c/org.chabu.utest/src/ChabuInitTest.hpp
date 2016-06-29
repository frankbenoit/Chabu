/*
 * ChabuInitTest.hpp
 *
 *  Created on: 29.06.2016
 *      Author: Frank
 */

#ifndef CHABUINITTEST_HPP_
#define CHABUINITTEST_HPP_

#include "ChabuTest.hpp"

class ChabuInitTest : public ChabuTest {
public:
	ChabuInitTest();
	virtual ~ChabuInitTest();
	virtual void SetUp();
	virtual void TearDown();
};

#endif /* CHABUINITTEST_HPP_ */
