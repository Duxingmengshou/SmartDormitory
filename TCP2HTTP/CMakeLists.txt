cmake_minimum_required(VERSION 3.26)
project(SmartDormitoryTCP2HTTP)

set(CMAKE_CXX_STANDARD 17)

# 设置boost库lib的路径
link_directories("D:/boost/msvc_bin_complete/lib")

# 设置头文件路径
include_directories("D:/boost/msvc_bin/include/boost-1_84")

add_executable(SmartDormitoryTCP2HTTP
        main.cpp
        HTTPServer.hpp
        TCPServer.hpp
        mutex.hxx
)
