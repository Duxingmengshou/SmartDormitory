#include <iostream>
#include <boost/thread.hpp>
#include "HTTPServer.hpp"
#include "TCPServer.hpp"


class SDServer {
public:
    SDHTTPServer sdHS;
    SDTCPServer sdTS;
    boost::thread thHS;
    boost::thread thTS;

    SDServer() : thHS(&SDHTTPServer::run, &sdHS),
                 thTS(&SDTCPServer::run, &sdTS) {
        std::cout << "start" << std::endl;
        thHS.join();
        thTS.join();
    }
};


int main() {

    SDServer sdS;
    return 0;
}