/*
 * Session.cpp
 *
 *  Created on: 18.07.2016
 *      Author: fbenoit1
 */

#include "SessionCtrl.h"

namespace network {

SessionCtrl::SessionCtrl(tcp::socket socket)
	: socket_(std::move(socket))
{
}

SessionCtrl::~SessionCtrl() {
}

void SessionCtrl::start() {
	do_read();
}

void SessionCtrl::do_read() {

	auto self(shared_from_this());

	socket_.async_read_some(boost::asio::buffer(data_, max_length),
	[this, self](boost::system::error_code ec, std::size_t length)
	{
		if (!ec) {
			do_write(length);
		}
	});
}

void SessionCtrl::do_write(std::size_t length) {

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
