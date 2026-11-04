/*
 * GatedResponder.hpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_TESTS_GATED_RESPONDER_HPP
#define CORE_TESTS_GATED_RESPONDER_HPP

// Helpers for tests only; this header pulls in gtest.

#include <gtest/gtest.h>

#include <chrono>
#include <condition_variable>
#include <functional>
#include <memory>
#include <mutex>
#include <string>
#include <thread>

#include <boost/asio.hpp>

namespace rstudio {
namespace core {
namespace tests {

// One-shot signal from an io_context thread (typically a client or proxy
// callback) to a thread blocked waiting on it, such as GatedResponder's.
class Gate
{
public:
   void open()
   {
      std::lock_guard<std::mutex> lock(mutex_);
      open_ = true;
      cv_.notify_all();
   }

   // Bounded, so a gate that never opens fails the test rather than hanging it.
   // Returns false if the wait timed out.
   bool wait()
   {
      std::unique_lock<std::mutex> lock(mutex_);
      return cv_.wait_for(lock, std::chrono::seconds(2), [this]() { return open_; });
   }

private:
   std::mutex mutex_;
   std::condition_variable cv_;
   bool open_ = false;
};

// A blocking upstream HTTP server on its own thread, for tests that need to
// control exactly when the rest of a response arrives. It accepts a single
// connection, reads the request headers, and writes `head` -- the status line
// and headers, plus the start of the body if the test wants one. Once `pGate`
// opens it hands the socket to `finish`, which ends the response however the
// test needs (writing the rest of the body, resetting, stalling), and then
// closes the socket.
class GatedResponder
{
public:
   typedef std::function<void(boost::asio::ip::tcp::socket&)> Finish;

   GatedResponder(const std::string& head, std::shared_ptr<Gate> pGate, Finish finish)
      : acceptor_(ioc_, boost::asio::ip::tcp::endpoint(boost::asio::ip::make_address("127.0.0.1"), 0)),
        head_(head),
        pGate_(pGate),
        finish_(finish)
   {
   }

   ~GatedResponder()
   {
      if (thread_.joinable())
         thread_.join();
   }

   unsigned short port() { return acceptor_.local_endpoint().port(); }

   void start()
   {
      thread_ = std::thread([this]() { run(); });
   }

private:
   void run()
   {
      boost::system::error_code ec;
      boost::asio::ip::tcp::socket socket(ioc_);
      acceptor_.accept(socket, ec);
      if (ec)
         return;

      boost::asio::streambuf request;
      boost::asio::read_until(socket, request, "\r\n\r\n", ec);
      if (ec)
         return;

      boost::asio::write(socket, boost::asio::buffer(head_), ec);
      if (ec)
         return;

      // finish regardless, so a client still waiting on the response sees it end
      if (!pGate_->wait())
         ADD_FAILURE() << "GatedResponder timed out waiting for its gate to open";

      finish_(socket);
      socket.close(ec);
   }

   boost::asio::io_context ioc_;
   boost::asio::ip::tcp::acceptor acceptor_;
   std::string head_;
   std::shared_ptr<Gate> pGate_;
   Finish finish_;
   std::thread thread_;
};

} // namespace tests
} // namespace core
} // namespace rstudio

#endif // CORE_TESTS_GATED_RESPONDER_HPP
