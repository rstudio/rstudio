/*
 * Command.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef MONITOR_AUDIT_COMMAND_HPP
#define MONITOR_AUDIT_COMMAND_HPP

#include <string>

namespace rstudio {
namespace monitor {
namespace audit {

struct Command
{
   Command() : timestamp(0) {}

   Command(const std::string& username,
           double timestamp,
           const std::string& command)
      : username(username), timestamp(timestamp), command(command)
   {
   }

   bool empty() const { return username.empty(); }

   std::string username;
   double timestamp;
   std::string command;
};

} // namespace audit
} // namespace monitor
} // namespace rstudio

#endif // MONITOR_AUDIT_COMMAND_HPP

