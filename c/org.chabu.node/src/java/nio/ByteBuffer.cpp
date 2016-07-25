/*
 * ByteBuffer.cpp
 *
 *  Created on: 19.07.2016
 *      Author: Frank
 */

#include "ByteBuffer.h"
#include <stdexcept>

namespace java { namespace nio {

ByteBuffer::~ByteBuffer()
{
}

ByteBuffer&    ByteBuffer::flip(){
	limit_ = position_;
	position_ = 0;
	mark_ = markDiscarded_;
	return *this;
}

ByteBuffer&    ByteBuffer::compact(){
	std::size_t sz = remaining();
	if( sz && position_ ){
		std::memmove( data_, data_ + position_, sz );
	}
	limit_ = capacity_;
	position_ = sz;
	mark_ = markDiscarded_;
	return *this;
}

ByteBuffer&    ByteBuffer::reset(){
	if( mark_ == markDiscarded_ ) throw std::runtime_error("ByteBuffer: invalid mark");
	mark_ = position_;
	return *this;
}

ByteBuffer&    ByteBuffer::put(const void* value, std::size_t offset, std::size_t length){
	if( offset > length ) throw std::out_of_range("ByteBuffer");
	std::size_t rem = remaining();
	if( rem < length ) throw std::overflow_error("ByteBuffer");
	std::memcpy( data_ + position_, (char*)value + offset, length );
	position_ += length;
	return *this;
}
ByteBuffer&    ByteBuffer::put(ByteBuffer& src){
	std::size_t remT = remaining();
	std::size_t remS = src.remaining();
	if( remT < remS ) throw std::overflow_error("ByteBuffer");
	std::memcpy( data_ + position_, src.data_+ src.position_, remS );
	position_ += remS;
	src.position_ += remS;

	return *this;
}

ByteBuffer&    ByteBuffer::putU(uint8_t value){
	if( position_ >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	data_[ position_++ ] = value;
	return *this;
}
ByteBuffer&    ByteBuffer::put(int8_t value){
	if( position_ >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	data_[ position_++ ] = value;
	return *this;
}
ByteBuffer&    ByteBuffer::putU(std::size_t index, uint8_t value){
	if( index >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	data_[ index ] = value;
	return *this;
}
ByteBuffer&    ByteBuffer::put(std::size_t index, int8_t value){
	if( index >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	data_[ index ] = value;
	return *this;
}

ByteBuffer&    ByteBuffer::putUShort(uint16_t value){
	if( position_+1 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	if( orderBe_ ){
		data_[ position_ + 1 ] = value; value >>= 8;
		data_[ position_ + 0 ] = value;
	}
	else {
		data_[ position_ + 0 ] = value; value >>= 8;
		data_[ position_ + 1 ] = value;
	}
	position_ += 2;
	return *this;
}
ByteBuffer&    ByteBuffer::putShort(int16_t value){
	if( position_+1 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	if( orderBe_ ){
		data_[ position_ + 1 ] = value; value >>= 8;
		data_[ position_ + 0 ] = value;
	}
	else {
		data_[ position_ + 1 ] = value;
		data_[ position_ + 0 ] = value; value >>= 8;
	}
	position_ += 2;

	return *this;
}
ByteBuffer&    ByteBuffer::putUShort(std::size_t index, uint16_t value){
	if( index+1 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	if( orderBe_ ){
		data_[ index + 1 ] = value; value >>= 8;
		data_[ index + 0 ] = value;
	}
	else {
		data_[ index + 0 ] = value; value >>= 8;
		data_[ index + 1 ] = value;
	}
	return *this;
}
ByteBuffer&    ByteBuffer::putShort(std::size_t index, int16_t value){
	if( index+1 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	if( orderBe_ ){
		data_[ index + 1 ] = value; value >>= 8;
		data_[ index + 0 ] = value;
	}
	else {
		data_[ index + 0 ] = value; value >>= 8;
		data_[ index + 1 ] = value;
	}
	return *this;
}

ByteBuffer&    ByteBuffer::putUInt(uint32_t value){
	if( position_+3 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	if( orderBe_ ){
		data_[ position_ + 3 ] = value;	value >>= 8;
		data_[ position_ + 2 ] = value;	value >>= 8;
		data_[ position_ + 1 ] = value;	value >>= 8;
		data_[ position_ + 0 ] = value;
	}
	else {
		data_[ position_ + 0 ] = value; value >>= 8;
		data_[ position_ + 1 ] = value; value >>= 8;
		data_[ position_ + 2 ] = value; value >>= 8;
		data_[ position_ + 3 ] = value;
	}
	position_ += 4;

	return *this;
}
ByteBuffer&    ByteBuffer::putInt(int32_t value){
	if( position_+3 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	if( orderBe_ ){
		data_[ position_ + 3 ] = value;	value >>= 8;
		data_[ position_ + 2 ] = value;	value >>= 8;
		data_[ position_ + 1 ] = value;	value >>= 8;
		data_[ position_ + 0 ] = value;
	}
	else {
		data_[ position_ + 0 ] = value; value >>= 8;
		data_[ position_ + 1 ] = value; value >>= 8;
		data_[ position_ + 2 ] = value; value >>= 8;
		data_[ position_ + 3 ] = value;
	}
	position_ += 4;

	return *this;
}
ByteBuffer&    ByteBuffer::putUInt(std::size_t index, uint32_t value){
	if( index+3 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	if( orderBe_ ){
		data_[ index + 3 ] = value;	value >>= 8;
		data_[ index + 2 ] = value;	value >>= 8;
		data_[ index + 1 ] = value;	value >>= 8;
		data_[ index + 0 ] = value;
	}
	else {
		data_[ index + 0 ] = value; value >>= 8;
		data_[ index + 1 ] = value; value >>= 8;
		data_[ index + 2 ] = value; value >>= 8;
		data_[ index + 3 ] = value;
	}

	return *this;
}
ByteBuffer&    ByteBuffer::putInt(std::size_t index, int32_t value){
	if( index+3 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	if( orderBe_ ){
		data_[ index + 3 ] = value;	value >>= 8;
		data_[ index + 2 ] = value;	value >>= 8;
		data_[ index + 1 ] = value;	value >>= 8;
		data_[ index + 0 ] = value;
	}
	else {
		data_[ index + 0 ] = value; value >>= 8;
		data_[ index + 1 ] = value; value >>= 8;
		data_[ index + 2 ] = value; value >>= 8;
		data_[ index + 3 ] = value;
	}

	return *this;
}

ByteBuffer&    ByteBuffer::putULong(uint64_t value){
	if( position_+7 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	if( orderBe_ ){
		data_[ position_ + 7 ] = value;	value >>= 8;
		data_[ position_ + 6 ] = value;	value >>= 8;
		data_[ position_ + 5 ] = value;	value >>= 8;
		data_[ position_ + 4 ] = value;	value >>= 8;
		data_[ position_ + 3 ] = value;	value >>= 8;
		data_[ position_ + 2 ] = value;	value >>= 8;
		data_[ position_ + 1 ] = value;	value >>= 8;
		data_[ position_ + 0 ] = value;
	}
	else {
		data_[ position_ + 0 ] = value; value >>= 8;
		data_[ position_ + 1 ] = value; value >>= 8;
		data_[ position_ + 2 ] = value; value >>= 8;
		data_[ position_ + 3 ] = value; value >>= 8;
		data_[ position_ + 4 ] = value; value >>= 8;
		data_[ position_ + 5 ] = value; value >>= 8;
		data_[ position_ + 6 ] = value; value >>= 8;
		data_[ position_ + 7 ] = value;
	}
	position_ += 8;

	return *this;
}
ByteBuffer&    ByteBuffer::putLong(int64_t value){
	if( position_+7 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	if( orderBe_ ){
		data_[ position_ + 7 ] = value;	value >>= 8;
		data_[ position_ + 6 ] = value;	value >>= 8;
		data_[ position_ + 5 ] = value;	value >>= 8;
		data_[ position_ + 4 ] = value;	value >>= 8;
		data_[ position_ + 3 ] = value;	value >>= 8;
		data_[ position_ + 2 ] = value;	value >>= 8;
		data_[ position_ + 1 ] = value;	value >>= 8;
		data_[ position_ + 0 ] = value;
	}
	else {
		data_[ position_ + 0 ] = value; value >>= 8;
		data_[ position_ + 1 ] = value; value >>= 8;
		data_[ position_ + 2 ] = value; value >>= 8;
		data_[ position_ + 3 ] = value; value >>= 8;
		data_[ position_ + 4 ] = value; value >>= 8;
		data_[ position_ + 5 ] = value; value >>= 8;
		data_[ position_ + 6 ] = value; value >>= 8;
		data_[ position_ + 7 ] = value;
	}
	position_ += 8;

	return *this;
}
ByteBuffer&    ByteBuffer::putULong(std::size_t index, uint64_t value){
	if( index+7 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	if( orderBe_ ){
		data_[ index + 7 ] = value;	value >>= 8;
		data_[ index + 6 ] = value;	value >>= 8;
		data_[ index + 5 ] = value;	value >>= 8;
		data_[ index + 4 ] = value;	value >>= 8;
		data_[ index + 3 ] = value;	value >>= 8;
		data_[ index + 2 ] = value;	value >>= 8;
		data_[ index + 1 ] = value;	value >>= 8;
		data_[ index + 0 ] = value;
	}
	else {
		data_[ index + 0 ] = value; value >>= 8;
		data_[ index + 1 ] = value; value >>= 8;
		data_[ index + 2 ] = value; value >>= 8;
		data_[ index + 3 ] = value; value >>= 8;
		data_[ index + 4 ] = value; value >>= 8;
		data_[ index + 5 ] = value; value >>= 8;
		data_[ index + 6 ] = value; value >>= 8;
		data_[ index + 7 ] = value;
	}

	return *this;
}
ByteBuffer&    ByteBuffer::putLong(std::size_t index, int64_t value){
	if( index+7 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	if( orderBe_ ){
		data_[ index + 7 ] = value;	value >>= 8;
		data_[ index + 6 ] = value;	value >>= 8;
		data_[ index + 5 ] = value;	value >>= 8;
		data_[ index + 4 ] = value;	value >>= 8;
		data_[ index + 3 ] = value;	value >>= 8;
		data_[ index + 2 ] = value;	value >>= 8;
		data_[ index + 1 ] = value;	value >>= 8;
		data_[ index + 0 ] = value;
	}
	else {
		data_[ index + 0 ] = value; value >>= 8;
		data_[ index + 1 ] = value; value >>= 8;
		data_[ index + 2 ] = value; value >>= 8;
		data_[ index + 3 ] = value; value >>= 8;
		data_[ index + 4 ] = value; value >>= 8;
		data_[ index + 5 ] = value; value >>= 8;
		data_[ index + 6 ] = value; value >>= 8;
		data_[ index + 7 ] = value;
	}

	return *this;
}

int8_t         ByteBuffer::get(){
	if( position_ >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	return data_[ position_++ ];
}
int8_t         ByteBuffer::get(std::size_t index){
	if( index >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	return data_[ index ];
}
uint8_t        ByteBuffer::getU(){
	if( position_ >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	return data_[ position_++ ];
}
uint8_t        ByteBuffer::getU(std::size_t index){
	if( index >= limit_ ) throw std::overflow_error("ByteBuffer overflow");
	return data_[ index ];
}

int16_t         ByteBuffer::getShort(){

	if( position_+1 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");

	int16_t res;
	if( orderBe_ ){
		res = data_[ position_ + 0 ];
		res <<= 8;
		res |= data_[ position_ + 1 ];
	}
	else {
		res = data_[ position_ + 1 ];
		res <<= 8;
		res |= data_[ position_ + 0 ];
	}
	position_ += 2;
	return res;
}
int16_t         ByteBuffer::getShort(std::size_t index){
	if( index+1 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");

	int16_t res;
	if( orderBe_ ){
		res = data_[ index + 0 ];
		res <<= 8;
		res |= data_[ index + 1 ];
	}
	else {
		res = data_[ index + 1 ];
		res <<= 8;
		res |= data_[ index + 0 ];
	}
	return res;
}

uint16_t        ByteBuffer::getUShort(){

	if( position_+1 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");

	uint16_t res;
	if( orderBe_ ){
		res = data_[ position_ + 0 ];
		res <<= 8;
		res |= data_[ position_ + 1 ];
	}
	else {
		res = data_[ position_ + 1 ];
		res <<= 8;
		res |= data_[ position_ + 0 ];
	}
	position_ += 2;
	return res;
}

uint16_t        ByteBuffer::getUShort(std::size_t index){
	if( index+1 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");

	uint16_t res;
	if( orderBe_ ){
		res = data_[ index + 0 ];
		res <<= 8;
		res |= data_[ index + 1 ];
	}
	else {
		res = data_[ index + 1 ];
		res <<= 8;
		res |= data_[ index + 0 ];
	}
	return res;
}

int32_t         ByteBuffer::getInt(){
	if( position_+3 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");

	int32_t res;
	if( orderBe_ ){
		res = data_[ position_ + 0 ];
		res <<= 8; res |= data_[ position_ + 1 ];
		res <<= 8; res |= data_[ position_ + 2 ];
		res <<= 8; res |= data_[ position_ + 3 ];
	}
	else {
		res = data_[ position_ + 3 ];
		res <<= 8; res |= data_[ position_ + 2 ];
		res <<= 8; res |= data_[ position_ + 1 ];
		res <<= 8; res |= data_[ position_ + 0 ];
	}
	position_ += 4;
	return res;
}
int32_t         ByteBuffer::getInt(std::size_t index){
	if( index+3 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");

	int32_t res;
	if( orderBe_ ){
		res = data_[ index + 0 ];
		res <<= 8; res |= data_[ index + 1 ];
		res <<= 8; res |= data_[ index + 2 ];
		res <<= 8; res |= data_[ index + 3 ];
	}
	else {
		res = data_[ index + 3 ];
		res <<= 8; res |= data_[ index + 2 ];
		res <<= 8; res |= data_[ index + 1 ];
		res <<= 8; res |= data_[ index + 0 ];
	}
	return res;
}
uint32_t        ByteBuffer::getUInt(){
	if( position_+3 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");

	uint32_t res;
	if( orderBe_ ){
		res = data_[ position_ + 0 ];
		res <<= 8; res |= data_[ position_ + 1 ];
		res <<= 8; res |= data_[ position_ + 2 ];
		res <<= 8; res |= data_[ position_ + 3 ];
	}
	else {
		res = data_[ position_ + 3 ];
		res <<= 8; res |= data_[ position_ + 2 ];
		res <<= 8; res |= data_[ position_ + 1 ];
		res <<= 8; res |= data_[ position_ + 0 ];
	}
	position_ += 4;
	return res;
}
uint32_t        ByteBuffer::getUInt(std::size_t index){
	if( index+3 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");

	uint32_t res;
	if( orderBe_ ){
		res = data_[ index + 0 ];
		res <<= 8; res |= data_[ index + 1 ];
		res <<= 8; res |= data_[ index + 2 ];
		res <<= 8; res |= data_[ index + 3 ];
	}
	else {
		res = data_[ index + 3 ];
		res <<= 8; res |= data_[ index + 2 ];
		res <<= 8; res |= data_[ index + 1 ];
		res <<= 8; res |= data_[ index + 0 ];
	}
	return res;

}

int64_t         ByteBuffer::getLong(){
	if( position_+7 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");

	int64_t res;
	if( orderBe_ ){
		res = data_[ position_ + 0 ];
		res <<= 8; res |= data_[ position_ + 1 ];
		res <<= 8; res |= data_[ position_ + 2 ];
		res <<= 8; res |= data_[ position_ + 3 ];
		res <<= 8; res |= data_[ position_ + 4 ];
		res <<= 8; res |= data_[ position_ + 5 ];
		res <<= 8; res |= data_[ position_ + 6 ];
		res <<= 8; res |= data_[ position_ + 7 ];
	}
	else {
		res = data_[ position_ + 7 ];
		res <<= 8; res |= data_[ position_ + 6 ];
		res <<= 8; res |= data_[ position_ + 5 ];
		res <<= 8; res |= data_[ position_ + 4 ];
		res <<= 8; res |= data_[ position_ + 3 ];
		res <<= 8; res |= data_[ position_ + 2 ];
		res <<= 8; res |= data_[ position_ + 1 ];
		res <<= 8; res |= data_[ position_ + 0 ];
	}
	position_ += 8;
	return res;
}
int64_t         ByteBuffer::getLong(std::size_t index){
	if( index+7 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");

	int64_t res;
	if( orderBe_ ){
		res = data_[ index + 0 ];
		res <<= 8; res |= data_[ index + 1 ];
		res <<= 8; res |= data_[ index + 2 ];
		res <<= 8; res |= data_[ index + 3 ];
		res <<= 8; res |= data_[ index + 4 ];
		res <<= 8; res |= data_[ index + 5 ];
		res <<= 8; res |= data_[ index + 6 ];
		res <<= 8; res |= data_[ index + 7 ];
	}
	else {
		res = data_[ index + 7 ];
		res <<= 8; res |= data_[ index + 6 ];
		res <<= 8; res |= data_[ index + 5 ];
		res <<= 8; res |= data_[ index + 4 ];
		res <<= 8; res |= data_[ index + 3 ];
		res <<= 8; res |= data_[ index + 2 ];
		res <<= 8; res |= data_[ index + 1 ];
		res <<= 8; res |= data_[ index + 0 ];
	}
	return res;
}
uint64_t        ByteBuffer::getULong(){
	if( position_+7 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");

	uint64_t res;
	if( orderBe_ ){
		res = data_[ position_ + 0 ];
		res <<= 8; res |= data_[ position_ + 1 ];
		res <<= 8; res |= data_[ position_ + 2 ];
		res <<= 8; res |= data_[ position_ + 3 ];
		res <<= 8; res |= data_[ position_ + 4 ];
		res <<= 8; res |= data_[ position_ + 5 ];
		res <<= 8; res |= data_[ position_ + 6 ];
		res <<= 8; res |= data_[ position_ + 7 ];
	}
	else {
		res = data_[ position_ + 7 ];
		res <<= 8; res |= data_[ position_ + 6 ];
		res <<= 8; res |= data_[ position_ + 5 ];
		res <<= 8; res |= data_[ position_ + 4 ];
		res <<= 8; res |= data_[ position_ + 3 ];
		res <<= 8; res |= data_[ position_ + 2 ];
		res <<= 8; res |= data_[ position_ + 1 ];
		res <<= 8; res |= data_[ position_ + 0 ];
	}
	position_ += 8;
	return res;
}
uint64_t        ByteBuffer::getULong(std::size_t index ){
	if( index+7 >= limit_ ) throw std::overflow_error("ByteBuffer overflow");

	uint64_t res;
	if( orderBe_ ){
		res = data_[ index + 0 ];
		res <<= 8; res |= data_[ index + 1 ];
		res <<= 8; res |= data_[ index + 2 ];
		res <<= 8; res |= data_[ index + 3 ];
		res <<= 8; res |= data_[ index + 4 ];
		res <<= 8; res |= data_[ index + 5 ];
		res <<= 8; res |= data_[ index + 6 ];
		res <<= 8; res |= data_[ index + 7 ];
	}
	else {
		res = data_[ index + 7 ];
		res <<= 8; res |= data_[ index + 6 ];
		res <<= 8; res |= data_[ index + 5 ];
		res <<= 8; res |= data_[ index + 4 ];
		res <<= 8; res |= data_[ index + 3 ];
		res <<= 8; res |= data_[ index + 2 ];
		res <<= 8; res |= data_[ index + 1 ];
		res <<= 8; res |= data_[ index + 0 ];
	}
	return res;
}

}} /* namespace java::nio */
