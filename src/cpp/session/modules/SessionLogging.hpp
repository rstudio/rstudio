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

// WARNING: This header redefines core logging macros (LOG_ERROR, etc.)
// to add section tagging and stderr mirroring. It should only be included
// from .cpp files, never from other headers (except thin wrappers like
// ChatLogging.hpp that exist solely to configure and re-export it).

#ifndef SESSION_MODULES_SESSION_LOGGING_HPP
#define SESSION_MODULES_SESSION_LOGGING_HPP

#include <algorithm>
#include <iostream>
#include <string>

#include <boost/optional.hpp>

#include <fmt/format.h>

#include <core/Log.hpp>

namespace rstudio
{
namespace session
{
namespace logging
{

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

namespace rstudio
{
namespace core
{
class Error;
} // namespace core
} // namespace rstudio

namespace rstudio
{
namespace session
{
namespace modules
{
namespace logging
{

core::Error initialize();

} // namespace logging
} // namespace modules
} // namespace session
} // namespace rstudio

// ---------------------------------------------------------------------------
// Configurable hooks (define before including this header to customize)
//
// SESSION_LOG_SECTION
//   The log section name. Defaults to an auto-derived name from __FILE__.
//   Example: #define SESSION_LOG_SECTION "chat"
// ---------------------------------------------------------------------------

#ifndef SESSION_LOG_SECTION
#define SESSION_LOG_SECTION rstudio::session::logging::logNameFromFile(__FILE__)
#endif

// Log section name, computed once per translation unit.
namespace
{
const std::string s_sessionLoggingSection(SESSION_LOG_SECTION);
} // anonymous namespace

// Helper macros for generating unique variable names within macros.
#define CONCAT_IMPL(a, b) a##b
#define CONCAT(a, b) CONCAT_IMPL(a, b)
#define VAR(name) CONCAT(name, __LINE__)

// Helper macro: mirror a message to stderr when configured via the
// runtime registry (setStderrLogLevel).
#define SESSION_STDERR_MIRROR_(message)                                        \
   do                                                                          \
   {                                                                           \
      using rstudio::session::logging::stderrLogLevel;                         \
      if (stderrLogLevel(s_sessionLoggingSection) >= 1)                        \
         std::cerr << (message) << std::endl;                                  \
   } while (0)

// Override standard logging macros to automatically supply the log
// section name derived from SESSION_LOG_SECTION, and mirror to stderr
// when configured.
#ifdef LOG_ERROR
#undef LOG_ERROR
#endif
#define LOG_ERROR(error)                                                       \
   do                                                                          \
   {                                                                           \
      using namespace rstudio::core;                                           \
      using namespace rstudio::core::log;                                      \
      Error VAR(error) = (error);                                              \
      logError(VAR(error), s_sessionLoggingSection, ERROR_LOCATION);           \
      SESSION_STDERR_MIRROR_(VAR(error).asString());                           \
   } while (0)

#ifdef LOG_ERROR_MESSAGE
#undef LOG_ERROR_MESSAGE
#endif
#define LOG_ERROR_MESSAGE(message)                                             \
   do                                                                          \
   {                                                                           \
      using namespace rstudio::core::log;                                      \
      std::string VAR(message) = (message);                                    \
      logErrorMessage(                                                         \
          VAR(message), s_sessionLoggingSection, boost::none, ERROR_LOCATION); \
      SESSION_STDERR_MIRROR_(VAR(message));                                    \
   } while (0)

#ifdef LOG_WARNING_MESSAGE
#undef LOG_WARNING_MESSAGE
#endif
#define LOG_WARNING_MESSAGE(message)                                           \
   do                                                                          \
   {                                                                           \
      using namespace rstudio::core::log;                                      \
      std::string VAR(message) = (message);                                    \
      logWarningMessage(                                                       \
          VAR(message), s_sessionLoggingSection, boost::none, ERROR_LOCATION); \
      SESSION_STDERR_MIRROR_(VAR(message));                                    \
   } while (0)

#ifdef LOG_INFO_MESSAGE
#undef LOG_INFO_MESSAGE
#endif
#define LOG_INFO_MESSAGE(message)                                              \
   do                                                                          \
   {                                                                           \
      using namespace rstudio::core::log;                                      \
      std::string VAR(message) = (message);                                    \
      logInfoMessage(VAR(message),                                             \
                     s_sessionLoggingSection,                                  \
                     boost::none,                                              \
                     LOG_LOCATION_IF_ENABLED());                               \
      SESSION_STDERR_MIRROR_(VAR(message));                                    \
   } while (0)

#ifdef LOG_DEBUG_MESSAGE
#undef LOG_DEBUG_MESSAGE
#endif
#define LOG_DEBUG_MESSAGE(message)                                             \
   do                                                                          \
   {                                                                           \
      using namespace rstudio::core::log;                                      \
      if (isLogLevel(LogLevel::DEBUG_LEVEL))                                   \
      {                                                                        \
         std::string VAR(message) = (message);                                 \
         logDebugMessage(VAR(message),                                         \
                         s_sessionLoggingSection,                              \
                         boost::none,                                          \
                         LOG_LOCATION_IF_ENABLED());                           \
         SESSION_STDERR_MIRROR_(VAR(message));                                 \
      }                                                                        \
   } while (0)

#ifdef LOG_TRACE_MESSAGE
#undef LOG_TRACE_MESSAGE
#endif
#define LOG_TRACE_MESSAGE(message)                                             \
   do                                                                          \
   {                                                                           \
      using namespace rstudio::core::log;                                      \
      if (isLogLevel(LogLevel::TRACE_LEVEL))                                   \
      {                                                                        \
         std::string VAR(message) = (message);                                 \
         logTraceMessage(VAR(message),                                         \
                         s_sessionLoggingSection,                              \
                         boost::none,                                          \
                         LOG_LOCATION_IF_ENABLED());                           \
         SESSION_STDERR_MIRROR_(VAR(message));                                 \
      }                                                                        \
   } while (0)

// DLOG / WLOG / ELOG / ILOG / TLOG: convenience macros using fmt::format
// with automatic function-name prefix.
//
// Output is mirrored to stderr when the runtime registry
// (setStderrLogLevel) has been set >= 1 for this section.

#define SESSION_LOG_IMPL_(__LOGGER__, __FMT__, ...)                            \
   do                                                                          \
   {                                                                           \
      std::string formatted =                                                  \
          fmt::format("[{}]: " __FMT__, __func__, ##__VA_ARGS__);              \
      __LOGGER__(s_sessionLoggingSection, formatted);                          \
      SESSION_STDERR_MIRROR_(formatted);                                       \
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
