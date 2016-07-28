/*
 * Session.cpp
 *
 *  Created on: 18.07.2016
 *      Author: fbenoit1
 */

#include "SessionCtrl.h"
#include <string>
#include <pugixml.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/format.hpp>
#include <iostream>
#include "../testprot/XferItem.h"
#include "../testprot/ParameterValue.h"
#include "../testprot/ParameterWithChilds.h"
#include "Chabu.h"
#include "../org/chabu/ChabuListener.hpp"
#include "../org/chabu/ChabuBuilder.hpp"
#include "../TestChabuListener.hpp"

namespace network {

using namespace testprot;
using namespace boost::asio;
using std::string;
using std::cout;
using std::cerr;
using std::endl;
using boost::format;

SessionCtrl::SessionCtrl(tcp::socket socket)
	: socket_(std::move(socket))
	, isHostA( true )
	, rx( dataRx_, sizeof(dataRx_) )
	, tx( dataTx_, sizeof(dataTx_) )
{
	rx.order(java::nio::ByteOrder::big_endian);
	tx.order(java::nio::ByteOrder::big_endian);
	tx.limit(0);
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
		uint32_t packetSize = rx.getUInt(0);
		if( rx.remaining() < 4u + packetSize ){
			// not yet fully received ...
		}
		else {
			rx.position(4);
			std::string xml( reinterpret_cast<char*>(rx.arrayPtrAtPosition()), packetSize );
			pugi::xml_document doc;
			cout << format("Request: %s\n") % xml;
			pugi::xml_parse_result result = doc.load_string(xml.c_str());
			if( result ){
				XferItem xferItem;
				xferItem.load(doc.root().child("XferItem"));
				auto res = process( xferItem );

				pugi::xml_document resDoc;
				std::stringstream sstr;
				res->encodeInto( resDoc.root() );
				resDoc.save(sstr);
				auto str = sstr.str();
				str.length();
				cout << format("Response: %s") % str << std::endl;

				tx.compact();
				tx.putInt( str.length() );
				tx.put( static_cast<const void*>(str.c_str()), 0, str.length() );
				tx.flip();
			}
			else {
				// xml problem
			}
		}
	}
	rx.compact();

	do_write();
}

void SessionCtrl::do_write() {

	auto self(shared_from_this());
	boost::asio::async_write(socket_, boost::asio::buffer(tx.arrayPtrAtPosition(), tx.remaining() ),
	[this, self](boost::system::error_code ec, std::size_t length) {
		if (!ec)
		{
			tx.positionIncrease(length);
			if( !tx.hasRemaining() ){
				cout << "start reading" << std::endl;
				do_read();
			}
		}
	});
}


shared_ptr<XferItem> SessionCtrl::process(XferItem& req) {
	int callIndex = req.callIndex;
	string name = req.name;
	shared_ptr<XferItem> resp{};

	cout << format("command: %s\n") % name;

	if( name == "Setup" ){
		resp = setup(
				req.getValueString("ChabuTestDirectorVersion"   ),
				req.getValueString("NodeLabel"   ));

	}
	else if( name == "ChabuBuilder.start" ){
		resp = builderStart(
				req.getValueInt("ApplicationVersion"),
				req.getValueString("ApplicationProtocolName"   ),
				req.getValueInt   ("RecvPacketSize"        ),
				req.getValueInt   ("PriorityCount"     ));
	}
	else if( name == "ChabuBuilder.addChannel" ){
		resp = builderAddChannel(
				req.getValueInt("Channel"   ),
				req.getValueInt("Priority"  ));
	}
	else if( name == "ChabuBuilder.build" ){
		resp = builderBuild();
	}
	else if( name == "Chabu.close" ){
		resp = chabuClose();
	}
	else if( name == "GetState" ){
		resp = chabuGetState();
	}
	else if( name == "Channel.recv" ){
		resp = channelRecv(
				req.getValueInt("Channel"),
				req.getValueInt("Amount" ));
	}
	else if( name == "Channel.xmit" ){
		resp = channelXmit(
				req.getValueInt("Channel"),
				req.getValueInt("Amount" ));
	}
	else if( name == "Channel.ensureCompleted" ){
		resp = channelEnsureCompleted(
				req.getValueInt("Channel"));
	}
	else if( name == "Channel.state" ){
		resp = channelState(
				req.getValueInt("Channel"));
	}
	else if( name == "Connect" ){
//		testServer.connectSync(req.getValueString("HostName"), req.getValueInt("Port"));
		resp = std::make_shared<XferItem>();
	}
	else if( name == "Close" ){
//		testServer.close();
		resp = std::make_shared<XferItem>();
	}
	else if( name == "ExpectClose" ){
//		testServer.expectClose();
		resp = std::make_shared<XferItem>();
	}
	else if( name == "EnsureClosed" ){
//		testServer.ensureClosed();
		resp = std::make_shared<XferItem>();
	}
	else {
		resp = std::make_shared<XferItem>();
		resp->addParameters( std::vector<shared_ptr<Parameter>>{
			shared_ptr<Parameter>{new ParameterValue("IsError", "1")},
			shared_ptr<Parameter>{new ParameterValue("Message", "Unknown Command")}
		});
	}
	resp->category = Category::RES;
	resp->callIndex = callIndex;
	resp->name = name;
	resp->addParameter( "NanoTime", 0L  ); // System.nanoTime()
	return resp;
}

shared_ptr<XferItem> SessionCtrl::setup(string directoryVersion, string hostLabel) {
	//System.out.printf("setup( %s, %s)\n",  directoryVersion, hostLabel );
	this->isHostA = ( "A" == hostLabel );
	shared_ptr<XferItem> res{new XferItem()};
	res->addParameters( std::vector<shared_ptr<Parameter>>{
		shared_ptr<Parameter>{new ParameterValue("Implementation", "C")},
		shared_ptr<Parameter>{new ParameterValue("ChabuProtocolVersion", boost::lexical_cast<string>(Chabu_ProtocolVersion))}
	});
	return res;
}

shared_ptr<XferItem> SessionCtrl::builderStart( int applicationVersion, std::string applicationProtocolName, int recvPacketSize, int priorityCount) {
	//System.out.printf("builderStart( %s, %s, %s, %s)\n",  applicationVersion, applicationProtocolName, recvPacketSize, priorityCount );
	chabuBuilder = std::make_shared<ChabuBuilder>( applicationVersion, applicationProtocolName, testListener );
	chabuBuilder->setRecvPacketSize(recvPacketSize);
	return std::make_shared<XferItem>();
}

shared_ptr<XferItem> SessionCtrl::builderAddChannel(int channelId, int priority) {
	//System.out.printf("builderAddChannel( %s, %s)\n",  channel, priority );

	if( channelId != (int)testChannelListener.size() ){
		cerr << format("SessionCtrl::builderAddChannel %s %s") % channelId % testChannelListener.size() << endl;
	}

	testChannelListener.push_back( TestChabuChannelListener{channelId} );
	auto listener = testChannelListener.at( testChannelListener.size() - 1);

	chabuBuilder->addChannel( channelId, priority, listener );
	return std::make_shared<XferItem>();
}

shared_ptr<XferItem> SessionCtrl::builderBuild() {
	//System.out.printf("builderBuild()\n");
	chabu = chabuBuilder->build();
//	testServer.setChabu(chabu);
	chabuBuilder = shared_ptr<ChabuBuilder>{};
	return std::make_shared<XferItem>();
}

shared_ptr<XferItem> SessionCtrl::chabuGetState() {
	Chabu& chabu = *this->chabu;
	size_t channelCount = chabu.getChannelCount();

	auto xi = std::make_shared<XferItem>();
	xi->addParameter("channelCount", static_cast<int64_t>(channelCount));
	ParameterWithChilds * channels = new ParameterWithChilds("channel");
	xi->addParameter(shared_ptr<Parameter>(channels));
	xi->addParameter("toString", chabu.toString());

	for( size_t channelId = 0; channelId < channelCount; channelId++ ){

		ChabuChannel& channel = chabu.getChannel(channelId);

		ParameterWithChilds * params = new ParameterWithChilds{ boost::lexical_cast<string>(channelId) };

		params->addParameterValue( "recvPosition", channel.getRecvPosition());
		params->addParameterValue( "recvLimit",    channel.getRecvLimit());
		params->addParameterValue( "xmitPosition", channel.getXmitPosition());
		params->addParameterValue( "xmitLimit",    channel.getXmitLimit());

		channels->addParameter( shared_ptr<Parameter>( params ) );
	}
	return xi;
}

shared_ptr<XferItem> SessionCtrl::chabuClose() {
	//System.out.printf("chabuClose()%n");
	//System.out.printf("Ch   | Recved   | Xmitted%n");
	//System.out.printf("-----|----------|----------%n");
	//for( TestChannelUser user : chabuChannelUsers ){
	//	System.out.printf("[% 2d] | % 8d | % 8d%n", user.getChannelId(), user.getSumRecved(), user.getSumXmitted());
	//}
	//System.out.printf("---------------------------%n");
//	chabu = null;
//	chabuChannelUsers.clear();
	return std::make_shared<XferItem>();
}

shared_ptr<XferItem> SessionCtrl::channelRecv(int channelId, int amount) {
	//System.out.printf("channelRecv( %s, %s)\n",  channelId, amount );
//	TestChannelUser user = chabuChannelUsers.get(channelId);
//	user.addRecvAmount(amount);
	return std::make_shared<XferItem>();
}

shared_ptr<XferItem> SessionCtrl::channelXmit(int channelId, int amount) {
	//System.out.printf("channelXmit( %s, %s)\n", channelId, amount );
//	TestChannelUser user = chabuChannelUsers.get(channelId);
//	user.addXmitAmount(amount);
	return std::make_shared<XferItem>();
}
shared_ptr<XferItem> SessionCtrl::channelEnsureCompleted(int channelId) {
	//System.out.printf("channelEnsureCompleted( %s )\n", channelId );
//	TestChannelUser user = chabuChannelUsers.get(channelId);
//	user.ensureCompleted();
	return std::make_shared<XferItem>();
}
shared_ptr<XferItem> SessionCtrl::channelState(int channelId) {
//	TestChannelUser user = chabuChannelUsers.get(channelId);
//	return user.getState();
	return std::make_shared<XferItem>();
}

void SessionCtrl::errorReceiver( std::string msg ){
//	if( firstError == null ){
//		System.out.println("Error: "+msg);
//		firstError = msg;
//	}
}

}
