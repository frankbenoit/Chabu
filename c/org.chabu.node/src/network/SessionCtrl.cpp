/*
 * Session.cpp
 *
 *  Created on: 18.07.2016
 *      Author: fbenoit1
 */

#include "SessionCtrl.h"
#include <string>
#include <pugixml.hpp>
#include <boost/format.hpp>
#include <iostream>
#include "../testprot/XferItem.h"
#include "../testprot/ParameterValue.h"

namespace network {

using namespace testprot;
using namespace boost::asio;
using std::string;
using std::cout;
using boost::format;

SessionCtrl::SessionCtrl(tcp::socket socket)
	: socket_(std::move(socket))
	, isHostA( true )
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
		uint32_t packetSize = rx.getUInt(0);
		if( rx.remaining() < 4u + packetSize ){
			// not yet fully received ...
		}
		else {
			rx.position(4);
			new int[12];
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
				tx.put( str.length() );
				tx.put( static_cast<const void*>(str.c_str()), 0, str.length() );
				tx.flip();
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
		resp->setParameters( std::vector<shared_ptr<Parameter>>{
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
	res->setParameters( std::vector<shared_ptr<Parameter>>{
		shared_ptr<Parameter>{new ParameterValue("Implementation", "Java")},
		shared_ptr<Parameter>{new ParameterValue("ChabuProtocolVersion", "0")}//ChabuBuilder.getChabuVersion() }
	});
	return res;
}

shared_ptr<XferItem> SessionCtrl::builderStart( int applicationVersion, std::string applicationProtocolName, int recvPacketSize, int priorityCount) {
	//System.out.printf("builderStart( %s, %s, %s, %s)\n",  applicationVersion, applicationProtocolName, recvPacketSize, priorityCount );
	//builder = ChabuBuilder.start( applicationVersion, applicationProtocolName, recvPacketSize, priorityCount, xmitRequestListener );
	return std::make_shared<XferItem>();
}

shared_ptr<XferItem> SessionCtrl::builderAddChannel(int channel, int priority) {
	//System.out.printf("builderAddChannel( %s, %s)\n",  channel, priority );
//	chabuChannelUsers.ensureCapacity(channel+1);
//	while( chabuChannelUsers.size() < channel+1 ){
//		chabuChannelUsers.add(null);
//	}
//	chabuChannelUsers.set( channel, new TestChannelUser( isHostA, this::errorReceiver ) );
//	builder.addChannel( channel, priority, chabuChannelUsers.get(channel));
	return std::make_shared<XferItem>();
}

shared_ptr<XferItem> SessionCtrl::builderBuild() {
	//System.out.printf("builderBuild()\n");
//	chabu = builder.build();
//	testServer.setChabu(chabu);
//	builder = null;
	return std::make_shared<XferItem>();
}

shared_ptr<XferItem> SessionCtrl::chabuGetState() {
//	int channelCount = chabu.getChannelCount();
//	Parameter[] channelInfo = new Parameter[ channelCount ];
//	for( int channelId = 0; channelId < chabu.getChannelCount(); channelId++ ){
//		ChabuChannel channel = chabu.getChannel(channelId);
//		channelInfo[channelId] = new ParameterWithChilds(Integer.toString(channelId), new Parameter[]{
//				new ParameterValue("recvPosition", channel.getRecvPosition()),
//				new ParameterValue("recvLimit", channel.getRecvLimit()),
//				new ParameterValue("xmitPosition", channel.getXmitPosition()),
//				new ParameterValue("xmitLimit", channel.getXmitLimit()),
//		});
//	}
	auto xi = std::make_shared<XferItem>();
//	xi->setParameters(new Parameter[]{
//			new ParameterValue("channelCount", channelCount),
//			new ParameterWithChilds("channel", channelInfo),
//			new ParameterValue("toString", chabu.toString())
//	});
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
