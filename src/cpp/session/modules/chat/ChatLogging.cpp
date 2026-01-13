/*
 * ChatLogging.cpp
 *
 * Copyright (C) 2025 by Posit Software, PBC
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

#include "ChatLogging.hpp"

#include <shared_core/json/Json.hpp>
#include <r/RSexp.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace logging {

// ============================================================================
// Global state
// ============================================================================
static int s_chatLogLevel = 0;
static std::string s_chatBackendMinLogLevel = "error"; // Default: show only error logs

// ============================================================================
// Log level management
// ============================================================================

int chatLogLevel()
{
   return s_chatLogLevel;
}

void setChatLogLevel(int level)
{
   s_chatLogLevel = level;
}

void setBackendMinLogLevel(const std::string& level)
{
   s_chatBackendMinLogLevel = level;
}

std::string getBackendMinLogLevel()
{
   return s_chatBackendMinLogLevel;
}

// ============================================================================
// Log level utilities
// ============================================================================

int getLogLevelPriority(const std::string& level)
{
   if (level == "trace") return 0;
   if (level == "debug") return 1;
   if (level == "info")  return 2;
   if (level == "warn")  return 3;
   if (level == "error") return 4;
   if (level == "fatal") return 5;
   return 999; // Unknown levels treated as highest priority (always show them)
}

bool shouldLogBackendMessage(const std::string& messageBody)
{
   using namespace rstudio::core;

   // Quick parse to check if this is a logger/log notification
   json::Value message;
   if (message.parse(messageBody))
      return true; // Parse error, show it

   if (!message.isObject())
      return true; // Not an object, show it

   json::Object obj = message.getObject();

   // Check if it's a logger/log notification
   std::string method;
   core::Error error = json::readObject(obj, "method", method);
   if (error)
      return true; // Parse error reading method field, show it
   if (method != "logger/log")
      return true; // Not a logger/log notification, always show it at level 2+

   // It's a logger/log notification
   // At level 2: hide raw JSON (formatted version will be shown by handleLoggerLog)
   // At level 3+: show raw JSON (for debugging the logging mechanism itself)
   if (chatLogLevel() >= 3)
   {
      // Level 3+: apply backend level filter and show if it passes
      json::Object params;
      error = json::readObject(obj, "params", params);
      if (error)
         return true; // Parse error reading params field, show it

      std::string level;
      error = json::readObject(params, "level", level);
      if (error)
         return true; // Parse error reading level field, show it

      return getLogLevelPriority(level) >= getLogLevelPriority(s_chatBackendMinLogLevel);
   }

   // Level 2: hide logger/log raw JSON (user will see formatted version)
   return false;
}

// ============================================================================
// R interface
// ============================================================================

SEXP rs_chatSetLogLevel(SEXP logLevelSEXP)
{
   int logLevel = r::sexp::asInteger(logLevelSEXP);
   s_chatLogLevel = logLevel;
   return logLevelSEXP;
}

} // namespace logging
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio
