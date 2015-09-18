#include <stdlib.h>
#include "Common.h"
#include "DataGen.h"
#include "PseudoRandom.h"

static const char* DataGen_GetBytesString(struct PseudoRandom_Data* rnd, int numBytes);
static char toHexDigit(int digit);

void DataGen_Init(struct DataGen_Data* data, const char* name, int64 seed)
{
    data->name = name;
	data->genPos = 0;
	data->expPos = 0;
    PseudoRandom_Init( &data->gen, seed);
    PseudoRandom_Init( &data->exp, seed);
}

void DataGen_GetGenBytes(struct DataGen_Data* data, uint8* bytes, int offset, int length)
{
    PseudoRandom_NextBytes(&data->gen, bytes, offset, length);
    data->genPos += length;
}

//TODO check callers for free'ing result
const char* DataGen_GetGenBytesString(struct DataGen_Data* data, int numBytes)
{
    data->genPos += numBytes;
    return DataGen_GetBytesString(&data->gen, numBytes);
}

void DataGen_GetExpBytes(struct DataGen_Data* data, uint8* bytes, int offset, int length)
{
    data->expPos += length;
    PseudoRandom_NextBytes(&data->exp, bytes, offset, length);
}

//TODO check callers for free'ing result
const char* DataGen_GetExpBytesString(struct DataGen_Data* data, int numBytes)
{
    data->expPos += numBytes;
    return DataGen_GetBytesString( &data->exp, numBytes);
}

static const char* DataGen_GetBytesString(struct PseudoRandom_Data* rnd, int numBytes)
{
	int i, j;
    uint8* bytes;
    char* chars;

	if (numBytes == 0)
    {
        return "";
    }
	
	bytes = (uint8*)calloc(numBytes, 1);
	chars = (char*)calloc(numBytes*3, 1);
    
	PseudoRandom_NextBytes(rnd, bytes, 0, numBytes);
    
	for (i = 0, j = 0; i < numBytes; i++, j += 3)
    {
		chars[j+0] = toHexDigit((bytes[i] & 0xF0) >> 4);
		chars[j+1] = toHexDigit((bytes[i] & 0x0F) >> 0);
        chars[j+2] = ' ';
    }
	free(bytes);
    chars[numBytes*3-1] = '\0';
    return chars;
}
static char toHexDigit(int digit)
{
    if (digit < 10) return (char)('0' + digit);
    return (char)('A' + digit - 10);
}
void DataGen_EnsureSamePosition(struct DataGen_Data* data)
{
    if (data->genPos != data->expPos)
    {
        printf("DataGen %s, positions not equal: exp:%d gen:%d", data->name, data->expPos, data->genPos);
		exit( EXIT_FAILURE);
    }
}
