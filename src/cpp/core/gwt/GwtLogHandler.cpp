/*
 * GwtLogHandler.cpp
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

#include <core/gwt/GwtLogHandler.hpp>

#include <boost/format.hpp>
#include <boost/lexical_cast.hpp>

#include <core/Log.hpp>
#include <core/SafeConvert.hpp>
#include <core/system/System.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <core/json/JsonRpc.hpp>

namespace core {
namespace gwt {

void handleLogRequest(const std::string& username,
                      const http::Request& request, 
                      http::Response* pResponse)
{
   // parse log method
   json::JsonRpcRequest jsonRpcRequest;
   if (!parseJsonRpcRequestForMethod(request.body(),
                                     "log",
                                     &jsonRpcRequest,
                                     pResponse) )
   {
      return;
   }
   
   // read params
   int level = 0;
   std::string message ;
   Error error = json::readParams(jsonRpcRequest.params, &level, &message);
   if (error)
   {
      LOG_ERROR(error);
      json::setJsonRpcError(error, pResponse);
      return;
   }
   
   // convert level to appropriate enum and str
   using namespace core::system;
   LogLevel logLevel;
   std::string logLevelStr;
   switch(level)
   {
      case 0:
         logLevel = kLogLevelError; 
         logLevelStr = "ERROR";
         break;
      case 1:
         logLevel = kLogLevelWarning;
         logLevelStr = "WARNING";
         break;
      case 2:
         logLevel = kLogLevelInfo;
         logLevelStr = "INFO";
         break;
      default:
         LOG_WARNING_MESSAGE("Unexpected log level: " + 
                             safe_convert::numberToString(level));
         logLevel = kLogLevelError; 
         logLevelStr = "ERROR";
         break;
   }
   
   // form the log entry
   boost::format fmt("CLIENT %1% (%2%-%3%): %4%; USER-AGENT: %5%");
   std::string logEntry = boost::str(fmt % logLevelStr %
                                           username %
                                           jsonRpcRequest.clientId %
                                           message %
                                           request.userAgent());
   
   
   // log it
   core::system::log(logLevel, logEntry);
   
   // set void result
   json::setVoidJsonRpcResult(pResponse);
}

} // namespace gwt
} // namespace core


