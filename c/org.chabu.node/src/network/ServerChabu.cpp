/*
 * ServerChabu.cpp
 *
 *  Created on: 18.07.2016
 *      Author: fbenoit1
 */

#include "ServerChabu.h"

#include <memory>

#include "SessionChabu.h"

namespace network {

ServerChabu::ServerChabu(boost::asio::io_service& io_service, short port)
: acceptor_(io_service, tcp::endpoint(tcp::v4(), port)),
  socket_(io_service)
{
	do_accept();
}

ServerChabu::~ServerChabu() {
}



void ServerChabu::do_accept() {
	acceptor_.async_accept(socket_,
			[this](boost::system::error_code ec) {
		if (!ec) {
			std::make_shared<SessionChabu>(std::move(socket_))->start();
		}

		do_accept();
	});
}
}
