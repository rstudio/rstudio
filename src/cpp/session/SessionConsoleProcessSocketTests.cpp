/*
 * SessionConsoleProcessSocketTests.cpp
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <session/SessionConsoleProcessSocket.hpp>

#include <boost/bind.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/make_shared.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/lexical_cast.hpp>

#include <websocketpp/config/asio_no_tls_client.hpp>
#include <websocketpp/client.hpp>

#include <tests/TestThat.hpp>

namespace rstudio {
namespace session {
namespace console_process {

using namespace console_process;

namespace {

// send this text and the receiver will close the websocket
const std::string kCloseMessage = "CLOSE_CONNECTION";

// convenience wrapper for ConsoleProcessSocket
class SocketHarness : public boost::enable_shared_from_this<SocketHarness>
{
public:
   SocketHarness() {}
   ~SocketHarness() {}

   bool ensureServerRunning()
   {
      core::Error err = socket_.ensureServerRunning();
      return (!err);
   }

   void onSocketClosed()
   {
      LOG_ERROR_MESSAGE("onClosed");
   }

   void onReceivedInput(const std::string& input)
   {
      if (!input.compare(kCloseMessage))
      {
         socket_.stopListening("abcd");
         socket_.sendText("abcd", kCloseMessage);
         return;
      }
      receivedInput_.append(input);
   }

   bool listen(const std::string& terminalHandle,
               const ConsoleProcessSocketConnectionCallbacks& callbacks)
   {
      core::Error err = socket_.listen(terminalHandle, callbacks);
      return (!err);
   }

   bool stopListening(const std::string& terminalHandle)
   {
      core::Error err = socket_.stopListening(terminalHandle);
      return (!err);
   }

   int port() { return socket_.port(); }
   std::string receivedInput() { return receivedInput_; }

private:
   ConsoleProcessSocket socket_;
   std::string receivedInput_;
};

typedef websocketpp::client<websocketpp::config::asio_client> client;
typedef client::message_ptr message_ptr;

// Client-side websocket utility for testing ConsoleProcessSocket server.
class SocketClient
{
public:
   SocketClient(const std::string& handle, boost::shared_ptr<SocketHarness> pServerSocket)
      :
        handle_(handle),
        pServerSocket_(pServerSocket),
        gotOpened_(false),
        gotClosed_(false),
        clientRunning_(false)
   {}

   ~SocketClient()
   {
      try
      {
         disconnectFromServer();
      }
      catch (...) {}
   }

   void on_message(client* c, websocketpp::connection_hdl hdl, message_ptr msg)
   {
      std::string message = msg->get_payload();
   }

   bool connectToServer()
   {
      using websocketpp::lib::placeholders::_1;
      using websocketpp::lib::placeholders::_2;
      using websocketpp::lib::bind;

      try {
         std::string uri = "http://localhost:" +
               boost::lexical_cast<std::string>(pServerSocket_->port()) +
               "/terminal/" + handle_ + "/";

         // Set logging to be pretty verbose (everything except message payloads)
         client_.set_access_channels(websocketpp::log::alevel::all);
         client_.clear_access_channels(websocketpp::log::alevel::frame_payload);

         // Initialize ASIO
         client_.init_asio();

         // Register our message handler
         client_.set_message_handler(bind(&SocketClient::on_message, this,
                                          &client_, ::_1, ::_2));

         websocketpp::lib::error_code ec;
         client::connection_ptr con = client_.get_connection(uri, ec);
         if (ec)
            return false;

         // Note that connect here only requests a connection. No network
         // messages are exchanged until the event loop starts running in
         // the thread.
         client_.connect(con);

         // Start the ASIO io_service run loop in a separate thread; allows
         // us to perform multiple operations as part of the test.
         core::thread::safeLaunchThread(
                  boost::bind(&SocketClient::watchSocket, this),
                  &clientSocketThread_);
      }
      catch (websocketpp::exception const & e)
      {
         return false;
      }
      return true;
   }

   bool disconnectFromServer()
   {
      if (clientRunning_)
      {
         client_.stop();
         clientRunning_ = false;
         clientSocketThread_.join();
      }
      return true;
   }

   void onReceivedInput(const std::string& input)
   {
      input_.append(input);
   }

   void onConnectionOpened()
   {
//      scoped_lock guard(lock_);
      gotOpened_ = true;
      clientRunning_ = true;
   }

   void onConnectionClosed()
   {
//      scoped_lock guard(lock_);
      gotClosed_ = true;
      clientRunning_ = false;
   }

   ConsoleProcessSocketConnectionCallbacks createConsoleProcessSocketConnectionCallbacks()
   {
      ConsoleProcessSocketConnectionCallbacks cb;
      cb.onReceivedInput = boost::bind(&SocketClient::onReceivedInput, this, _1);
      cb.onConnectionOpened = boost::bind(&SocketClient::onConnectionOpened, this);
      cb.onConnectionClosed = boost::bind(&SocketClient::onConnectionClosed, this);
      return cb;
   }

   bool listen()
   {
      return pServerSocket_->listen(handle_, createConsoleProcessSocketConnectionCallbacks());
   }

   bool stopListening()
   {
      return pServerSocket_->stopListening(handle_);
   }

   bool sendMessage(const std::string& msg)
   {
      return true;
   }

   std::string getInput() { return input_; }
   bool gotOpened() { return gotOpened_; }
   bool gotClosed() { return gotClosed_; }

private:
   void watchSocket() { client_.run(); }

   std::string handle_;
   boost::shared_ptr<SocketHarness> pServerSocket_;
   std::string input_;
   bool gotOpened_;
   bool gotClosed_;

   client client_;
   boost::thread clientSocketThread_;
   bool clientRunning_;
   websocketpp::connection_hdl hdl_;
   websocketpp::lib::mutex lock_;
};

} // anonymous namespace

context("websocket for interactive terminals")
{
   const std::string handle1 = "abcd";

   test_that("port for new socket object is zero")
   {
      boost::shared_ptr<SocketHarness> pSocket = boost::make_shared<SocketHarness>();
      expect_true(pSocket->port() == 0);
   }

   test_that("cannot listen if server is not running")
   {
      boost::shared_ptr<SocketHarness> pSocket = boost::make_shared<SocketHarness>();
      SocketClient client(handle1, pSocket);
      expect_false(client.listen());
   }

   test_that("server port returned correctly when started")
   {
      boost::shared_ptr<SocketHarness> pSocket = boost::make_shared<SocketHarness>();
      expect_true(pSocket->ensureServerRunning());
      expect_true(pSocket->port() > 0);
   }

   test_that("stop listening to unknown handle returns error")
   {
      boost::shared_ptr<SocketHarness> pSocket = boost::make_shared<SocketHarness>();
      expect_true(pSocket->ensureServerRunning());
      SocketClient client(handle1, pSocket);
      expect_false(client.stopListening());
   }

   test_that("can start and stop listening to a handle")
   {
      boost::shared_ptr<SocketHarness> pSocket = boost::make_shared<SocketHarness>();
      expect_true(pSocket->ensureServerRunning());
      SocketClient client(handle1, pSocket);
      expect_true(client.listen());
   }

//   test_that("can connect to server and send a message")
//   {
//      boost::shared_ptr<SocketHarness> pSocket = boost::make_shared<SocketHarness>();
//      expect_true(pSocket->ensureServerRunning());
//      SocketClient client(handle1, pSocket);
//      expect_true(client.listen());
//      expect_true(client.connectToServer());

//      const std::string message = "Hello World!";

//      expect_true(client.sendMessage(message));
//      //expect_true(client.sendMessage(kCloseMessage));

//      for (;;)
//      {
//         if (!pSocket->receivedInput.compare(message))
//            break;
//      }

//      expect_true(client.disconnectFromServer());
//      expect_true(pSocket->stopServer());
//   }
}

} // namespace console_process
} // namespace session
} // namespace rstudio
