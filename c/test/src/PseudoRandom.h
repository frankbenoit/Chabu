#ifndef __PSEUDO_RANDOM
#define __PSEUDO_RANDOM

#include <Common.h>

#define PseudoRandom_BUFFER_SZ 0x2000

struct PseudoRandom_Data {

	int64 seed;
    uint8 buffer[PseudoRandom_BUFFER_SZ];
    int idx;

};

void PseudoRandom_Init(struct PseudoRandom_Data* data, int64 seed);
void PseudoRandom_NextBytes(struct PseudoRandom_Data* data, uint8* bytes, int offset, int length );
void PseudoRandom_NextBytesVerify(struct PseudoRandom_Data* data, uint8* bytes, int offset, int length );
uint8 PseudoRandom_NextByte(struct PseudoRandom_Data* data);

#endif