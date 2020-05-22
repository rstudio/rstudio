/*
 * SessionConsoleProcessSocketPacket.cpp
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

#include <session/SessionConsoleProcessSocketPacket.hpp>

namespace rstudio {
namespace session {
namespace console_process {

const std::string ConsoleProcessSocketPacket::kKeepAlivePrefix = "b";
const std::string ConsoleProcessSocketPacket::kTextPrefix = "a";

/* static */
std::string ConsoleProcessSocketPacket::textPacket(const std::string& text)
{
   return kTextPrefix + text;
}

/* static */
std::string ConsoleProcessSocketPacket::keepAlivePacket()
{
   return kKeepAlivePrefix;
}

/* static */
bool ConsoleProcessSocketPacket::isKeepAlive(const std::string& text)
{
   return text == kKeepAlivePrefix;
}

/* static */
std::string ConsoleProcessSocketPacket::getMessage(const std::string& text)
{
   if (!text.compare(0, kTextPrefix.length(), kTextPrefix))
   {
      return text.substr(kTextPrefix.length());
   }
   else
   {
      return std::string();
   }
}

} // namespace console_process
} // namespace session
} // namespace rstudio
