/*
 * ChatLogging.hpp
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

#ifndef SESSION_CHAT_LOGGING_HPP
#define SESSION_CHAT_LOGGING_HPP

#include <string>
#include <iostream>
#include <fmt/format.h>
#include <core/Log.hpp>

// Forward declare SEXP for R interface
#ifndef R_INTERNALS_H_
typedef struct SEXPREC *SEXP;
#endif

// Logging macros - must be in header to capture __func__ at call site
#define CHAT_LOG_IMPL(__LOGGER__, __FMT__, ...)                             \
   do                                                                       \
   {                                                                        \
      std::string __message__ = fmt::format(__FMT__, ##__VA_ARGS__);        \
      std::string __formatted__ =                                           \
          fmt::format("[{}]: {}", __func__, __message__);                   \
      __LOGGER__("chat", __formatted__);                                    \
      if (rstudio::session::modules::chat::logging::chatLogLevel() >= 1)   \
         std::cerr << __formatted__ << std::endl;                           \
   } while (0)

#define DLOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_DEBUG_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)
#define WLOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_WARNING_MESSAGE_NAMED, __FMT__, ##__VA_ARGS__)
#define ELOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_ERROR_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)
#define ILOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_INFO_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)
#define TLOG(__FMT__, ...) CHAT_LOG_IMPL(LOG_TRACE_MESSAGE_NAMED,   __FMT__, ##__VA_ARGS__)

namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace logging {

// ============================================================================
// Log level management
// ============================================================================

// Get current chat log level
int chatLogLevel();

// Set chat log level (internal use)
void setChatLogLevel(int level);

// Set backend minimum log level (internal use)
void setBackendMinLogLevel(const std::string& level);

// Get backend minimum log level (internal use)
std::string getBackendMinLogLevel();

// ============================================================================
// Log level utilities
// ============================================================================

// Map log level names to numeric priorities for filtering
// Returns priority (higher = more severe)
// Unknown levels return very high priority to ensure critical messages are never filtered
int getLogLevelPriority(const std::string& level);

// Check if a logger/log notification should be shown in raw JSON-RPC format
// Returns true if the message should be logged in raw form
// Verbosity levels:
//   CHAT_LOG_LEVEL=2: Show all JSON except logger/log (formatted version is enough)
//   CHAT_LOG_LEVEL=3+: Show all JSON including logger/log (for debugging logging itself)
bool shouldLogBackendMessage(const std::string& messageBody);

// ============================================================================
// R interface
// ============================================================================

// R callable function to set log level
SEXP rs_chatSetLogLevel(SEXP logLevelSEXP);

} // namespace logging
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_CHAT_LOGGING_HPP
