#pragma once

#include <iostream>
#include <vector>
#include <string>
#include <list>
#include <boost/asio.hpp>
#include <boost/system.hpp>
#include <boost/bind.hpp>
#include <boost/thread.hpp>
#include "mjpeg_streamer.hpp"

// for convenience
using MJPEGStreamer = nadjieb::MJPEGStreamer;

class SDTCPServer {
    typedef boost::asio::ip::tcp::acceptor acceptor;
    typedef boost::asio::ip::tcp::endpoint endpoint;
    typedef boost::asio::ip::tcp::socket socket;
    typedef boost::asio::ip::tcp::iostream iostream_socket;
    typedef boost::asio::io_service io_service;
    typedef std::shared_ptr<socket> shared_socket_ptr;
    typedef socket *socket_ptr;
public:
    io_service m_ios;
    acceptor m_acceptor;
    std::list<std::pair<std::string, shared_socket_ptr >> sessions;
    MJPEGStreamer streamer;

public:

    SDTCPServer() : m_acceptor(m_ios,
                               endpoint(
                                       boost::asio::ip::tcp::v4(), 8083
                               )
    ) {
        streamer.start(8103);
        accept();
    }

    void run() {
        m_ios.run();
    }


    void accept() {
        shared_socket_ptr sock = std::make_shared<socket>(m_ios);
        m_acceptor.async_accept(*sock,
                                boost::bind(&SDTCPServer::accept_handler, this, boost::asio::placeholders::error,
                                            sock));
    }

    void accept_handler(const boost::system::error_code &ec, shared_socket_ptr sock) {
        if (ec) {
            return;
        }
        std::cout << "客户端:";
        std::cout << sock->remote_endpoint().address().to_string() << ":" <<
                  sock->remote_endpoint().port() <<
                  "加入" <<
                  std::endl;
        this->read_sn(sock);
        this->read(sock);
        this->accept();
    }


    void read(shared_socket_ptr sock) {
        auto buf = std::make_shared<std::vector<char>>(1048576);
        sock->async_read_some(boost::asio::buffer(*buf),
                              [sock, buf, this](const boost::system::error_code &error,
                                                std::size_t bytes_transferred) {
                                  read_handler(error, bytes_transferred, buf, sock);
                              });
    }


    void read_handler(const boost::system::error_code &error,
                      std::size_t bytes_transferred,
                      std::shared_ptr<std::vector<char>> buffer,
                      shared_socket_ptr sock) {
        if (!error) {
            // std::cout << "Received " << bytes_transferred << " bytes\n";
            // 处理接收到的数据
            std::string msg = std::string(buffer->begin(), buffer->begin() + bytes_transferred);
            if (!streamer.isRunning())
                streamer.start(8103);
            for (auto &session: sessions) {
                if (session.second == sock) {
                    streamer.publish(std::string("/video/") + session.first, msg);
                }
            }
            read(sock);
        } else {
            // std::cerr << "Error: " << error.message() << std::endl;
            std::cerr << "客户端:" <<
                      sock->remote_endpoint().address().to_string() << ":" <<
                      sock->remote_endpoint().port() <<
                      "连接已经断开" <<
                      std::endl;
            for (auto it = sessions.begin(); it != sessions.end(); ++it) {
                if (it->second == sock) {
                    sessions.erase(it);
                    streamer.stop();
                    break; // 找到并删除会话后退出循环
                }
            }
            std::cout << "当前会话：" << std::endl;
            if (sessions.empty())
                std::cout << "没有会话" << std::endl;
            else
                for (auto c: sessions) {
                    std::cout << c.first << "-" << c.second << std::endl;
                }
        }
    }

    void read_sn(shared_socket_ptr sock) {
        auto buf = std::make_shared<std::vector<char>>(1024);
        sock->async_read_some(boost::asio::buffer(*buf),
                              [sock, buf, this](const boost::system::error_code &error,
                                                std::size_t bytes_transferred) {
                                  read_sn_handler(error, bytes_transferred, buf, sock);
                              });
    }

    void read_sn_handler(const boost::system::error_code &error,
                         std::size_t bytes_transferred,
                         std::shared_ptr<std::vector<char>> buffer,
                         shared_socket_ptr sock) {
        if (!error) {
            std::string sn = std::string(buffer->begin(), buffer->begin() + bytes_transferred);
            sessions.push_back(std::make_pair(sn, sock));
            std::cout << sn << std::endl;
        }
    }

};