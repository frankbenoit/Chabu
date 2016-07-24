/*
 * Server.cpp
 *
 *  Created on: 18.07.2016
 *      Author: fbenoit1
 */

#include "ServerCtrl.h"

#include <memory>

#include "SessionCtrl.h"

namespace network {
ServerCtrl::ServerCtrl(boost::asio::io_service& io_service, short port)
: acceptor_(io_service, tcp::endpoint(tcp::v4(), port))
, socket_(io_service)
, serverChabu_(io_service, port+1 )
{
	do_accept();
}

ServerCtrl::~ServerCtrl() {
//	serverChabu_.acceptor_.close()
}



void ServerCtrl::do_accept() {
	acceptor_.async_accept(socket_,
			[this](boost::system::error_code ec) {
		if (!ec) {
			std::make_shared<SessionCtrl>(std::move(socket_))->start();
		}

		do_accept();
	});
}
}
