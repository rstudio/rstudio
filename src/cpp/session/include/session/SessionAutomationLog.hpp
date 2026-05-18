/*
 * SessionAutomationLog.hpp
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

#ifndef SESSION_AUTOMATION_LOG_HPP
#define SESSION_AUTOMATION_LOG_HPP

#include <string>

#include <fmt/format.h>

namespace rstudio {
namespace session {
namespace module_context {

// Append a message to /tmp/rstudio-automation/automation.log when the
// session is running under --run-automation. No-op otherwise, so this is
// cheap to leave in place after a debugging session. Thread-safe; appends a
// trailing newline if the message doesn't already end with one. POSIX only.
void automationLog(const std::string& message);

} // namespace module_context
} // namespace session
} // namespace rstudio

// Append a string to the automation log.
#define ALOG(message) \
   ::rstudio::session::module_context::automationLog(message)

// Append an fmt-formatted message to the automation log.
#define ALOGF(__FMT__, ...) \
   do { \
      std::string __automationLogMessage = \
            fmt::format(FMT_STRING(__FMT__), ##__VA_ARGS__); \
      ::rstudio::session::module_context::automationLog(__automationLogMessage); \
   } while (0)

#endif // SESSION_AUTOMATION_LOG_HPP
