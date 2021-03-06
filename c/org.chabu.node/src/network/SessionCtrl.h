/*
 * Session.h
 *
 *  Created on: 18.07.2016
 *      Author: fbenoit1
 */

#ifndef SESSION_H_
#define SESSION_H_

#include <memory>
#include <vector>
#include <boost/asio.hpp>
#include "../java/nio/ByteBuffer.h"
#include "../testprot/XferItem.h"
#include "../org/chabu/Chabu.hpp"
#include "../org/chabu/ChabuBuilder.hpp"
#include "../TestChabuChannelListener.hpp"
#include "../TestChabuListener.hpp"

using boost::asio::ip::tcp;
using java::nio::ByteBuffer;
using std::uint8_t;
using std::shared_ptr;
using std::vector;
using testprot::XferItem;
using org::chabu::Chabu;
using org::chabu::ChabuBuilder;


namespace network {

class SessionCtrl : public std::enable_shared_from_this<SessionCtrl>
{
	tcp::socket socket_;
	bool isHostA;
	enum { max_length = 1024*16 };
	uint8_t dataRx_[max_length];
	uint8_t dataTx_[max_length];

	ByteBuffer rx;
	ByteBuffer tx;


	shared_ptr<ChabuBuilder> chabuBuilder;
	shared_ptr<Chabu> chabu;

	TestChabuListener                testListener;
	vector<TestChabuChannelListener> testChannelListener;


public:
	SessionCtrl(tcp::socket socket);
	virtual ~SessionCtrl();

	void start();

private:
	void do_read();

	void handleReceived();
	void do_write();
	shared_ptr<XferItem> process(XferItem& req);
	shared_ptr<XferItem> setup(std::string directoryVersion, std::string hostLabel);
	shared_ptr<XferItem> builderStart( int applicationVersion, std::string applicationProtocolName, int recvPacketSize, int priorityCount) ;
	shared_ptr<XferItem> builderAddChannel(int channel, int priority) ;
	shared_ptr<XferItem> builderBuild() ;
	shared_ptr<XferItem> chabuGetState() ;
	shared_ptr<XferItem> chabuClose() ;
	shared_ptr<XferItem> channelRecv(int channelId, int amount) ;
	shared_ptr<XferItem> channelXmit(int channelId, int amount) ;
	shared_ptr<XferItem> channelEnsureCompleted(int channelId) ;
	shared_ptr<XferItem> channelState(int channelId) ;
	void errorReceiver( std::string msg );

};
}
#endif /* SESSION_H_ */
