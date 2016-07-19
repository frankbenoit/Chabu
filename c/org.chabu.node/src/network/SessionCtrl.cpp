/*
 * Session.cpp
 *
 *  Created on: 18.07.2016
 *      Author: fbenoit1
 */

#include "SessionCtrl.h"
#include <string>
#include <pugixml.hpp>
#include "../testprot/XferItem.h"

namespace network {

using namespace testprot;
using namespace boost::asio;
using std::string;

SessionCtrl::SessionCtrl(tcp::socket socket)
	: socket_(std::move(socket))
	, rx( dataRx_, sizeof(dataRx_) )
	, tx( dataTx_, sizeof(dataTx_) )
{
}

SessionCtrl::~SessionCtrl() {
}

void SessionCtrl::start() {
	do_read();
}

void SessionCtrl::do_read() {

	auto self(shared_from_this());

	socket_.async_read_some(boost::asio::buffer(rx.arrayPtrAtPosition(), rx.remaining()),
	[this, self](boost::system::error_code ec, std::size_t length)
	{
		if (!ec) {
			rx.positionIncrease(length);
			handleReceived();
			do_read();
		}
	});
}

void SessionCtrl::handleReceived(){

	rx.flip();
	if( rx.remaining() >= 4 ){
		uint32_t packetSize = rx.getUInt();
		if( rx.remaining() < 4u + packetSize ){
			// not yet fully received ...
			rx.position(0);
		}
		else {
			new int[12];
			std::string xml( reinterpret_cast<char*>(rx.arrayPtrAtPosition()), packetSize );
			pugi::xml_document doc;
			pugi::xml_parse_result result = doc.load_string(xml.c_str());
			if( result ){
				XferItem xferItem;
				xferItem.load(doc.root().child("XferItem"));
			}
			else {
				// xml problem
			}
		}
	}
	rx.compact();

	//do_write(length);
}

void SessionCtrl::do_write(std::size_t length) {

	auto self(shared_from_this());
	boost::asio::async_write(socket_, boost::asio::buffer(tx.arrayPtrAtPosition(), tx.remaining() ),
	[this, self](boost::system::error_code ec, std::size_t length) {
		if (!ec)
		{
			tx.positionIncrease(length);
			do_read();
		}
	});
}
}
