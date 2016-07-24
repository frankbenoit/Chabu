/*
 * main.cpp
 *
 *  Created on: 16.07.2016
 *      Author: Frank
 */
#ifdef  _MSC_VER
#define _CRTDBG_MAP_ALLOC 
#include <stdlib.h> 
#include <crtdbg.h>
#endif

#include <cstdlib>
#include <iostream>
#include <iomanip>
#include <memory>
#include <utility>
#include <boost/asio.hpp>
#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>

#include "network/ServerCtrl.h"
#include "network/ServerChabu.h"

int main(int argc, char* argv[]) {
#ifdef  _MSC_VER
	_CrtSetDbgFlag(_CRTDBG_ALLOC_MEM_DF | _CRTDBG_LEAK_CHECK_DF);
#endif
	try {
		if (argc != 2) {
			std::cerr << "Usage: async_tcp_echo_server <port>\n";
			return 1;
		}

		boost::asio::io_service io_service;

		int port = boost::lexical_cast<int>(argv[1]);

		network::ServerCtrl  serverCtrl (io_service, port );

		std::cout << "server at " << port << std::endl;
		io_service.run();
	}
	catch (std::exception& e) {
		std::cerr << "Exception: " << e.what() << "\n";
	}
	std::cout << std::endl;
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

int main2(int argc, char **argv) {
#ifdef  _MSC_VER
	_CrtSetDbgFlag(_CRTDBG_ALLOC_MEM_DF | _CRTDBG_LEAK_CHECK_DF);
#endif
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
	return 0;
}


