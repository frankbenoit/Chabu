#ifndef __DATAGEN
#define __DATAGEN

#include "Common.h"
#include "PseudoRandom.h"

struct DataGen_Data {

    struct PseudoRandom_Data gen;
    struct PseudoRandom_Data exp;
    long genPos;
    long expPos;
    const char* name;

};

void DataGen_Init(struct DataGen_Data* data, const char* name, int64 seed);
void DataGen_GetGenBytes(struct DataGen_Data* data, uint8* bytes, int offset, int length);
const char* DataGen_GetGenBytesString(struct DataGen_Data* data, int numBytes);
void DataGen_GetExpBytes(struct DataGen_Data* data, uint8* bytes, int offset, int length);
const char* DataGen_GetExpBytesString(struct DataGen_Data* data, int numBytes);
void DataGen_EnsureSamePosition(struct DataGen_Data* data);

#endif