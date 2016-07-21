/*
 * ByteBuffer.h
 *
 *  Created on: 19.07.2016
 *      Author: Frank
 */

#ifndef BYTEBUFFER_H_
#define BYTEBUFFER_H_

#include <cstring>
#include <cstdint>
#include <limits>

namespace java { namespace nio {

enum class ByteOrder { little_endian, big_endian };

class ByteBuffer {

	std::uint8_t * data_;
	std::size_t    position_;
	std::size_t    limit_;
	std::size_t    capacity_;
	std::size_t    mark_;
	bool           orderBe_;
	static constexpr std::size_t markDiscarded_ = std::numeric_limits<std::size_t>::max();

public:

	inline ByteBuffer( std::uint8_t * data, std::size_t length )
		: data_(data)
		, position_(0)
		, limit_(0)
		, capacity_(length)
		, mark_(markDiscarded_)
		, orderBe_( true )
	{}

	virtual ~ByteBuffer();

	inline std::uint8_t * array(){ return data_; }
	inline std::uint8_t * arrayPtrAtPosition(){ return data_+position_; }
	inline std::size_t    position(){ return position_; }
	inline ByteBuffer&    positionIncrease(std::size_t increment){ position_ += increment; return *this; }
	inline std::size_t    limit   (){ return limit_; }
	inline std::size_t    capacity(){ return capacity_; }
	inline ByteBuffer&    position(std::size_t newPosition){ position_ = newPosition; return *this; }
	inline ByteBuffer&    limit   (std::size_t newLimit){ limit_ = newLimit; return *this; }
	inline bool           hasRemaining(){ return position_ < limit_; }
	inline std::size_t    remaining(){ return limit_ - position_; }


	inline ByteOrder      order(){ return orderBe_ ? ByteOrder::big_endian : ByteOrder::little_endian; }
	inline ByteBuffer&    order(ByteOrder order){ orderBe_ = ( order == ByteOrder::big_endian ); return *this; }

	inline ByteBuffer&    mark(){ mark_ = position_; return *this; }
	ByteBuffer&           reset();
	inline ByteBuffer&    clear(){ mark_ = markDiscarded_; position_ = 0; limit_ = capacity_; return *this; }
	inline ByteBuffer&    rewing(){ mark_ = markDiscarded_; position_ = 0; return *this; }

	ByteBuffer&    flip();
	ByteBuffer&    compact();

	ByteBuffer&    put(void* value, std::size_t offset, std::size_t length);
	ByteBuffer&    put(ByteBuffer& src);

	ByteBuffer&    putU(uint8_t value);
	ByteBuffer&    put(int8_t value);
	ByteBuffer&    putU(std::size_t index, uint8_t value);
	ByteBuffer&    put(std::size_t index, int8_t value);

	ByteBuffer&    putUShort(uint16_t value);
	ByteBuffer&    putShort(int16_t value);
	ByteBuffer&    putUShort(std::size_t index, uint16_t value);
	ByteBuffer&    putShort(std::size_t index, int16_t value);

	ByteBuffer&    putUInt(uint32_t value);
	ByteBuffer&    putInt(int32_t value);
	ByteBuffer&    putUInt(std::size_t index, uint32_t value);
	ByteBuffer&    putInt(std::size_t index, int32_t value);

	ByteBuffer&    putULong(uint64_t value);
	ByteBuffer&    putLong(int64_t value);
	ByteBuffer&    putULong(std::size_t index, uint64_t value);
	ByteBuffer&    putLong(std::size_t index, int64_t value);

	int8_t         get();
	int8_t         get(std::size_t index);
	uint8_t        getU();
	uint8_t        getU(std::size_t index);

	int16_t         getShort();
	int16_t         getShort(std::size_t index);
	uint16_t        getUShort();
	uint16_t        getUShort(std::size_t index);

	int32_t         getInt();
	int32_t         getInt(std::size_t index);
	uint32_t        getUInt();
	uint32_t        getUInt(std::size_t index);

	int64_t         getLong();
	int64_t         getLong(std::size_t index);
	uint64_t        getULong();
	uint64_t        getULong(std::size_t index);



};

}} /* namespace java::nio */

#endif /* BYTEBUFFER_H_ */
