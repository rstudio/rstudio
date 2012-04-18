/*
 * Log.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef CORE_LOG_HPP
#define CORE_LOG_HPP

#include <string>

namespace core {

class Error ;
class ErrorLocation ;
   
namespace log {
        
void logError(const Error& error, const ErrorLocation& loggedFromLocation) ;
   
void logErrorMessage(const std::string& message, 
                     const ErrorLocation& loggedFromlocation);
   
void logWarningMessage(const std::string& message,
                       const ErrorLocation& loggedFromLocation);
      
void logInfoMessage(const std::string& message);
   
void logDebugMessage(const std::string& message);
   
std::string errorAsLogEntry(const Error& error);  
  
} // namespace log
} // namespace core 

// Macros for automatic inclusion of ERROR_LOCATION and easy ability to 
// compile out logging calls

#define LOG_ERROR(error) core::log::logError(error, ERROR_LOCATION) 

#define LOG_ERROR_MESSAGE(message) core::log::logErrorMessage(message, \
                                                              ERROR_LOCATION)

#define LOG_WARNING_MESSAGE(message) core::log::logWarningMessage( \
                                                               message, \
                                                               ERROR_LOCATION)

#define LOG_INFO_MESSAGE(message) core::log::logInfoMessage(message)

#define LOG_DEBUG_MESSAGE(message) core::log::logDebugMessage(message)

#endif // CORE_LOG_HPP

