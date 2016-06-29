/*
 * AddClassTest.cpp
 *
 *  Created on: 25.06.2016
 *      Author: fbenoit1
 */




#include "gmock/gmock.h"
#include "AddClass.h"

TEST(AddClassTest, CanAddTwoNumbers)
{
  //Setup
  AddClass myAddClass;

  //Exercise
  int sum = myAddClass.add(1,2);

  //Verify
  ASSERT_EQ(3, sum);
}
