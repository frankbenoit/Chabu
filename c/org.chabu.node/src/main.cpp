/*
 * main.cpp
 *
 *  Created on: 16.07.2016
 *      Author: Frank
 */


#include <cstdlib>
#include <iostream>
#include <iomanip>
#include <memory>
#include <utility>
#include <boost/asio.hpp>
#include <boost/format.hpp>

using boost::asio::ip::tcp;

class Session : public std::enable_shared_from_this<Session>
{
	tcp::socket socket_;
	enum { max_length = 1024 };
	char data_[max_length];

public:
	Session(tcp::socket socket)
	: socket_(std::move(socket))
	{
	}

	void start() {
		do_read();
	}

private:
	void do_read() {

		auto self(shared_from_this());

		socket_.async_read_some(boost::asio::buffer(data_, max_length),
				[this, self](boost::system::error_code ec, std::size_t length)
				{
			if (!ec) {
				do_write(length);
			}
				});
	}

	void do_write(std::size_t length) {

		auto self(shared_from_this());
		boost::asio::async_write(socket_, boost::asio::buffer(data_, length),
				[this, self](boost::system::error_code ec, std::size_t /*length*/) {
			if (!ec)
			{
				do_read();
			}
		});
	}

};

class Server {

	tcp::acceptor acceptor_;
	tcp::socket socket_;

public:
	Server(boost::asio::io_service& io_service, short port)
	: acceptor_(io_service, tcp::endpoint(tcp::v4(), port)),
	  socket_(io_service)
	{
		do_accept();
	}

private:
	void do_accept() {
		acceptor_.async_accept(socket_,
				[this](boost::system::error_code ec) {
			if (!ec) {
				std::make_shared<Session>(std::move(socket_))->start();
			}

			do_accept();
		});
	}

};

int main3(int argc, char* argv[]) {
	try {
		if (argc != 2) {
			std::cerr << "Usage: async_tcp_echo_server <port>\n";
			return 1;
		}

		boost::asio::io_service io_service;

		int port = std::atoi(argv[1]);
		Server s(io_service, port );

		std::cout << "server at " << port << std::endl;
		io_service.run();
	}
	catch (std::exception& e) {
		std::cerr << "Exception: " << e.what() << "\n";
	}

	return 0;
}

#include <pugixml.hpp>
#include <fstream>
#include "testprot/XferItem.h"


using std::string;
using std::shared_ptr;
using std::vector;


using namespace testprot;

int main(int argc, char **argv) {
	std::ifstream stream("example.xml");
	pugi::xml_document doc;
	pugi::xml_parse_result result = doc.load(stream);
	try{
		if( result ){
			auto root = doc.root();
			XferItem xferItem;
			xferItem.load(root.child("XferItem"));
			std::cout << "XferItem.toString: "  << xferItem.toString();
		}
		else {
			std::cout << "error: " <<
			result.description() << std::endl;
		}
	}
	catch( std::exception& e ){
		std::cout << "exception: " << e.what();
	}
}


