/*
 * SessionConsoleProcessSocketPacket.hpp
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
#ifndef SESSION_CONSOLE_PROCESS_SOCKET_PACKET_HPP
#define SESSION_CONSOLE_PROCESS_SOCKET_PACKET_HPP

#include <string>

namespace rstudio {
namespace session {
namespace console_process {

/**
 * Super-simple packet format for Terminal Websocket payloads. Not using JSON to keep
 * overhead to absolute minimum, and to make server-side parsing trivial on a worker
 * thread.
 *
 * First character is a method indicator, as follows:
 *    "a" = send text, e.g. "aHello"
 *    "b" = ping/pong, e.g. "b"
 *
 * Only the "send text" method has a payload (everything after the "a").
 *
 * See TerminalSocketPacket in Java code for client-side of this.
 */
class ConsoleProcessSocketPacket
{
public:
   // create packet for given text
   static std::string textPacket(const std::string& text);

   // create keepalive packet
   static std::string keepAlivePacket();

   // is this packet a keep-alive packet?
   static bool isKeepAlive(const std::string& text);

   // extract text from packet (empty string if unable to comply)
   static std::string getMessage(const std::string& text);

private:
   static const std::string kKeepAlivePrefix;
   static const std::string kTextPrefix;
};

} // namespace console_process
} // namespace session
} // namespace rstudio

#endif // SESSION_CONSOLE_PROCESS_SOCKET_PACKET_HPP
