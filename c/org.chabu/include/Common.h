/*
 * common.h
 *
 *  Created on: 14.07.2011
 *      Author: Frank Benoit
 */

#ifndef CHABU_SRC_COMMON_H_
#define CHABU_SRC_COMMON_H_

//#include <string.h>
//#include <stdbool.h>
//#include "ChabuOpts.h"
//#include <pthread.h>
#include <stddef.h>

#ifdef _WIN32
#    define CALL_SPEC __cdecl
#    ifdef _LIB
#        define LIBRARY_API __stdcall __declspec(dllexport)
#    else
#        define LIBRARY_API __stdcall __declspec(dllimport)
#    endif
#else
#    define LIBRARY_API
#    define CALL_SPEC
#endif

#ifndef __cplusplus
# define true 1
# define false 0
#endif

#define countof(_v) (sizeof(_v)/(sizeof(_v[0])))

#define BIT_NONE 0
#define BIT_00 1
#define BIT_01 2
#define BIT_02 4
#define BIT_03 8
#define BIT_04 16
#define BIT_05 32
#define BIT_06 64
#define BIT_07 128
#define BIT_08 (1L<< 8)
#define BIT_09 (1L<< 9)
#define BIT_10 (1L<<10)
#define BIT_11 (1L<<11)
#define BIT_12 (1L<<12)
#define BIT_13 (1L<<13)
#define BIT_14 (1L<<14)
#define BIT_15 (1L<<15)
#define BIT_16 (1L<<16)
#define BIT_17 (1L<<17)
#define BIT_18 (1L<<18)
#define BIT_19 (1L<<19)
#define BIT_20 (1L<<20)
#define BIT_21 (1L<<21)
#define BIT_22 (1L<<22)
#define BIT_23 (1L<<23)
#define BIT_24 (1L<<24)
#define BIT_25 (1L<<25)
#define BIT_26 (1L<<26)
#define BIT_27 (1L<<27)
#define BIT_28 (1L<<28)
#define BIT_29 (1L<<29)
#define BIT_30 (1L<<30)
#define BIT_31 (1L<<31)
#define BIT_32 (1LL<<32)
#define BIT_33 (1LL<<33)
#define BIT_34 (1LL<<34)
#define BIT_35 (1LL<<35)
#define BIT_36 (1LL<<36)
#define BIT_37 (1LL<<37)
#define BIT_38 (1LL<<38)
#define BIT_39 (1LL<<39)
#define BIT_40 (1LL<<40)
#define BIT_41 (1LL<<41)
#define BIT_42 (1LL<<42)
#define BIT_43 (1LL<<43)
#define BIT_44 (1LL<<44)
#define BIT_45 (1LL<<45)
#define BIT_46 (1LL<<46)
#define BIT_47 (1LL<<47)
#define BIT_48 (1LL<<48)
#define BIT_49 (1LL<<49)
#define BIT_50 (1LL<<50)
#define BIT_51 (1LL<<51)
#define BIT_52 (1LL<<52)
#define BIT_53 (1LL<<53)
#define BIT_54 (1LL<<54)
#define BIT_55 (1LL<<55)
#define BIT_56 (1LL<<56)
#define BIT_57 (1LL<<57)
#define BIT_58 (1LL<<58)
#define BIT_59 (1LL<<59)
#define BIT_60 (1LL<<60)
#define BIT_61 (1LL<<61)
#define BIT_62 (1LL<<62)
#define BIT_63 (1LL<<63)

typedef signed char        int8;
typedef signed short       int16;
typedef signed long        int32;
typedef signed long long   int64;

typedef unsigned char      uint8;
typedef unsigned short     uint16;
typedef unsigned long      uint32;
typedef unsigned long long uint64;

#define UNUSED(x) do { (void)(x); } while (0)

#define UINT16_HI(v) (uint8)(((uint16)(v))>>8)
#define UINT16_LO(v) (uint8)(((uint16)(v)))
#define UINT16_B0 UINT16_LO
#define UINT16_B1 UINT16_HI

#define UINT32_HI(v) (uint16)(((uint32)(v))>>16)
#define UINT32_LO(v) (uint16)(((uint32)(v)))
#define UINT32_B3(v) (uint8)(((uint32)(v))>>24)
#define UINT32_B2(v) (uint8)(((uint32)(v))>>16)
#define UINT32_B1(v) (uint8)(((uint32)(v))>>8)
#define UINT32_B0(v) (uint8)(((uint32)(v)))

#define UINT64_HI(v) (uint32)(((uint64)(v))>>32)
#define UINT64_LO(v) (uint32)(((uint64)(v)))

#define UINT64_B7(v) (uint8)(((uint64)(v))>>56)
#define UINT64_B6(v) (uint8)(((uint64)(v))>>48)
#define UINT64_B5(v) (uint8)(((uint64)(v))>>40)
#define UINT64_B4(v) (uint8)(((uint64)(v))>>32)
#define UINT64_B3(v) (uint8)(((uint64)(v))>>24)
#define UINT64_B2(v) (uint8)(((uint64)(v))>>16)
#define UINT64_B1(v) (uint8)(((uint64)(v))>>8)
#define UINT64_B0(v) (uint8)(((uint64)(v)))

#define UINT16_COMP(hi,lo) ((((uint16)(hi)) <<  8 ) | (((uint16)(lo)) & UINT16_LO_MASK))
#define UINT16_COMP2 UINT16_COMP
#define UINT32_COMP(hi,lo) ((((uint32)(hi)) << 16 ) | (((uint32)(lo)) & UINT32_LO_MASK))
#define UINT32_COMP2 UINT32_COMP
#define UINT32_COMP4(hi,mh,ml,lo) ((((hi)&UINT32_B0_MASK)<<24) |(((mh)&UINT32_B0_MASK)<<16) |((((ml)&UINT32_B0_MASK))<<8) | ((lo)&UINT32_B0_MASK) )
#define UINT64_COMP(hi,lo) ((((uint64)(hi)) << 32 ) | (((uint64)(lo)) & UINT64_LO_MASK))
#define UINT62_COMP2 UINT64_COMP
#define UINT64_COMP4(hi,mh,ml,lo) ((((hi)&UINT64_W0_MASK)<<24) |(((mh)&UINT64_W0_MASK)<<16) |((((ml)&UINT64_W0_MASK))<<8) | ((lo)&UINT64_W0_MASK) )
#define UINT64_COMP8(b7,b6,b5,b4,b3,b2,b1,b0) ((((b7)&UINT64_B0_MASK)<<(7*8)) |(((b6)&UINT64_B0_MASK)<<(6*8)) |(((b5)&UINT64_B0_MASK)<<(5*8)) | (((b4)&UINT64_B0_MASK)<<(4*8))|(((b3)&UINT64_B0_MASK)<<(3*8)) |(((b2)&UINT64_B0_MASK)<<(2*8)) |((((b1)&UINT64_B0_MASK))<<(1*8)) | ((b0)&UINT64_B0_MASK<<(0*8)))

#define UINT8_GET_UNALIGNED(_p)  ({ void* _p2 = (_p); *((uint8*)_p2)[0]; })
#define UINT16_GET_UNALIGNED(_p) ({ void* _p2 = (_p); UINT16_COMP2( ((uint8*)_p2)[1], ((uint8*)_p2)[0] ); })
#define UINT32_GET_UNALIGNED(_p) ({ void* _p2 = (_p); UINT32_COMP4( ((uint8*)_p2)[3], ((uint8*)_p2)[2], ((uint8*)_p2)[1], ((uint8*)_p2)[0] ); })
#define UINT64_GET_UNALIGNED(_p) ({ void* _p2 = (_p); UINT64_COMP8( ((uint8*)_p2)[7], ((uint8*)_p2)[6], ((uint8*)_p2)[5], ((uint8*)_p2)[4], ((uint8*)_p2)[3], ((uint8*)_p2)[2], ((uint8*)_p2)[1], ((uint8*)_p2)[0] ); })

#define UINT8_GET(p,o) (*(((uint8*)(p))+(o)))
#define UINT8_PUT(p,o,v) (*(((uint8*)(p))+(o)))=(v)

#define UINT8_GET_UNALIGNED_HTON(p)  UINT8_GET(p,0)
#define UINT16_GET_UNALIGNED_HTON(p) UINT16_COMP2(UINT8_GET(p,0),UINT8_GET(p,1))
#define UINT32_GET_UNALIGNED_HTON(p) UINT32_COMP4(UINT8_GET(p,0),UINT8_GET(p,1),UINT8_GET(p,2),UINT8_GET(p,3))
#define UINT64_GET_UNALIGNED_HTON(p) UINT64_COMP8(UINT8_GET(p,0),UINT8_GET(p,1),UINT8_GET(p,2),UINT8_GET(p,3),UINT8_GET(p,4),UINT8_GET(p,5),UINT8_GET(p,6),UINT8_GET(p,7))

#define UINT8_PUT_UNALIGNED(_p,_v)  ({ uint8* _p2 = (uint8*)(_p); uint8  _v2 = (uint8 )_v; _p2[0] = _v2; })
#define UINT16_PUT_UNALIGNED(_p,_v) ({ uint8* _p2 = (uint8*)(_p); uint16 _v2 = (uint16)_v; _p2[0] = _v2; _p2[1] = _v2 >>  8; })
#define UINT32_PUT_UNALIGNED(_p,_v) ({ uint8* _p2 = (uint8*)(_p); uint32 _v2 = (uint32)_v; _p2[0] = _v2; _p2[1] = _v2 >>  8;  _p2[2] = _v2 >> 16; _p2[3] = _v2 >> 24; })
#define UINT64_PUT_UNALIGNED(_p,_v) ({ uint8* _p2 = (uint8*)(_p); uint64 _v2 = (uint64)_v; _p2[0] = _v2; _p2[1] = _v2 >>  8;  _p2[2] = _v2 >> 16; _p2[3] = _v2 >> 24; _p2[4] = _v2 >> 32; _p2[5] = _v2 >> 40;  _p2[6] = _v2 >> 48; _p2[7] = _v2 >> 56; })

#define UINT8_PUT_UNALIGNED_HTON(_p,_v)  UINT8_PUT(_p,0,UINT32_B1(_v))
#define UINT16_PUT_UNALIGNED_HTON(_p,_v) UINT8_PUT(_p,0,UINT16_B1(_v)),UINT8_PUT(_p,1,UINT16_B0(_v))
#define UINT32_PUT_UNALIGNED_HTON(_p,_v) UINT8_PUT(_p,0,UINT32_B3(_v)),UINT8_PUT(_p,1,UINT32_B2(_v)),UINT8_PUT(_p,2,UINT32_B1(_v)),UINT8_PUT(_p,3,UINT32_B0(_v))
#define UINT64_PUT_UNALIGNED_HTON(_p,_v) UINT8_PUT(_p,0,UINT64_B7(_v)),UINT8_PUT(_p,1,UINT64_B6(_v)),UINT8_PUT(_p,2,UINT64_B5(_v)),UINT8_PUT(_p,3,UINT64_B4(_v)),UINT8_PUT(_p,4,UINT64_B3(_v)),UINT8_PUT(_p,5,UINT64_B2(_v)),UINT8_PUT(_p,6,UINT64_B1(_v)),UINT8_PUT(_p,7,UINT64_B0(_v))

#define UINT16_SWAP(_v) UINT16_COMP2( (_v), (_v) >> 8 )
#define UINT32_SWAP(_v) UINT32_COMP4( (_v), (_v) >> 8, (_v) >> 16, (_v) >> 24 )
#define UINT64_SWAP(_v) UINT64_COMP8( (_v), (_v) >> 8, (_v) >> 16, (_v) >> 24, (_v) >> 32, (_v) >> 40, (_v) >> 48, (_v) >> 56 )

#define UINT16_HTON(_v) UINT16_SWAP(_v)
#define UINT32_HTON(_v) UINT32_SWAP(_v)
#define UINT64_HTON(_v) UINT64_SWAP(_v)


#define UINT16_B0_MASK 0x00FF
#define UINT16_B1_MASK 0xFF00
#define UINT16_LO_MASK 0x00FF
#define UINT16_HI_MASK 0xFF00
#define UINT32_B0_MASK 0x000000FFL
#define UINT32_B1_MASK 0x0000FF00L
#define UINT32_B2_MASK 0x00FF0000L
#define UINT32_B3_MASK 0xFF000000L
#define UINT32_HI_MASK 0xFFFF0000L
#define UINT32_LO_MASK 0x0000FFFFL
#define UINT64_B0_MASK 0x00000000000000FFLL
#define UINT64_B1_MASK 0x000000000000FF00LL
#define UINT64_B2_MASK 0x0000000000FF0000LL
#define UINT64_B3_MASK 0x00000000FF000000LL
#define UINT64_B4_MASK 0x000000FF00000000LL
#define UINT64_B5_MASK 0x0000FF0000000000LL
#define UINT64_B6_MASK 0x00FF000000000000LL
#define UINT64_B7_MASK 0xFF00000000000000LL
#define UINT64_W0_MASK 0x000000000000FFFFLL
#define UINT64_W1_MASK 0x00000000FFFF0000LL
#define UINT64_W2_MASK 0x0000FFFF00000000LL
#define UINT64_W3_MASK 0xFFFF000000000000LL
#define UINT64_LO_MASK 0x00000000FFFFFFFFLL
#define UINT64_HI_MASK 0xFFFFFFFF00000000LL

#ifndef __cplusplus
#define bool uint8
#endif
typedef float       float32;
typedef double      float64;
typedef long double float80;

#define Common_AlignUp8(v) (((v)+7)&~7)
#define Common_AlignUp4(v) (((v)+3)&~3)
#define Common_AlignUp2(v) (((v)+1)&~1)

#define Common_IsAligned2(v) ((((int)(v))&1)==0)
#define Common_IsAligned4(v) ((((int)(v))&3)==0)
#define Common_IsAligned8(v) ((((int)(v))&7)==0)

// ------------------
#define CompileAssert(predicate, file) _impl_CASSERT_LINE(predicate,__LINE__,file)
#define _impl_PASTE(a,b) a##b
#define _impl_CASSERT_LINE(predicate, line, file) \
    typedef char _impl_PASTE(assertion_failed_##file##_,line)[2*!!(predicate)-1];


// ------------------
size_t Common_strnlen( const char *start, size_t maxlen );

#endif /* CHABU_SRC_COMMON_H_ */
