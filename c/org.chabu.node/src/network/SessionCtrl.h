/*
 * Session.h
 *
 *  Created on: 18.07.2016
 *      Author: fbenoit1
 */

#ifndef SESSION_H_
#define SESSION_H_

#include <boost/asio.hpp>

using boost::asio::ip::tcp;
namespace network {

class SessionCtrl : public std::enable_shared_from_this<SessionCtrl>
{
	tcp::socket socket_;
	enum { max_length = 1024 };
	char data_[max_length];

public:
	SessionCtrl(tcp::socket socket);
	virtual ~SessionCtrl();

	void start();

private:
	void do_read();

	void do_write(std::size_t length);

};
}
#endif /* SESSION_H_ */
