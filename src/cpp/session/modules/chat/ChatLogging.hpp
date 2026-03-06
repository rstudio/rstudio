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

// Forward declare chatLogLevel() so the macro can reference it.
namespace rstudio {
namespace session {
namespace modules {
namespace chat {
namespace logging {
int chatLogLevel();
} // namespace logging
} // namespace chat
} // namespace modules
} // namespace session
} // namespace rstudio

// Configure SessionLogging.hpp: use "chat" section and mirror to stderr
// when CHAT_LOG_LEVEL >= 1.
#define SESSION_LOG_SECTION "chat"
#define SESSION_STDERR_LOG_LEVEL \
   rstudio::session::modules::chat::logging::chatLogLevel()
#include "../SessionLogging.hpp"

// Forward declare SEXP for R interface
#ifndef R_INTERNALS_H_
typedef struct SEXPREC *SEXP;
#endif

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
