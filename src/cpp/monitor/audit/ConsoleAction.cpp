/*
 * ConsoleAction.cpp
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

#include <monitor/audit/ConsoleAction.hpp>

#include <core/Log.hpp>
#include <core/json/JsonRpc.hpp>

#include <shared_core/SafeConvert.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace monitor {
namespace audit {

std::string consoleActionTypeToString(int type)
{
    switch(type)
    {
        case kConsoleActionPrompt:
            return "prompt";
        case kConsoleActionInput:
            return "input";
        case kConsoleActionOutput:
            return "output";
        case kConsoleActionOutputError:
            return "error";
        default:
            return "";
    }
}

json::Object consoleActionToJson(const ConsoleAction& action)
{
   json::Object actionJson;
   actionJson["session_id"] = action.sessionId;
   actionJson["project"] = action.project;
   actionJson["pid"] = action.pid;
   actionJson["username"] = action.username;
   actionJson["timestamp"] = action.timestamp;
   actionJson["type"] = action.type;
   actionJson["data"] = action.data;
   return actionJson;
}

json::Object consoleActionToJsonLogEntry(const ConsoleAction& action)
{
   json::Object actionJson = consoleActionToJson(action);
   actionJson["pid"] = static_cast<boost::int64_t>(action.pid);
   actionJson["timestamp"] = static_cast<boost::int64_t>(action.timestamp);
   actionJson["type"] = audit::consoleActionTypeToString(action.type);
   return actionJson;
}

ConsoleAction consoleActionFromJson(const json::Object& actionJson)
{
   ConsoleAction action;
   Error error = json::readObject(actionJson,
                                  "session_id", action.sessionId,
                                  "project", action.project,
                                  "pid", action.pid,
                                  "username", action.username,
                                  "timestamp", action.timestamp,
                                  "type", action.type,
                                  "data", action.data);
   if (error)
      LOG_ERROR(error);

   return action;
}




} // namespace audit
} // namespace monitor
} // namespace rstudio

