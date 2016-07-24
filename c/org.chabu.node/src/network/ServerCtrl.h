/*
 * Server.h
 *
 *  Created on: 18.07.2016
 *      Author: fbenoit1
 */

#ifndef SERVER_H_
#define SERVER_H_

#include <boost/asio.hpp>
#include "ServerChabu.h"

using boost::asio::ip::tcp;
namespace network {

class ServerCtrl {

	tcp::acceptor acceptor_;
	tcp::socket socket_;
	ServerChabu serverChabu_;

public:
	ServerCtrl(boost::asio::io_service& io_service, short port);
	virtual ~ServerCtrl();
private:
	void do_accept();
};

}

#endif /* SERVER_H_ */
