/*
 * SessionConsoleProcessSocketTests.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#include <session/SessionConsoleProcessSocket.hpp>
#include <session/SessionConsoleProcessSocketPacket.hpp>

#include <boost/shared_ptr.hpp>
#include <boost/make_shared.hpp>
#include <boost/enable_shared_from_this.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/bind/bind.hpp>

#include <websocketpp/config/asio_no_tls_client.hpp>
#include <websocketpp/client.hpp>

#include <gtest/gtest.h>

namespace rstudio {
namespace session {
namespace console_process {
namespace tests {

// Test constants
const std::string handle1("unit-test01");
const std::string msgString1("Sample message text");

using namespace console_process;
using namespace boost::placeholders;

namespace {

void blockingwait(int ms)
{
   boost::asio::io_context io;
   boost::asio::system_timer timer(io, std::chrono::milliseconds(ms));
   timer.wait();
}

// Wrapper for ConsoleProcessSocket, the class we're testing. Primarily
// converts Error codes into true/false, and accumulates input.
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

   bool stopServer()
   {
      blockingwait(50);
      socket_.stopServer();
      return true;
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

   bool sendRawText(const std::string& terminalHandle,
                 const std::string& message)
   {
      core::Error err = socket_.sendRawText(terminalHandle, message);
      return (!err);
   }

   int port() { return socket_.port(); }

private:
   ConsoleProcessSocket socket_;
};

// Server-side connection test harness; each server-side connection needs one
// of these created and listening before a client can connect to the websocket
class SocketConnection : public boost::enable_shared_from_this<SocketConnection>
{
public:
   SocketConnection(const std::string& handle,
                    boost::shared_ptr<SocketHarness> pServerSocket)
      :
        handle_(handle),
        pServerSocket_(pServerSocket),
        didClose_(false),
        didOpen_(false)

   {}

   void onReceivedInput(const std::string& input)
   {
      received_ += input;
   }

   void onConnectionOpened()
   {
      didOpen_ = true;
   }

   void onConnectionClosed()
   {
      didClose_ = true;
   }

   ConsoleProcessSocketConnectionCallbacks createConsoleProcessSocketConnectionCallbacks()
   {
      using boost::bind;
      ConsoleProcessSocketConnectionCallbacks cb;
      cb.onReceivedInput =
            bind(&SocketConnection::onReceivedInput, SocketConnection::shared_from_this(), _1);
      cb.onConnectionOpened =
            bind(&SocketConnection::onConnectionOpened, SocketConnection::shared_from_this());
      cb.onConnectionClosed =
            bind(&SocketConnection::onConnectionClosed, SocketConnection::shared_from_this());
      return cb;
   }

   // listen informs the server to watch for incoming connections with a unique
   // handle, and callbacks to issue for activity on that connection
   bool listen()
   {
      return pServerSocket_->listen(handle_,
                                    createConsoleProcessSocketConnectionCallbacks());
   }

   // tell server to no longer recognize connections/activity on a given handle
   bool stopListening()
   {
      return pServerSocket_->stopListening(handle_);
   }

   // send a message to client of this connection
   bool sendRawMessage(const std::string& msg)
   {
      return pServerSocket_->sendRawText(handle_, msg);
   }

   std::string getReceived() const
   {
      blockingwait(50);
      return received_;
   }

   bool didClose() const { return didClose_; }
   bool didOpen() const { return didOpen_; }

private:
   std::string handle_;
   std::string received_;
   boost::shared_ptr<SocketHarness> pServerSocket_;
   bool didClose_;
   bool didOpen_;
};

using client = websocketpp::client<websocketpp::config::asio_client>;
using message_ptr = client::message_ptr;

// Client-side websocket connection test class; used to verify a client
// can connect to the websocket and send/receive data over the socket
class SocketClient : public boost::enable_shared_from_this<SocketClient>
{
public:
   SocketClient(const std::string& handle, int port)
      :
        handle_(handle),
        port_(port),
        gotOpened_(false),
        gotClosed_(false),
        gotFailed_(false),
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
      if (msg->get_opcode() == websocketpp::frame::opcode::text)
      {
         input_ += msg->get_payload();
      }
      else
      {
         std::cerr << "Unsupported websocket message type" << std::endl;
      }
   }

   void on_open(client* c, websocketpp::connection_hdl hdl)
   {
      gotOpened_ = true;
      hdl_ = hdl;
      client::connection_ptr con = c->get_con_from_hdl(hdl);
      server_ = con->get_response_header("Server");
    }

   void on_fail(client* c, websocketpp::connection_hdl hdl)
   {
      gotFailed_ = true;
      client::connection_ptr con = c->get_con_from_hdl(hdl);
      server_ = con->get_response_header("Server");
      errorReason_ = con->get_ec().message();
   }

   bool connectToServer()
   {
      using websocketpp::lib::bind;

      try 
      {
         std::string uri = "http://localhost:" +
               boost::lexical_cast<std::string>(port_) +
               "/terminal/" + handle_ + "/";

         // don't clutter up unit test runs with warnings
         client_.set_access_channels(websocketpp::log::alevel::none);
         client_.set_error_channels(websocketpp::log::alevel::none);

         // Initialize ASIO
         client_.init_asio();

         // Register our message handler
         client_.set_message_handler(bind(&SocketClient::on_message,
                                          SocketClient::shared_from_this(),
                                          &client_, _1, _2));
         client_.set_open_handler(bind(&SocketClient::on_open,
                                       SocketClient::shared_from_this(),
                                       &client_, _1));
         client_.set_fail_handler(bind(&SocketClient::on_fail,
                                       SocketClient::shared_from_this(),
                                       &client_, _1));

         websocketpp::lib::error_code ec;
         client::connection_ptr con = client_.get_connection(uri, ec);
         if (ec)
            return false;

         // Note that connect here only requests a connection. No network
         // messages are exchanged until the event loop starts running in
         // the thread.
         client_.connect(con);

         // Start the ASIO io_context run loop in a separate thread; allows
         // us to perform multiple operations as part of the test.
         core::thread::safeLaunchThread(
                  boost::bind(&SocketClient::watchSocket, this),
                  &clientSocketThread_);
      }
      catch (websocketpp::exception const &)
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

   bool sendText(const std::string& str)
   {
      websocketpp::lib::error_code ec;
      client_.send(hdl_, ConsoleProcessSocketPacket::textPacket(str), websocketpp::frame::opcode::text, ec);
      if (ec)
      {
         std::string error = ec.message();
         std::cerr << "Client sendText failed because: " << error << std::endl;
         return false;
      }
      return true;
   }

   void waitForConnectionOrError()
   {
      while (!gotOpened() && !gotFailed())
      {
         blockingwait(50);
      }
      blockingwait(10);
      EXPECT_TRUE(gotOpened());
      EXPECT_FALSE(gotFailed());
   }

   std::string getInput() { blockingwait(50); return input_; }
   bool gotOpened() { return gotOpened_; }
   bool gotClosed() { return gotClosed_; }
   bool gotFailed() { return gotFailed_; }
   std::string getServer() { return server_; }
   std::string getErrorReason() { return errorReason_; }

private:
   void watchSocket() { client_.run(); }

   std::string handle_;
   int port_;

   std::string input_;
   bool gotOpened_;
   bool gotClosed_;
   bool gotFailed_;
   std::string server_;
   std::string errorReason_;

   client client_;
   boost::thread clientSocketThread_;
   bool clientRunning_;
   websocketpp::connection_hdl hdl_;
};

} // anonymous namespace

TEST(ConsoleProcessSocketTest, NewSocketHasZeroPort) {
   boost::shared_ptr<SocketHarness> pSocket = boost::make_shared<SocketHarness>();
   EXPECT_TRUE(pSocket->port() == 0);
}

TEST(ConsoleProcessSocketTest, StoppingNonRunningServerSucceeds) {
   boost::shared_ptr<SocketHarness> pSocket = boost::make_shared<SocketHarness>();
   EXPECT_TRUE(pSocket->stopServer());
}

TEST(ConsoleProcessSocketTest, StartedServerReturnsValidPort) {
   boost::shared_ptr<SocketHarness> pSocket = boost::make_shared<SocketHarness>();
   EXPECT_TRUE(pSocket->ensureServerRunning());
   EXPECT_TRUE(pSocket->port() > 0);

   EXPECT_TRUE(pSocket->stopServer());
}

TEST(ConsoleProcessSocketTest, CanStartAndStopListeningToHandle) {
   boost::shared_ptr<SocketHarness> pSocket = boost::make_shared<SocketHarness>();
   EXPECT_TRUE(pSocket->ensureServerRunning());

   boost::shared_ptr<SocketConnection> pConnection =
         boost::make_shared<SocketConnection>(handle1, pSocket);
   EXPECT_TRUE(pConnection->listen());

   EXPECT_TRUE(pConnection->stopListening());
   EXPECT_TRUE(pSocket->stopServer());
}

TEST(ConsoleProcessSocketTest, ClientCanConnectAndDisconnect) {
   boost::shared_ptr<SocketHarness> pSocket = boost::make_shared<SocketHarness>();
   EXPECT_TRUE(pSocket->ensureServerRunning());

   boost::shared_ptr<SocketConnection> pConnection = boost::make_shared<SocketConnection>(handle1, pSocket);
   boost::shared_ptr<SocketClient> pClient = boost::make_shared<SocketClient>(handle1, pSocket->port());
   EXPECT_TRUE(pConnection->listen());
   EXPECT_TRUE(pClient->connectToServer());

   pClient->waitForConnectionOrError();

   EXPECT_TRUE(pClient->disconnectFromServer());
   EXPECT_TRUE(pSocket->stopServer());
}

TEST(ConsoleProcessSocketTest, ClientCanSendTextToServer) {
   boost::shared_ptr<SocketHarness> pSocket = boost::make_shared<SocketHarness>();
   EXPECT_TRUE(pSocket->ensureServerRunning());

   boost::shared_ptr<SocketConnection> pConnection = boost::make_shared<SocketConnection>(handle1, pSocket);
   boost::shared_ptr<SocketClient> pClient = boost::make_shared<SocketClient>(handle1, pSocket->port());
   EXPECT_TRUE(pConnection->listen());
   EXPECT_TRUE(pClient->connectToServer());

   pClient->waitForConnectionOrError();

   EXPECT_TRUE(pClient->sendText(msgString1));
   EXPECT_EQ(msgString1, pConnection->getReceived());

   EXPECT_TRUE(pClient->disconnectFromServer());
   EXPECT_TRUE(pSocket->stopServer());
}

TEST(ConsoleProcessSocketTest, ServerCanSendTextToClient) {
   boost::shared_ptr<SocketHarness> pSocket = boost::make_shared<SocketHarness>();
   EXPECT_TRUE(pSocket->ensureServerRunning());

   boost::shared_ptr<SocketConnection> pConnection = boost::make_shared<SocketConnection>(handle1, pSocket);
   boost::shared_ptr<SocketClient> pClient = boost::make_shared<SocketClient>(handle1, pSocket->port());
   EXPECT_TRUE(pConnection->listen());
   EXPECT_TRUE(pClient->connectToServer());

   pClient->waitForConnectionOrError();

   EXPECT_TRUE(pConnection->sendRawMessage(msgString1));
   EXPECT_EQ(msgString1, pClient->getInput());

   EXPECT_TRUE(pClient->disconnectFromServer());
   EXPECT_TRUE(pSocket->stopServer());
}

TEST(ConsoleProcessSocketTest, ClientCanMakeMultipleConnectionsToServer) {
   boost::shared_ptr<SocketHarness> pSocket = boost::make_shared<SocketHarness>();
   EXPECT_TRUE(pSocket->ensureServerRunning());

   // Create first connection
   boost::shared_ptr<SocketConnection> pConnection1 = boost::make_shared<SocketConnection>(handle1, pSocket);
   boost::shared_ptr<SocketClient> pClient1 = boost::make_shared<SocketClient>(handle1, pSocket->port());
   EXPECT_TRUE(pConnection1->listen());
   EXPECT_TRUE(pClient1->connectToServer());

   pClient1->waitForConnectionOrError();
   
   // Create second connection with a different handle
   const std::string handle2 = "unit-test02";
   boost::shared_ptr<SocketConnection> pConnection2 = boost::make_shared<SocketConnection>(handle2, pSocket);
   boost::shared_ptr<SocketClient> pClient2 = boost::make_shared<SocketClient>(handle2, pSocket->port());
   EXPECT_TRUE(pConnection2->listen());
   EXPECT_TRUE(pClient2->connectToServer());

   pClient2->waitForConnectionOrError();
   
   // Test that both connections work independently
   EXPECT_TRUE(pConnection1->sendRawMessage("Message to client 1"));
   EXPECT_TRUE(pConnection2->sendRawMessage("Message to client 2"));
   
   EXPECT_EQ("Message to client 1", pClient1->getInput());
   EXPECT_EQ("Message to client 2", pClient2->getInput());

   EXPECT_TRUE(pClient1->disconnectFromServer());
   EXPECT_TRUE(pClient2->disconnectFromServer());
   EXPECT_TRUE(pSocket->stopServer());
}

} // namespace tests
} // namespace console_process
} // namespace session
} // namespace rstudio
