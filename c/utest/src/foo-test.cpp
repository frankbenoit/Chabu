/*
 * foo-test.cpp
 *
 *  Created on: 25.06.2016
 *      Author: fbenoit1
 */

//#include "gmock/gmock.h"
#include "gtest/gtest.h"
#include "fff.h"

#define UNIT_TESTING

#ifdef UNIT_TESTING

// Declare a function as a function pointer and add a function declaration with _Impl postfix.
//
// Example:
//   int example( char* name );
//
// Use of the macro:
//   FUNC_DECL(int, example, ( char* name ));
//
// Expands to:
//   int example_Impl( char* name );
//   int (*example)( char* name );
//
# define FUNC_DECL(r,n,a) \
		r n##_Impl a;\
		extern r (*n) a

// Implement a function with _Impl postfix and assign its address to the function pointer.
//
// Example:
//   int example( char* name ){ ...
//
// Use of the macro:
//   FUNC_IMPL(int, example, ( char* name )) { ...
//
// Expands to:
//   int (*example)( char* name ) = example_Impl;
//   int example_Impl( char* name ) { ...
//
# define FUNC_IMPL(r,n,a) \
	r (*n) a = n##_Impl;\
	r n##_Impl a


#else

# define FUNC_DECL(r,n,a) extern r n a
# define FUNC_IMPL(r,n,a) r n a

#endif

DEFINE_FFF_GLOBALS;
FAKE_VALUE_FUNC(int, foo2, int, char* );

FUNC_DECL(void, foo3, (int a, const char* b));

FUNC_IMPL(void, foo3, (int a, const char* b)){

}


void myTest(){
	foo3( 3, "" );
}
//using namespace ::testing;

//class FreeFunctionHooks
//{
//public:
//	MOCK_METHOD1(malloc, void *(size_t));
//};
//#include <string.h>
//#define INIT_HOOKS() FreeFunctionHooks hooks
//
//
//TEST( AB, CD )
//{
//	INIT_HOOKS();
//
//#define malloc(x) hooks.malloc(x)
//#include "foo.c"
//#undef malloc
//
//	char buffer[100];
//	void* ptr = (void*) buffer;
//	EXPECT_CALL(hooks, malloc(_))
//		.WillRepeatedly(ReturnPointee(ptr));
//
//	foo(42);
//
//	ASSERT_STREQ("hello world\n", buffer);
//
//	EXPECT_CALL(hooks, malloc(31337))
//		.WillOnce(ReturnNull());
//
//	foo(31337);
//}
//
//
