/*
 * ServerChabu.h
 *
 *  Created on: 18.07.2016
 *      Author: fbenoit1
 */

#ifndef NETWORK_SERVERCHABU_H_
#define NETWORK_SERVERCHABU_H_

#include <boost/asio.hpp>

using boost::asio::ip::tcp;
namespace network {
class ServerChabu {

	tcp::acceptor acceptor_;
	tcp::socket socket_;

public:
	ServerChabu(boost::asio::io_service& io_service, short port);
	virtual ~ServerChabu();
private:
	void do_accept();
};

}
#endif /* NETWORK_SERVERCHABU_H_ */
