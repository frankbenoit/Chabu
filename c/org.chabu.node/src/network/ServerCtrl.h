/*
 * Server.h
 *
 *  Created on: 18.07.2016
 *      Author: fbenoit1
 */

#ifndef SERVER_H_
#define SERVER_H_

#include <boost/asio.hpp>

using boost::asio::ip::tcp;
namespace network {

class ServerCtrl {

	tcp::acceptor acceptor_;
	tcp::socket socket_;

public:
	ServerCtrl(boost::asio::io_service& io_service, short port);
	virtual ~ServerCtrl();
private:
	void do_accept();
};

}

#endif /* SERVER_H_ */
