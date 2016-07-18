/*
 * Session.cpp
 *
 *  Created on: 18.07.2016
 *      Author: fbenoit1
 */

#include "SessionChabu.h"

namespace network {

SessionChabu::SessionChabu(tcp::socket socket)
	: socket_(std::move(socket))
{
}

SessionChabu::~SessionChabu() {
}

void SessionChabu::start() {
	do_read();
}

void SessionChabu::do_read() {

	auto self(shared_from_this());

	socket_.async_read_some(boost::asio::buffer(data_, max_length),
	[this, self](boost::system::error_code ec, std::size_t length)
	{
		if (!ec) {
			do_write(length);
		}
	});
}

void SessionChabu::do_write(std::size_t length) {

	auto self(shared_from_this());
	boost::asio::async_write(socket_, boost::asio::buffer(data_, length),
	[this, self](boost::system::error_code ec, std::size_t /*length*/) {
		if (!ec)
		{
			do_read();
		}
	});
}
}
