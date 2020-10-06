/*
 * ConsoleAction.hpp
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

#ifndef MONITOR_AUDIT_CONSOLE_ACTION_HPP
#define MONITOR_AUDIT_CONSOLE_ACTION_HPP

#include <string>

#include <shared_core/json/Json.hpp>

#define kConsoleActionPrompt        0
#define kConsoleActionInput         1
#define kConsoleActionOutput        2
#define kConsoleActionOutputError   3

namespace rstudio {
namespace monitor {
namespace audit {

struct ConsoleAction
{
   ConsoleAction() : timestamp(0) {}

   ConsoleAction(const std::string& sessionId,
                 const std::string& project,
                 double pid,
                 const std::string& username,
                 double timestamp,
                 int type,
                 const std::string& data)
      : sessionId(sessionId),
        project(project),
        pid(pid),
        username(username),
        timestamp(timestamp),
        type(type),
        data(data)
   {
   }

   bool empty() const { return username.empty(); }

   std::string sessionId;
   std::string project;
   double pid;
   std::string username;
   double timestamp;
   int type;
   std::string data;
};

std::string consoleActionTypeToString(int type);
core::json::Object consoleActionToJson(const ConsoleAction& action);
ConsoleAction consoleActionFromJson(const core::json::Object& actionJson);
core::json::Object consoleActionToJsonLogEntry(const ConsoleAction& action);


} // namespace audit
} // namespace monitor
} // namespace rstudio

#endif // MONITOR_AUDIT_CONSOLE_ACTION_HPP

