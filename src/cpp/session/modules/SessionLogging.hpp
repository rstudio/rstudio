/*
 * SessionLogging.hpp
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

#ifndef SESSION_MODULES_SESSION_LOGGING_HPP
#define SESSION_MODULES_SESSION_LOGGING_HPP

#include <algorithm>
#include <iostream>
#include <string>

#include <fmt/format.h>

#include <core/Log.hpp>


namespace rstudio {
namespace session {
namespace logging {

// Derives a log section name from __FILE__.
// Strips the directory, removes a "Session" prefix if present,
// strips the file extension, and lowercases the result.
//
// Examples:
//   "src/cpp/session/modules/SessionAssistant.cpp" -> "assistant"
//   "src/cpp/session/modules/SessionNodeTools.cpp" -> "nodetools"
//   "src/cpp/session/modules/SessionLSP.cpp"       -> "lsp"
inline std::string logNameFromFile(const char* file)
{
   std::string name(file);

   // Strip directory
   auto pos = name.find_last_of("/\\");
   if (pos != std::string::npos)
      name = name.substr(pos + 1);

   // Strip extension
   pos = name.rfind('.');
   if (pos != std::string::npos)
      name = name.substr(0, pos);

   // Strip "Session" prefix
   if (name.size() > 7 && name.substr(0, 7) == "Session")
      name = name.substr(7);

   // Lowercase
   std::transform(name.begin(),
                  name.end(),
                  name.begin(),
                  [](unsigned char c) { return std::tolower(c); });

   return name;
}

// Runtime stderr log level registry. Allows toggling stderr mirroring
// for any named section without recompilation.
int stderrLogLevel(const std::string& section);
void setStderrLogLevel(const std::string& section, int level);

} // namespace logging
} // namespace session
} // namespace rstudio

// ---------------------------------------------------------------------------
// Configurable hooks (define before including this header to customize)
//
// SESSION_LOG_SECTION
//   The log section name. Defaults to an auto-derived name from __FILE__.
//   Example: #define SESSION_LOG_SECTION "chat"
//
// SESSION_STDERR_LOG_LEVEL
//   An expression returning int; when >= 1, DLOG/WLOG/ELOG/ILOG/TLOG also
//   mirror output to stderr. Defaults to 0 (disabled).
//   Example: #define SESSION_STDERR_LOG_LEVEL chatLogLevel()
// ---------------------------------------------------------------------------

#ifndef SESSION_LOG_SECTION
#define SESSION_LOG_SECTION rstudio::session::logging::logNameFromFile(__FILE__)
#endif

#ifndef SESSION_STDERR_LOG_LEVEL
#define SESSION_STDERR_LOG_LEVEL 0
#endif

// Log section name, computed once per translation unit.
namespace {
const std::string s_logSection(SESSION_LOG_SECTION);
} // anonymous namespace

// Override standard logging macros to automatically supply the log
// section name derived from SESSION_LOG_SECTION.
#ifdef LOG_ERROR
#undef LOG_ERROR
#endif
#define LOG_ERROR(error)                                                       \
   rstudio::core::log::logError(error, s_logSection, ERROR_LOCATION)

#ifdef LOG_ERROR_MESSAGE
#undef LOG_ERROR_MESSAGE
#endif
#define LOG_ERROR_MESSAGE(message)                                             \
   rstudio::core::log::logErrorMessage(                                        \
       message, s_logSection, boost::none, ERROR_LOCATION)

#ifdef LOG_WARNING_MESSAGE
#undef LOG_WARNING_MESSAGE
#endif
#define LOG_WARNING_MESSAGE(message)                                           \
   rstudio::core::log::logWarningMessage(                                      \
       message, s_logSection, boost::none, ERROR_LOCATION)

#ifdef LOG_INFO_MESSAGE
#undef LOG_INFO_MESSAGE
#endif
#define LOG_INFO_MESSAGE(message)                                              \
   rstudio::core::log::logInfoMessage(                                         \
       message, s_logSection, boost::none, LOG_LOCATION_IF_ENABLED())

#ifdef LOG_DEBUG_MESSAGE
#undef LOG_DEBUG_MESSAGE
#endif
#define LOG_DEBUG_MESSAGE(message)                                             \
   (rstudio::core::log::isLogLevel(                                            \
       rstudio::core::log::LogLevel::DEBUG_LEVEL)                              \
        ? (rstudio::core::log::logDebugMessage(                                \
               message, s_logSection, boost::none,                             \
               LOG_LOCATION_IF_ENABLED()),                                     \
           true)                                                               \
        : false)

#ifdef LOG_TRACE_MESSAGE
#undef LOG_TRACE_MESSAGE
#endif
#define LOG_TRACE_MESSAGE(message)                                             \
   (rstudio::core::log::isLogLevel(                                            \
       rstudio::core::log::LogLevel::TRACE_LEVEL)                              \
        ? (rstudio::core::log::logTraceMessage(                                \
               message, s_logSection, boost::none,                             \
               LOG_LOCATION_IF_ENABLED()),                                     \
           true)                                                               \
        : false)

// DLOG / WLOG / ELOG / ILOG / TLOG: convenience macros using fmt::format
// with automatic function-name prefix.
//
// Output is mirrored to stderr when either the compile-time
// SESSION_STDERR_LOG_LEVEL hook >= 1, or the runtime registry
// (setStderrLogLevel) has been set >= 1 for this section.

#define SESSION_LOG_IMPL_(__LOGGER__, __FMT__, ...)                            \
   do                                                                          \
   {                                                                           \
      std::string formatted =                                                  \
          fmt::format("[{}]: " __FMT__, __func__, ##__VA_ARGS__);              \
      __LOGGER__(s_logSection, formatted);                                     \
      if ((SESSION_STDERR_LOG_LEVEL) >= 1 ||                                   \
          rstudio::session::logging::stderrLogLevel(s_logSection) >= 1)        \
         std::cerr << formatted << std::endl;                                  \
   } while (0)

#define TLOG(__FMT__, ...)                                                     \
   SESSION_LOG_IMPL_(LOG_TRACE_MESSAGE_NAMED, __FMT__, ##__VA_ARGS__)
#define DLOG(__FMT__, ...)                                                     \
   SESSION_LOG_IMPL_(LOG_DEBUG_MESSAGE_NAMED, __FMT__, ##__VA_ARGS__)
#define ILOG(__FMT__, ...)                                                     \
   SESSION_LOG_IMPL_(LOG_INFO_MESSAGE_NAMED, __FMT__, ##__VA_ARGS__)
#define WLOG(__FMT__, ...)                                                     \
   SESSION_LOG_IMPL_(LOG_WARNING_MESSAGE_NAMED, __FMT__, ##__VA_ARGS__)
#define ELOG(__FMT__, ...)                                                     \
   SESSION_LOG_IMPL_(LOG_ERROR_MESSAGE_NAMED, __FMT__, ##__VA_ARGS__)

#endif // SESSION_MODULES_SESSION_LOGGING_HPP
