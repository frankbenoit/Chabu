
#include "Common.h"
#include "PseudoRandom.h"

#define multiplier (0x5DEECE66DULL)
#define addend     (0x0BULL)
#define mask       ((1ULL << 48) - 1)
#define MIN(x, y) (((x) < (y)) ? (x) : (y))
static int nextInt_(struct PseudoRandom_Data * data);

void PseudoRandom_Init( struct PseudoRandom_Data * data, int64 seed) {
	int i;
	memset( data, 0, sizeof(struct PseudoRandom_Data));
	data->idx = 0;
    data->seed = (seed ^ multiplier) & mask;
    for (i = 0; i < PseudoRandom_BUFFER_SZ; ) {
        int v = nextInt_(data);
        data->buffer[i++ ] = (uint8)(v >> 24);
        data->buffer[i++ ] = (uint8)(v >> 16);
        data->buffer[i++ ] = (uint8)(v >>  8);
        data->buffer[i++ ] = (uint8)(v);
    }
        
}

void PseudoRandom_NextBytes(struct PseudoRandom_Data * data, uint8* bytes, int offset, int length ) {
    int endOffset = offset+length;
    while( offset < endOffset ){
    	int cpySz = MIN( endOffset - offset, PseudoRandom_BUFFER_SZ - data->idx );
		memcpy( &bytes[offset], &data->buffer[ data->idx ], cpySz );
    	offset += cpySz;
    	data->idx += cpySz;
    	if( data->idx == PseudoRandom_BUFFER_SZ ){
    		data->idx = 0;
    	}
    }
}

void PseudoRandom_NextBytesVerify(struct PseudoRandom_Data * data, uint8* bytes, int offset, int length ) {
	int i;
	for( i = 0; i < length; i++ ){
		uint8 exp = data->buffer[ data->idx % PseudoRandom_BUFFER_SZ ];
		uint8 cur = bytes[i+offset];
		data->idx++;
		if( data->idx == PseudoRandom_BUFFER_SZ ){
    		data->idx = 0;
		}
		if( exp != cur ){
			printf("PseudoRandom: mismatch at %d: exp:0x%x != cur:0x%x", i, exp, cur);
			exit(EXIT_FAILURE);
		}
	}
}
    
uint8 PseudoRandom_NextByte(struct PseudoRandom_Data * data) {
    uint8 res = data->buffer[ data->idx++ % PseudoRandom_BUFFER_SZ ];
    if( data->idx == PseudoRandom_BUFFER_SZ ){
    	data->idx = 0;
    }
    return res;
}

static int nextInt_(struct PseudoRandom_Data * data) {
    data->seed = (data->seed * multiplier + addend) & mask;
    return (int)(data->seed >> 16);
}

