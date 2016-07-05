/*
 * Utils.h
 *
 *  Created on: 01.07.2016
 *      Author: Frank
 */

#ifndef UTILS_H_
#define UTILS_H_

#include <iostream>
#include <string>

void Chabu_ByteBuffer_AppendHex( struct Chabu_ByteBuffer_Data* buffer, const std::string& hexString );
void VerifyContent(struct Chabu_ByteBuffer_Data* expected, struct Chabu_ByteBuffer_Data* current);
void VerifyContent(std::ostream& ostr, struct Chabu_ByteBuffer_Data* expected, struct Chabu_ByteBuffer_Data* current);

void Utils_DumpHex(struct Chabu_ByteBuffer_Data* bb);
void Utils_DumpHex(std::ostream& ostr, struct Chabu_ByteBuffer_Data& bb);
void Utils_DumpHex(std::ostream& ostr, struct Chabu_ByteBuffer_Data* bb) ;

#endif /* UTILS_H_ */
