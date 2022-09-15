/*
 * Log.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
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

#ifndef CORE_LOG_HPP
#define CORE_LOG_HPP

#include <fmt/format.h>

#include <string>

#include <shared_core/Error.hpp>
#include <shared_core/Logger.hpp>

namespace rstudio {
namespace core {
namespace log {

enum class LoggerType
{
   kStdErr = 0,
   kSysLog = 1,
   kFile = 2
};

std::string errorAsLogEntry(const Error& error);

// Macros for automatic inclusion of ERROR_LOCATION and easy ability to 
// compile out logging calls

#define LOG_ERROR(error) rstudio::core::log::logError(error, \
                                                      ERROR_LOCATION)

#define LOG_ERROR_NAMED(logSection, error) rstudio::core::log::logError(logSection, \
                                                                        error, \
                                                                        ERROR_LOCATION)

#define LOG_ERROR_MESSAGE(message) rstudio::core::log::logErrorMessage(message, \
                                                                       ERROR_LOCATION)

#define LOG_ERROR_MESSAGE_WITH_PROPS(message, props) rstudio::core::log::logErrorMessage(message, \
                                                                                         std::string(), \
                                                                                         props, \
                                                                                         ERROR_LOCATION)

#define LOG_ERROR_MESSAGE_NAMED(logSection, message) rstudio::core::log::logErrorMessage(message, \
                                                                                         logSection, \
                                                                                         boost::none, \
                                                                                         ERROR_LOCATION)

#define LOG_WARNING_MESSAGE(message) rstudio::core::log::logWarningMessage(message, \
                                                                           ERROR_LOCATION)

#define LOG_WARNING_MESSAGE_WITH_PROPS(message, props) rstudio::core::log::logWarningMessage(message, \
                                                                                             std::string(), \
                                                                                             props, \
                                                                                             ERROR_LOCATION)

#define LOG_WARNING_MESSAGE_NAMED(logSection, message) rstudio::core::log::logWarningMessage(message, \
                                                                                             logSection, \
                                                                                             boost::none, \
                                                                                             ERROR_LOCATION)

#define LOG_INFO_MESSAGE(message) rstudio::core::log::logInfoMessage(message)

#define LOG_INFO_MESSAGE_WITH_PROPS(message, props) rstudio::core::log::logInfoMessage(message, \
                                                                                       std::string(), \
                                                                                       props, \
                                                                                       ErrorLocation())

#define LOG_INFO_MESSAGE_NAMED(logSection, message) rstudio::core::log::logInfoMessage(message, \
                                                                                       logSection)

#define LOG_DEBUG_MESSAGE(message) rstudio::core::log::logDebugMessage(message)

#define LOG_DEBUG_MESSAGE_WITH_PROPS(message, props) rstudio::core::log::logDebugMessage(message, \
                                                                                         std::string(), \
                                                                                         props, \
                                                                                         ErrorLocation())

#define LOG_DEBUG_MESSAGE_NAMED(logSection, message) rstudio::core::log::logDebugMessage(message, \
                                                                                         logSection)

#define LOG_DEBUG_ACTION_NAMED(logSection, action) rstudio::core::log::logDebugAction(logSection, \
                                                                                      action)

#define LOG_PASSTHROUGH_MESSAGE(source, message) rstudio::core::log::logPassthroughMessage(source, message)

#define DLOGF(__FMT__, ...)                                                    \
  do {                                                                         \
    std::string message = fmt::format(FMT_STRING(__FMT__), ##__VA_ARGS__);     \
    ::rstudio::core::log::logDebugMessage(message);                            \
  } while (0)

#define ILOGF(__FMT__, ...)                                                    \
  do {                                                                         \
    std::string message = fmt::format(FMT_STRING(__FMT__), ##__VA_ARGS__);     \
    ::rstudio::core::log::logInfoMessage(message);                             \
  } while (0)

#define WLOGF(__FMT__, ...)                                                    \
  do {                                                                         \
    std::string message = fmt::format(FMT_STRING(__FMT__), ##__VA_ARGS__);     \
    ::rstudio::core::log::logWarningMessage(message);                          \
  } while (0)

#define ELOGF(__FMT__, ...)                                                    \
  do {                                                                         \
    std::string message = fmt::format(FMT_STRING(__FMT__), ##__VA_ARGS__);     \
    ::rstudio::core::log::logErrorMessage(message);                            \
  } while (0)


// define named logging sections
#define kFileLockingLogSection "file-locking"

} // namespace log
} // namespace core
} // namespace rstudio

#endif // CORE_LOG_HPP
