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

namespace {
	struct Chabu_ByteBuffer_Data cur, exp;
	uint8 rawCur[1000], rawExp[1000];
	stringstream ostr;


	void setup(){
		ostr.str("");
		Chabu_ByteBuffer_Init( &cur, rawCur, sizeof(rawCur));
		Chabu_ByteBuffer_Init( &exp, rawExp, sizeof(rawExp));
	}
	void flip(){
		Chabu_ByteBuffer_flip( &cur );
		Chabu_ByteBuffer_flip( &exp );
	}
}

TEST( UtilsTest, compareBuffers_findsMismatchPos ){

	setup();

	Chabu_ByteBuffer_AppendHex( &cur, string("00 01 02 03"));
	Chabu_ByteBuffer_AppendHex( &exp, string("00 01 22 03"));

	flip();

	VerifyContent( ostr, &exp, &cur );
	string text = ostr.str();
	EXPECT_TRUE( text.find("00 01 22 03") >= 0 ) << ">>>" << text;
//	cout << text << endl;
}

TEST( UtilsTest, compareBuffers_longerContent_showMismatchPosAfterShorter ){

	setup();

	Chabu_ByteBuffer_AppendHex( &cur, string("00 01 02 03"));
	Chabu_ByteBuffer_AppendHex( &exp, string("00 01 02 03 04"));

	flip();

	VerifyContent( ostr, &exp, &cur );
	string text = ostr.str();
	EXPECT_TRUE( regex_match(text, regex(".*mismatch after position 4[^\\d].*", regex::extended) )) << "text was: " << text;
}

TEST( UtilsTest, DumpHex_hasSpace ){

	setup();

	Chabu_ByteBuffer_AppendHex( &cur, string("00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 00 01 02 03 "));

	flip();

	Utils_DumpHex( ostr, cur );
	string text = ostr.str();
	ASSERT_TRUE( regex_search(text, regex("0000 00 01 02 03  00 01 02 03  00 01 02 03  00 01 02 03 +\\r?\\n") )) << "text was: " << text;
	ASSERT_TRUE( regex_search(text, regex("0010 00 01 02 03  00 01 02 03  00 01 02 03  00 01 02 03 +\\r?\\n") )) << "text was: " << text;
	ASSERT_TRUE( regex_search(text, regex("0020 00 01 02 03  00 01 02 03  00 01 02 03  00 01 02 03 +\\r?\\n") )) << "text was: " << text;
}


