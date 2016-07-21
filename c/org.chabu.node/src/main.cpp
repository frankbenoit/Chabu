/*
 * main.cpp
 *
 *  Created on: 16.07.2016
 *      Author: Frank
 */
#define _CRTDBG_MAP_ALLOC 
#include <stdlib.h> 
#include <crtdbg.h>

#include <cstdlib>
#include <iostream>
#include <iomanip>
#include <memory>
#include <utility>
#include <boost/asio.hpp>
#include <boost/format.hpp>

#include "network/ServerCtrl.h"
#include "network/ServerChabu.h"

int main3(int argc, char* argv[]) {
	try {
		if (argc != 2) {
			std::cerr << "Usage: async_tcp_echo_server <port>\n";
			return 1;
		}

		boost::asio::io_service io_service;

		int port = std::atoi(argv[1]);

		network::ServerCtrl  serverCtrl (io_service, port+0 );
		network::ServerChabu serverChabu(io_service, port+1 );

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

#include <malloc.h>

using std::string;
using std::shared_ptr;
using std::vector;


using namespace testprot;
//static struct mallinfo infoStartup;
//static void test(){
//	struct mallinfo info = mallinfo();
//
//	std::cout << boost::format("total allocated space:  %s bytes\n") % (int)( info.uordblks - infoStartup.uordblks);
//	std::cout << boost::format("total free space:       %s bytes\n") % (int)( info.fordblks - infoStartup.fordblks);
//
//}
static void test() {
}
int main(int argc, char **argv) {
	_CrtSetDbgFlag(_CRTDBG_ALLOC_MEM_DF | _CRTDBG_LEAK_CHECK_DF);
	//_CrtMemState s1; 
	//_CrtMemCheckpoint(&s1);
	//_CrtSetReportMode(_CRT_ERROR, _CRTDBG_MODE_DEBUG);
	{
		auto v = new int[12];
		v[0] = 0x1234;
		delete [] v;
	}
	//_CrtMemDumpStatistics(&s1);
	{
		std::ifstream stream("example.xml");
		pugi::xml_document doc;
		pugi::xml_parse_result result = doc.load(stream);
		try{
			if( result ){
				auto root = doc.root();
				XferItem xferItem;
				xferItem.load(root.child("XferItem"));
				std::cout << "XferItem.toString: "  << xferItem.toString() << std::endl;
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
	//_CrtDumpMemoryLeaks();
}


