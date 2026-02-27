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
#include <string>

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
   std::transform(name.begin(), name.end(), name.begin(),
                  [](unsigned char c) { return std::tolower(c); });

   return name;
}

} // namespace logging
} // namespace session
} // namespace rstudio

// Override LOG_ERROR and LOG_ERROR_MESSAGE to automatically supply the
// log section name derived from the source file.
#ifdef LOG_ERROR
# undef LOG_ERROR
#endif
#define LOG_ERROR(error)                                                       \
   do {                                                                        \
      rstudio::core::log::logError(                                            \
         error,                                                                \
         rstudio::session::logging::logNameFromFile(__FILE__),                 \
         ERROR_LOCATION);                                                      \
   } while (0)

#ifdef LOG_ERROR_MESSAGE
# undef LOG_ERROR_MESSAGE
#endif
#define LOG_ERROR_MESSAGE(message)                                             \
   do {                                                                        \
      rstudio::core::log::logErrorMessage(                                     \
         message,                                                              \
         rstudio::session::logging::logNameFromFile(__FILE__),                 \
         boost::none,                                                          \
         ERROR_LOCATION);                                                      \
   } while (0)

#ifdef LOG_WARNING_MESSAGE
# undef LOG_WARNING_MESSAGE
#endif
#define LOG_WARNING_MESSAGE(message)                                           \
   do {                                                                        \
      rstudio::core::log::logWarningMessage(                                   \
         message,                                                              \
         rstudio::session::logging::logNameFromFile(__FILE__),                 \
         boost::none,                                                          \
         ERROR_LOCATION);                                                      \
   } while (0)

// DLOG / WLOG / ELOG: convenience macros using fmt::format with automatic
// function name prefix.
#define DLOG(__FMT__, ...)                                                     \
   do {                                                                        \
      LOG_DEBUG_MESSAGE_NAMED(                                                 \
         rstudio::session::logging::logNameFromFile(__FILE__),                 \
         fmt::format("[{}]: " __FMT__, __func__, ##__VA_ARGS__));              \
   } while (0)

#define WLOG(__FMT__, ...)                                                     \
   do {                                                                        \
      LOG_WARNING_MESSAGE_NAMED(                                               \
         rstudio::session::logging::logNameFromFile(__FILE__),                 \
         fmt::format("[{}]: " __FMT__, __func__, ##__VA_ARGS__));              \
   } while (0)

#define ELOG(__FMT__, ...)                                                     \
   do {                                                                        \
      LOG_ERROR_MESSAGE_NAMED(                                                 \
         rstudio::session::logging::logNameFromFile(__FILE__),                 \
         fmt::format("[{}]: " __FMT__, __func__, ##__VA_ARGS__));              \
   } while (0)

#endif // SESSION_MODULES_SESSION_LOGGING_HPP
