/*
 * ChabuTest.hpp
 *
 *  Created on: 29.06.2016
 *      Author: Frank
 */

#ifndef CHABUTEST_HPP_
#define CHABUTEST_HPP_

#include "gtest/gtest.h"

class ChabuTest : public ::testing::Test{
public:
//	ChabuTest();
//	virtual ~ChabuTest();

	virtual void SetUp() = 0;
//	virtual void TearDown() = 0;

};

#endif /* CHABUTEST_HPP_ */
