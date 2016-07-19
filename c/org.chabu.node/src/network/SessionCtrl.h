/*
 * Session.h
 *
 *  Created on: 18.07.2016
 *      Author: fbenoit1
 */

#ifndef SESSION_H_
#define SESSION_H_

#include <boost/asio.hpp>
#include "../java/nio/ByteBuffer.h"

using boost::asio::ip::tcp;
using java::nio::ByteBuffer;
using std::uint8_t;

namespace network {

class SessionCtrl : public std::enable_shared_from_this<SessionCtrl>
{
	tcp::socket socket_;
	enum { max_length = 1024*16 };
	uint8_t dataRx_[max_length];
	uint8_t dataTx_[max_length];

	ByteBuffer rx;
	ByteBuffer tx;

public:
	SessionCtrl(tcp::socket socket);
	virtual ~SessionCtrl();

	void start();

private:
	void do_read();

	void handleReceived();
	void do_write(std::size_t length);

};
}
#endif /* SESSION_H_ */
