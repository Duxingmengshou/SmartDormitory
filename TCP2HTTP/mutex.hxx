#pragma once

#include <iostream>
#include <boost/asio.hpp>
#include <boost/array.hpp>
#include <boost/bind.hpp>
#include <string>
#include <boost/thread.hpp>

boost::mutex mux;
std::vector<std::pair<std::string, std::string>> SN2Msg;