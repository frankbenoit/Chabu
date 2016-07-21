/*
 * UtilsTest.cpp
 *
 *  Created on: 04.07.2016
 *      Author: Frank
 */

#include "gtest/gtest.h"
#include "Utils.h"
#include "Chabu.h"
//#include <stringstream>
#include <string>
#include <regex>
using namespace std;
namespace {}
namespace {
	struct Chabu_ByteBuffer_Data curBuf, expBuf;
	uint8 rawCur[1000], rawExp[1000];
	stringstream ostr;


	void setup(){
		ostr.str("");
		Chabu_ByteBuffer_Init( &curBuf, rawCur, sizeof(rawCur));
		Chabu_ByteBuffer_Init( &expBuf, rawExp, sizeof(rawExp));
	}
	void flip(){
		Chabu_ByteBuffer_flip( &curBuf );
		Chabu_ByteBuffer_flip( &expBuf );
	}
}

TEST(UtilsTest, compareBuffers_findsMismatchPos) {

	setup();

	Chabu_ByteBuffer_AppendHex(&curBuf, string("00 01 02 03"));
	Chabu_ByteBuffer_AppendHex(&expBuf, string("00 01 22 03"));

	flip();

	VerifyContent(ostr, &expBuf, &curBuf);
	string text = ostr.str();
	EXPECT_TRUE(text.find("00 01 22 03") >= 0) << ">>>" << text;
	//	cout << text << endl;
}

TEST( UtilsTest, compareBuffers_longerContent_showMismatchPosAfterShorter ){

	setup();

	Chabu_ByteBuffer_AppendHex( &curBuf, string("00 01 02 03"));
	Chabu_ByteBuffer_AppendHex( &expBuf, string("00 01 02 03 04"));

	flip();

	VerifyContent( ostr, &expBuf, &curBuf );
	string text = ostr.str();
	EXPECT_EQ( 0, text.find("mismatch after position 4")) << "text was: " << text;
}

TEST( UtilsTest, DumpHex_hasSpace ){

	setup();

	Chabu_ByteBuffer_AppendHex( &curBuf, string("00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 "));

	flip();

	Utils_DumpHex( ostr, curBuf );
	string text = ostr.str();
	ASSERT_TRUE( regex_search(text, regex("0000 00 01 02 03  00 01 02 03  00 01 02 03  00 01 02 03 +\\r?\\n") )) << "text was: " << text;
	ASSERT_TRUE( regex_search(text, regex("0010 00 01 02 03  00 01 02 03  00 01 02 03  00 01 02 03 +\\r?\\n") )) << "text was: " << text;
	ASSERT_TRUE( regex_search(text, regex("0020 00 01 02 03  00 01 02 03  00 01 02 03  00 01 02 03 +\\r?\\n") )) << "text was: " << text;
}


