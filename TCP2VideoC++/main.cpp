#include <iostream>
#include <boost/thread.hpp>
#include "TCPServer.hpp"


class SDServer {
public:
    SDTCPServer sdTS;
    boost::thread thTS;

    SDServer() : thTS(&SDTCPServer::run, &sdTS) {
        std::cout << "start!" << std::endl;
        thTS.join();
    }
};


int main() {

    SDServer sdS;
    return 0;
}