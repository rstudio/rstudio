/*
 * Log.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef CORE_LOG_HPP
#define CORE_LOG_HPP

#include <string>

#include <core/Error.hpp>
#include <boost/function.hpp>

namespace rstudio {
namespace core {

enum LoggerType
{
   kLoggerTypeStdErr = 0,
   kLoggerTypeSysLog = 1,
   kLoggerTypeFile = 2
};

namespace log {

extern const char DELIM;
std::string cleanDelims(const std::string& source);

void writeError(const Error& error,
                std::ostream& os);

void logError(const Error& error,
              const ErrorLocation& loggedFromLocation);

void logError(const std::string& logSection,
              const Error& error,
              const ErrorLocation& loggedFromLocation);
   
void logErrorMessage(const std::string& message, 
                     const ErrorLocation& loggedFromlocation);

void logErrorMessage(const std::string& logSection,
                     const std::string& message,
                     const ErrorLocation& loggedFromLocation);
   
void logWarningMessage(const std::string& message,
                       const ErrorLocation& loggedFromLocation);

void logWarningMessage(const std::string& logSection,
                       const std::string& message,
                       const ErrorLocation& loggedFromLocation);
      
void logInfoMessage(const std::string& message,
                    const ErrorLocation& loggedFromLocation = ErrorLocation());

void logInfoMessage(const std::string& logSection,
                    const std::string& message,
                    const ErrorLocation& loggedFromLocation = ErrorLocation());
   
void logDebugMessage(const std::string& message,
                     const ErrorLocation& loggedFromLocation = ErrorLocation());

void logDebugAction(const boost::function<std::string()>& action,
                    const ErrorLocation& loggedFromLocation = ErrorLocation());

void logDebugMessage(const std::string& logSection,
                     const std::string& message,
                     const ErrorLocation& loggedFromLocation = ErrorLocation());
   
std::string errorAsLogEntry(const Error& error);  
  
} // namespace log
} // namespace core 
} // namespace rstudio

// Macros for automatic inclusion of ERROR_LOCATION and easy ability to 
// compile out logging calls

#define LOG_ERROR(error) rstudio::core::log::logError(error, \
                                                      ERROR_LOCATION)

#define LOG_ERROR_NAMED(logSection, error) rstudio::core::log::logError(logSection, \
                                                                        error, \
                                                                        ERROR_LOCATION)

#define LOG_ERROR_MESSAGE(message) rstudio::core::log::logErrorMessage(message, \
                                                                       ERROR_LOCATION)

#define LOG_ERROR_MESSAGE_NAMED(logSection, message) rstudio::core::log::logErrorMessage(logSection, \
                                                                                         message, \
                                                                                         ERROR_LOCATION)

#define LOG_WARNING_MESSAGE(message) rstudio::core::log::logWarningMessage(message, \
                                                                           ERROR_LOCATION)

#define LOG_WARNING_MESSAGE_NAMED(logSection, message) rstudio::core::log::logWarningMessage(logSection, \
                                                                                             message, \
                                                                                             ERROR_LOCATION)

#define LOG_INFO_MESSAGE(message) rstudio::core::log::logInfoMessage(message)

#define LOG_INFO_MESSAGE_NAMED(logSection, message) rstudio::core::log::logInfoMessage(logSection, \
                                                                                       message)

#define LOG_DEBUG_MESSAGE(message) rstudio::core::log::logDebugMessage(message)

#define LOG_DEBUG_ACTION(action) rstudio::core::log::logDebugAction(action)

#define LOG_DEBUG_MESSAGE_NAMED(logSection, message) rstudio::core::log::logDebugMessage(logSection, \
                                                                                         message)

#define LOG_DEBUG_ACTION_NAMED(logSection, action) rstudio::core::log::logDebugAction(logSection, \
                                                                                      action)

// define named logging sections
#define kFileLockingLogSection "file-locking"

#endif // CORE_LOG_HPP

