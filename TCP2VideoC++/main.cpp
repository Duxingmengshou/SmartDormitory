#include <iostream>
#include <boost/thread.hpp>
#include "TCPServer.hpp"


class TCPMJPGStreamerServer {
public:
    TCPServer TS;
    boost::thread thTS;

    TCPMJPGStreamerServer() : thTS(&TCPServer::run, &TS) {
        std::cout << "start!" << std::endl;
        thTS.join();
    }
};


int main() {
    TCPMJPGStreamerServer sdS;
    return 0;
}