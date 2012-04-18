/*
 * Log.cpp
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

#include <core/Log.hpp>

#include <iostream>
#include <sstream>
#include <algorithm>

#include <core/Error.hpp>
#include <core/system/System.hpp>

namespace core {
namespace log {

namespace {   

const char DELIM = ';';
const char * const OCCURRED_AT = "OCCURRED AT";
const char * const LOGGED_FROM = "LOGGED FROM";
const char * const CAUSED_BY = "CAUSED BY";
   
std::string cleanDelims(const std::string& source)
{
   std::string cleanTarget(source);
   std::replace(cleanTarget.begin(), cleanTarget.end(), DELIM, ' ');
   return cleanTarget;
}
   
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
   
void logMessageWithLocation(const std::string& prefix,
                            system::LogLevel logLevel,
                            const std::string& message,
                            const ErrorLocation& loggedFromLocation)
{
   try
   {
      std::ostringstream os ;
      
      // error
      os << prefix << " " << message ;
      
      // log location
      os << DELIM << " " << LOGGED_FROM << ": " 
         << cleanDelims(loggedFromLocation.asString());
      
      system::log(logLevel, os.str()) ;
   }
   catch(...)
   {
      system::log(system::kLogLevelError, 
                  "ERROR unexpected error while logging");
   }
}

}
   
void logError(const Error& error, const ErrorLocation& loggedFromLocation) 
{
   try
   {
      std::ostringstream os ;

      // error
      writeError(error, os) ;
      
      // log location 
      os << DELIM << " " << LOGGED_FROM << ": "  
         << cleanDelims(loggedFromLocation.asString());
            
      system::log( system::kLogLevelError, os.str()) ;
   }
   catch(...)
   {
      system::log(system::kLogLevelError, 
                  "ERROR unexpected error while logging");
   }
}

void logErrorMessage(const std::string& message, 
                     const ErrorLocation& loggedFromLocation) 
{
   logMessageWithLocation("ERROR", 
                          system::kLogLevelError,
                          message,
                          loggedFromLocation);
}
   
void logWarningMessage(const std::string& message,
                       const ErrorLocation& loggedFromLocation)
{
   logMessageWithLocation("WARNING", 
                          system::kLogLevelWarning,
                          message,
                          loggedFromLocation);   
}

void logInfoMessage(const std::string& message)
{
   system::log(system::kLogLevelInfo, message.c_str());
}

void logDebugMessage(const std::string& message)
{
   system::log(system::kLogLevelDebug, message.c_str());
}
   
std::string errorAsLogEntry(const Error& error)
{
   std::ostringstream ostr;
   writeError(error, ostr);
   return ostr.str();
}
   

} // namespace log
} // namespace core 



