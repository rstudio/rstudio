/*
 * SessionConsoleProcessConnectionCallbacks.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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
#ifndef SESSION_CONSOLE_PROCESS_CONNECTION_CALLBACKS_HPP
#define SESSION_CONSOLE_PROCESS_CONNECTION_CALLBACKS_HPP

#include <string>

#include <boost/function.hpp>

namespace rstudio {
namespace session {
namespace console_process {

// ConsoleProcessSocketConnectionCallbacks are related to connections.
// Each connections made will supply a unique set of these callbacks,
// and will receive callbacks only related to that connection.
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

} // namespace console_process
} // namespace session
} // namespace rstudio

#endif // SESSION_CONSOLE_PROCESS_CONNECTION_CALLBACKS_HPP
