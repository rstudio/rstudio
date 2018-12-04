/*
 * Log.cpp
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

#include <core/Log.hpp>

#include <iostream>
#include <sstream>
#include <algorithm>

#include <core/Error.hpp>
#include <core/system/System.hpp>

namespace rstudio {
namespace core {
namespace log {

const char DELIM = ';';

std::string cleanDelims(const std::string& source)
{
   std::string cleanTarget(source);
   std::replace(cleanTarget.begin(), cleanTarget.end(), DELIM, ' ');
   return cleanTarget;
}

namespace {   

const char * const OCCURRED_AT = "OCCURRED AT";
const char * const LOGGED_FROM = "LOGGED FROM";
const char * const CAUSED_BY = "CAUSED BY";

void logMessage(system::LogLevel logLevel,
                const std::string& message,
                const ErrorLocation& loggedFromLocation = ErrorLocation(),
                const std::string& logSection = std::string())
{
   if (logLevel < system::lowestLogLevel())
      return;

   std::string levelStr = logLevelToStr(logLevel);

   try
   {
      std::ostringstream os ;

      // error
      os << levelStr << " " << message ;

      if (loggedFromLocation.hasLocation())
      {
         // log location
         os << DELIM << " " << LOGGED_FROM << ": "
            << cleanDelims(loggedFromLocation.asString());
      }

      system::log(logLevel, os.str(), logSection);
   }
   catch(...)
   {
      system::log(system::kLogLevelError, "ERROR unexpected error while logging", logSection);
   }
}

void logAction(system::LogLevel logLevel,
               const boost::function<std::string()>& action,
               const ErrorLocation& loggedFromLocation = ErrorLocation(),
               const std::string& logSection = std::string())
{
   if (logLevel < system::lowestLogLevel())
      return;

   return logMessage(logLevel, action(), loggedFromLocation, logSection);
}

} // anonymous namespace

void writeError(const Error& error, std::ostream& os)
{
   // build intermediate string so we can remove any embedded instances of
   // DELIM in the output (since this is used as a delimiter for parsing)
   std::ostringstream errorStream ;

   // basics
   const boost::system::error_code& ec = error.code();
   errorStream << "ERROR " << ec.category().name() << " error "
               << ec.value() << " (" << ec.message() << ")"  ;

   // properties
   if ( !error.properties().empty() )
   {
      errorStream << " [" ;
      std::vector<std::pair<std::string,std::string> >::const_iterator
      it = error.properties().begin() ;
      errorStream << it->first << "=" << it->second ;
      ++it ;
      for ( ; it != error.properties().end(); ++it)
         errorStream << ", " << it->first << "=" << it->second ;
      errorStream << "]" ;
   }

   // clean delims and output
   os << cleanDelims(errorStream.str());

   // location
   os << DELIM << " " << OCCURRED_AT << ": "
      << cleanDelims(error.location().asString());

   // cause (recurse)
   if (error.cause() )
   {
      os << DELIM << " " << CAUSED_BY << ": " ;
      writeError(error.cause(), os);
   }
}
   
void logError(const Error& error,
              const ErrorLocation& loggedFromLocation)
{
   logError(std::string(), error, loggedFromLocation);
}

void logError(const std::string& logSection,
              const Error& error,
              const ErrorLocation& loggedFromLocation)
{
   try
   {
      std::ostringstream os ;

      // error
      writeError(error, os) ;

      // log location
      os << DELIM << " " << LOGGED_FROM << ": "
         << cleanDelims(loggedFromLocation.asString());

      system::log(system::kLogLevelError, os.str(), logSection);
   }
   catch(...)
   {
      system::log(system::kLogLevelError,
                  "ERROR unexpected error while logging",
                  logSection);
   }
}

void logErrorMessage(const std::string& message, 
                     const ErrorLocation& loggedFromLocation) 
{
   logMessage(system::kLogLevelError,
              message,
              loggedFromLocation);
}

void logErrorMessage(const std::string& logSection,
                     const std::string& message,
                     const ErrorLocation& loggedFromLocation)
{
  logMessage(system::kLogLevelError,
             message,
             loggedFromLocation,
             logSection);
}
   
void logWarningMessage(const std::string& message,
                       const ErrorLocation& loggedFromLocation)
{
   logMessage(system::kLogLevelWarning,
              message,
              loggedFromLocation);
}

void logWarningMessage(const std::string& logSection,
                       const std::string& message,
                       const ErrorLocation& loggedFromLocation)
{
   logMessage(system::kLogLevelWarning,
              message,
              loggedFromLocation,
              logSection);
}

void logInfoMessage(const std::string& message,
                    const ErrorLocation& loggedFromLocation)
{
   logMessage(system::kLogLevelInfo,
              message,
              loggedFromLocation);
}

void logInfoMessage(const std::string& logSection,
                    const std::string& message,
                    const ErrorLocation& loggedFromLocation)
{
   logMessage(system::kLogLevelInfo,
              message,
              loggedFromLocation,
              logSection);
}

void logDebugMessage(const std::string& message,
                     const ErrorLocation& loggedFromLocation)
{
   logMessage(system::kLogLevelDebug,
              message,
              loggedFromLocation);
}

void logDebugMessage(const std::string& logSection,
                     const std::string& message,
                     const ErrorLocation& loggedFromLocation)
{
   logMessage(system::kLogLevelDebug,
              message,
              loggedFromLocation,
              logSection);
}

void logDebugAction(const boost::function<std::string()>& action,
                    const ErrorLocation& loggedFromLocation)
{
   logAction(system::kLogLevelDebug,
             action,
             loggedFromLocation);
}

void logDebugAction(const std::string& logSection,
                    const boost::function<std::string ()>& action,
                    const ErrorLocation& loggedFromLocation)
{
   logAction(system::kLogLevelDebug,
             action,
             loggedFromLocation,
             logSection);
}
   
std::string errorAsLogEntry(const Error& error)
{
   std::ostringstream ostr;
   writeError(error, ostr);
   return ostr.str();
}
   

} // namespace log
} // namespace core 
} // namespace rstudio



