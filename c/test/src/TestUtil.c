#include "TestUtil.h"

#include <stdlib.h>       // calloc
#include <stdarg.h>
#include <stdio.h>
#include <string.h>

#if defined(_MSC_VER) && _MSC_VER < 1900

int c99_vsnprintf(char *outBuf, size_t size, const char *format, va_list ap)
{
    int count = -1;

    if (size != 0)
        count = _vsnprintf_s(outBuf, size, _TRUNCATE, format, ap);
    if (count == -1)
        count = _vscprintf(format, ap);

    return count;
}

int c99_snprintf(char *outBuf, size_t size, const char *format, ...)
{
    int count;
    va_list ap;

    va_start(ap, format);
    count = c99_vsnprintf(outBuf, size, format, ap);
    va_end(ap);

    return count;
}

#endif

void TestUtil_DumpHex(struct ByteBuffer_Data* bb) {
	int i = bb->position;
	int c = 0;
	for( ; i < bb->limit; i++, c++ ){

		if( c % 16 == 0 ) printf("%04X: ", c );

		printf("%02X ", bb->data[i] );

		if( c % 4 == 3 ) printf(" ");
		if( c % 16 == 15 ) printf("\n");
	}
	if( c % 16 != 0 ) printf("\n");
}

const char* TestUtil_StrCat( int count, ... ){
    int i;
    char *merged;
	int len;
	int null_pos;

	va_list ap;

    // Find required length to store merged string
    len = 1; // room for NULL
    va_start(ap, count);
    for(i=0 ; i<count ; i++){
        len += strlen(va_arg(ap, char*));
	}
    va_end(ap);

    // Allocate memory to concat strings
	merged = (char*)calloc(sizeof(char),len);
    null_pos = 0;

    // Actually concatenate strings
    va_start(ap, count);
    for(i=0 ; i<count ; i++)
    {
        char *s = va_arg(ap, char*);
        strcpy(merged+null_pos, s);
        null_pos += strlen(s);
		free(s);
    }
    va_end(ap);

    return merged;
}
