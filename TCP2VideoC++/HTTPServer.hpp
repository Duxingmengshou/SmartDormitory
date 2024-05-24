#include <boost/beast/core.hpp>
#include <boost/beast/http.hpp>
#include <boost/beast/version.hpp>
#include <boost/asio.hpp>
#include <chrono>
#include <cstdlib>
#include <ctime>
#include <iostream>
#include <memory>
#include <string>
#include <boost/thread.hpp>
#include <boost/asio.hpp>
#include "mutex.hxx"

namespace beast = boost::beast;         // from <boost/beast.hpp>
namespace http = beast::http;           // from <boost/beast/http.hpp>
namespace net = boost::asio;            // from <boost/asio.hpp>
using tcp = boost::asio::ip::tcp;       // from <boost/asio/ip/tcp.hpp>

namespace my_program_state {
    std::size_t
    request_count() {
        static std::size_t count = 0;
        return ++count;
    }
}

class http_connection : public std::enable_shared_from_this<http_connection> {
public:
    explicit http_connection(tcp::socket socket)
            : socket_(std::move(socket)) {
    }

    // Initiate the asynchronous operations associated with the connection.
    void
    start() {
        read_request();
        check_deadline();
    }

private:
    // The socket for the currently connected client.
    tcp::socket socket_;

    // The buffer for performing reads.
    beast::flat_buffer buffer_{8192};

    // The request message.
    http::request<http::dynamic_body> request_;

    // The response message.
    http::response<http::dynamic_body> response_;

    // The timer for putting a deadline on connection processing.
    net::steady_timer deadline_{
            socket_.get_executor(), std::chrono::seconds(60)};

    // Asynchronously receive a complete request message.
    void
    read_request() {
        auto self = shared_from_this();

        http::async_read(
                socket_,
                buffer_,
                request_,
                [self](beast::error_code ec,
                       std::size_t bytes_transferred) {
                    boost::ignore_unused(bytes_transferred);
                    if (!ec)
                        self->process_request();
                });
    }

    // Determine what needs to be done with the request message.
    void
    process_request() {
        response_.version(request_.version());
        response_.keep_alive(false);
//        res.set(http::field::access_control_allow_origin, "*");
        response_.set(http::field::access_control_allow_origin, "*");

        switch (request_.method()) {
            case http::verb::get:
                response_.result(http::status::ok);
                response_.set(http::field::server, "Beast");
                create_response();
                break;

            default:
                // We return responses indicating an error if
                // we do not recognize the request method.
                response_.result(http::status::bad_request);
                response_.set(http::field::content_type, "text/plain");
                beast::ostream(response_.body())
                        << "Invalid request-method '"
                        << std::string(request_.method_string())
                        << "'";
                break;
        }

        write_response();
    }

    // Construct a response message based on the program state.
    void
    create_response() {
        if (request_.target() == "/count") {
            response_.set(http::field::content_type, "text/html");
            beast::ostream(response_.body())
                    << "<html>\n"
                    << "<head><title>Request count</title></head>\n"
                    << "<body>\n"
                    << "<h1>Request count</h1>\n"
                    << "<p>There have been "
                    << my_program_state::request_count()
                    << " requests so far.</p>\n"
                    << "</body>\n"
                    << "</html>\n";
        } else if (request_.target() == "/all") {
            response_.set(http::field::content_type, "text/html");
            mux.lock();
            for (const auto &c: SN2Msg)
                beast::ostream(response_.body()) << c.first << "-";
            mux.unlock();
        } else if (request_.target() == "/all-js") {
            response_.set(http::field::content_type, "text/html");
            mux.lock();
            std::string buf = "";
            for (int i = 0; i < SN2Msg.size(); i++) {
                if (i == SN2Msg.size() - 1) {
                    buf += SN2Msg[i].first;
                    continue;
                }
                buf += SN2Msg[i].first;
                buf += "-";
            }
            beast::ostream(response_.body())
                    << "HTTP/1.1 200 OK\r\n"
                    << "Content-Type: text/plain\r\n"
                    << "Access-Control-Allow-Origin: *\r\n"
                    << "Content-Length: " << buf.size() << "\r\n"
                    << "\r\n"
                    << buf;
            mux.unlock();
        } else {
            std::cout << request_.target() << std::endl;
            mux.lock();
            bool i = true;
            for (auto it = SN2Msg.begin(); it != SN2Msg.end(); it++) {
                if (request_.target() == std::string("/") + it->first) {
                    response_.set(http::field::content_type, "text/html");
                    beast::ostream(response_.body())
                            << it->second;
                    mux.unlock();
                    i = false;
                    return;
                }
            }
            if (i)
                mux.unlock();
            response_.result(http::status::not_found);
            response_.set(http::field::content_type, "text/plain");
            beast::ostream(response_.body()) << "File not found\r\n";
        }
    }

    // Asynchronously transmit the response message.
    void
    write_response() {
        auto self = shared_from_this();

        response_.content_length(response_.body().size());

        http::async_write(
                socket_,
                response_,
                [self](beast::error_code ec, std::size_t) {
                    self->socket_.shutdown(tcp::socket::shutdown_send, ec);
                    self->deadline_.cancel();
                });
    }

    // Check whether we have spent enough time on this connection.
    void
    check_deadline() {
        auto self = shared_from_this();

        deadline_.async_wait(
                [self](beast::error_code ec) {
                    if (!ec) {
                        // Close socket to cancel any outstanding operation.
                        self->socket_.close(ec);
                    }
                });
    }
};

// "Loop" forever accepting new connections.
void
http_server(tcp::acceptor &acceptor, tcp::socket &socket) {
    acceptor.async_accept(socket,
                          [&](beast::error_code ec) {
                              if (!ec)
                                  std::make_shared<http_connection>(std::move(socket))->start();
                              http_server(acceptor, socket);
                          });
}

class SDHTTPServer {
public:
    net::ip::address const address = net::ip::make_address("0.0.0.0");
    unsigned short port = 8089;
    net::io_context ioc{1};
    tcp::acceptor acceptor{ioc, {address, port}};
    tcp::socket socket{ioc};

    SDHTTPServer() = default;

    void run() {
        http_server(acceptor, socket);
        ioc.run();
    }
};