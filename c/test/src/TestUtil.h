#ifndef TESTUTIL_H
#define TESTUTIL_H

#include <stdarg.h>
#include <stdio.h>
#include <ByteBuffer.h>

#if defined(_MSC_VER) && _MSC_VER < 1900

#define snprintf c99_snprintf
#define vsnprintf c99_vsnprintf

int c99_vsnprintf(char *outBuf, size_t size, const char *format, va_list ap);
int c99_snprintf(char *outBuf, size_t size, const char *format, ...);

#endif


void TestUtil_DumpHex(struct ByteBuffer_Data* bb);
extern const char* TestUtil_StrCat( int count, ... );
#endif
