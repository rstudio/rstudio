/*
 * SessionConsoleProcessSocket.hpp
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
#ifndef SESSION_CONSOLE_PROCESS_SOCKET_HPP
#define SESSION_CONSOLE_PROCESS_SOCKET_HPP

#include <string>

#ifdef _WIN32
# include <winsock2.h>
#endif

#include <boost/function.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/asio.hpp>

#include <core/Error.hpp>
#include <core/Thread.hpp>

#include <websocketpp/config/asio_no_tls.hpp>
#include <websocketpp/server.hpp>
#include <websocketpp/frame.hpp>

namespace rstudio {
namespace session {
namespace console_process {

// Overview: ConsoleProcessSocket manages a single websocket, and
// multiple connections to that websocket, for the purpose of high-
// speed communication of input/output for interactive terminals
// spawned by the server, and displayed in the client.
//
// ConsoleProcessSocketConnectionCallbacks are related to connections.
// Each connections made will supply a unique set of these callbacks,
// and will receive callbacks only related to that connection.
//
// Each connection MUST be made with a URL ending with /xxxx/ where "xxxx"
// is some textual unique handle for that connection. In practice, this is
// the terminal handle string used elsewhere in the codebase. This uniqueId
// is used to dispatch callbacks, and to send output to the right connection.
//
// IMPORTANT: Callbacks are dispatched on a background thread.

struct ConsoleProcessSocketConnectionCallbacks
{
   // invoked when input arrives on the socket
   boost::function<void (const std::string& input)> onReceivedInput;

   // invoked when connection opens
   boost::function<void()> onConnectionOpened;

   // invoked when connection closes
   boost::function<void ()> onConnectionClosed;
};

typedef websocketpp::server<websocketpp::config::asio> terminalServer;
typedef terminalServer::message_ptr terminalMessage_ptr;

struct ConsoleProcessSocketConnectionDetails
{
   std::string handle_;
   ConsoleProcessSocketConnectionCallbacks connectionCallbacks_;
   websocketpp::connection_hdl hdl_;
};

// Manages a websocket that channels input and output from client for
// interactive terminals. Terminals are identified via a unique handle.
class ConsoleProcessSocket : boost::noncopyable
{
public:
   ConsoleProcessSocket();
   ~ConsoleProcessSocket();

   // start the websocket servicing thread
   core::Error ensureServerRunning();

   // stop the websocket servicing thread
   void stopServer();

   // start receiving callbacks for given connection; client should call
   // before making the connection to ensure all callbacks are received
   core::Error listen(const std::string& terminalHandle,
                      const ConsoleProcessSocketConnectionCallbacks& callbacks);

   // stop listening to given terminal handle
   core::Error stopListening(const std::string& terminalHandle);

   // send text to client
   core::Error sendText(const std::string& terminalHandle,
                        const std::string& message);

   // network port for websocket listener; 0 means no port
   int port() const;

private:
   void watchSocket();

   void releaseAllConnections();
   std::string getHandle(terminalServer* s, websocketpp::connection_hdl hdl);
   void onMessage(terminalServer* s, websocketpp::connection_hdl hdl,
                  terminalMessage_ptr msg);
   void onClose(terminalServer* s, websocketpp::connection_hdl hdl);
   void onOpen(terminalServer* s, websocketpp::connection_hdl hdl);
   void onHttp(terminalServer* s, websocketpp::connection_hdl hdl);

   void onServerTimeout(boost::system::error_code ec);

private:
   core::thread::ThreadsafeMap<std::string, ConsoleProcessSocketConnectionDetails> connections_;

   int port_;
   boost::thread websocketThread_;
   bool serverRunning_;
   boost::shared_ptr<terminalServer> pwsServer_;

   int activeConnections_;
};

} // namespace console_process
} // namespace session
} // namespace rstudio

#endif // SESSION_CONSOLE_PROCESS_SOCKET_HPP
