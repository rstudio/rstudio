/*
 * Log.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include <core/Log.hpp>

#include <iostream>
#include <sstream>
#include <algorithm>

#include <core/system/System.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/SafeConvert.hpp>

namespace rstudio {
namespace core {
namespace log {

namespace {

void logAction(LogLevel logLevel,
               const boost::function<std::string()>& action,
               const ErrorLocation& loggedFromLocation = ErrorLocation(),
               const std::string& logSection = std::string())
{
   switch (logLevel)
   {
      case LogLevel::ERR:
         return logErrorMessage(action(), logSection, loggedFromLocation);
      case LogLevel::WARN:
         return logWarningMessage(action(), logSection, loggedFromLocation);
      case LogLevel::DEBUG:
         return logDebugMessage(action(), logSection, loggedFromLocation);
      case LogLevel::INFO:
         return logInfoMessage(action(), logSection, loggedFromLocation);
      case LogLevel::OFF:
         return;
      default:
      {
         assert(false);
         logErrorMessage(
            "Failed to log action. Invalid log level specified: " +
            safe_convert::numberToString(static_cast<int>(logLevel)));
         return;
      }
   }
}

} // anonymous namespace

void logDebugAction(const boost::function<std::string()>& action,
                    const ErrorLocation& loggedFromLocation)
{
   logAction(LogLevel::DEBUG,
             action,
             loggedFromLocation);
}

void logDebugAction(const std::string& logSection,
                    const boost::function<std::string ()>& action,
                    const ErrorLocation& loggedFromLocation)
{
   logAction(LogLevel::DEBUG,
             action,
             loggedFromLocation,
             logSection);
}
   
std::string errorAsLogEntry(const Error& error)
{
   return writeError(error);
}

} // namespace log
} // namespace core 
} // namespace rstudio



