/*
 * Utils.c
 *
 *  Created on: 01.07.2016
 *      Author: Frank
 */


#include "Chabu.h"
#include "Utils.h"

#include <iostream>
#include <iomanip>
#include <string>
#include <stdexcept>
#include <algorithm>
#include <boost/format.hpp>
using namespace std;
using boost::format;

static inline int hexChar2int(char input) {
  if(input >= '0' && input <= '9')
    return input - '0';
  if(input >= 'A' && input <= 'F')
    return input - 'A' + 10;
  if(input >= 'a' && input <= 'f')
    return input - 'a' + 10;
  throw std::invalid_argument("Invalid input string");
}

void Chabu_ByteBuffer_AppendHex( struct Chabu_ByteBuffer_Data* buffer, const string& hexString ){
	size_t strIdx = 0;
	while( strIdx+1 < hexString.length() ){
		if( strIdx > 0 ){
			if( hexString.at( strIdx-1) != ' ' ){
				  throw std::invalid_argument("missing space");
			}
		}
		char nibbleHigh = hexString.at(strIdx);
		char nibbleLow = hexString.at(strIdx+1);

		uint8 d = 0;
		d |= hexChar2int( nibbleHigh ) << 4;
		d |= hexChar2int( nibbleLow );

		Chabu_ByteBuffer_putByte( buffer, d );

		strIdx += 3;
	}
}

void VerifyContent(struct Chabu_ByteBuffer_Data* expected, struct Chabu_ByteBuffer_Data* current) {
	VerifyContent( cout, expected, current );
}
void VerifyContent( ostream& ostr, struct Chabu_ByteBuffer_Data* expected, struct Chabu_ByteBuffer_Data* current) {
//	int more = 0;
//	bool isOk;
//	int mismatchPos;
//
//
//
//	Chabu_ByteBuffer_clear(&data->txBuf);
//	data->txBuf.limit = data->bb.limit + more;
//	Chabu_GetXmitData( data->chabu, &data->txBuf);
//	ByteBuffer_flip(&data->txBuf);
//
	int expectedLen = Chabu_ByteBuffer_remaining(expected);
	int currentLen = Chabu_ByteBuffer_remaining(current);
	int compareLen = min( expectedLen, currentLen );

	bool isOk = true;
	int mismatchPos = -1;

	if( expectedLen != currentLen ){
		isOk = false;
	}
	else {
		int i;
		for( i = 0; i < compareLen; i++ ){
			int exp = 0xFF & expected->data[ expected->position + i];
			int cur = 0xFF & current->data[ current->position + i];
			if( exp != cur ){
				isOk = false;
				mismatchPos = i;
				break;
			}
		}
	}

	if( !isOk ){
//		int i;
		if( mismatchPos < 0 ){
			mismatchPos = compareLen;
			ostr << format("mismatch after position %d") % mismatchPos << endl;
		}
		else{
			ostr << format("mismatch at position %d 0x%02X <> 0x%02X (exp <> cur)") % mismatchPos % (int)expected->data[mismatchPos] % (int)current->data[mismatchPos] << endl;
		}
		ostr << "Expected:" << endl;
		Utils_DumpHex(ostr, expected);
		ostr << "Current:" << endl;
		Utils_DumpHex(ostr, current);
//		TraceRunner_Ensure( data->txBuf.limit == data->bb.limit, "WIRE_TX @%d: TX length (%d) does not match the expected length (%d). First mismatch at pos %d", data->blockLineNum, data->txBuf.limit, data->bb.limit, mismatchPos );
//		for( i = 0; i < data->bb.limit; i++ ){
//			int exp = 0xFF & data->bb.data[i];
//			int cur = 0xFF & data->txBuf.data[i];
//            TraceRunner_Ensure(cur == exp, "TX data (0x%02X) != expected (0x%02X) at index 0x%04X", cur, exp, i);
//		}
	}
}

//
//#if defined(_MSC_VER) && _MSC_VER < 1900
//
//int c99_vsnprintf(char *outBuf, size_t size, const char *format, va_list ap)
//{
//    int count = -1;
//
//    if (size != 0)
//        count = _vsnprintf_s(outBuf, size, _TRUNCATE, format, ap);
//    if (count == -1)
//        count = _vscprintf(format, ap);
//
//    return count;
//}
//
//int c99_snprintf(char *outBuf, size_t size, const char *format, ...)
//{
//    int count;
//    va_list ap;
//
//    va_start(ap, format);
//    count = c99_vsnprintf(outBuf, size, format, ap);
//    va_end(ap);
//
//    return count;
//}
//
//#endif

void Utils_DumpHex(struct Chabu_ByteBuffer_Data* bb) {
	Utils_DumpHex( cout, bb );
}
void Utils_DumpHex(ostream& ostr, struct Chabu_ByteBuffer_Data& bb) {
	Utils_DumpHex( ostr, &bb );
}
void Utils_DumpHex(ostream& ostr, struct Chabu_ByteBuffer_Data* bb) {
	int i = bb->position;
	int c = 0;
	for( ; i < bb->limit; i++, c++ ){

		if( c % 16 == 0 )
			ostr << format("%04X ") % c ;

		ostr << format("%02X ") % (int)bb->data[i] ;

		if( c % 4 == 3 ) ostr << ' ';
		if( c % 16 == 15 ) ostr << endl;
	}
	if( c % 16 != 0 ) ostr << endl;
}

//const char* Utils_StrCat( int count, ... ){
//    int i;
//    char *merged;
//	int len;
//	int null_pos;
//
//	va_list ap;
//
//    // Find required length to store merged string
//    len = 1; // room for NULL
//    va_start(ap, count);
//    for(i=0 ; i<count ; i++){
//        len += strlen(va_arg(ap, char*));
//	}
//    va_end(ap);
//
//    // Allocate memory to concat strings
//	merged = (char*)malloc(len);
//    null_pos = 0;
//
//    // Actually concatenate strings
//    va_start(ap, count);
//    for(i=0 ; i<count ; i++)
//    {
//        char *s = va_arg(ap, char*);
//        strcpy(merged+null_pos, s);
//        null_pos += strlen(s);
//		free(s);
//    }
//    va_end(ap);
//
//    return merged;
//}
