/*
 * GwtLogHandler.cpp
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

#include <core/gwt/GwtLogHandler.hpp>

#include <boost/format.hpp>
#include <boost/foreach.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Log.hpp>
#include <core/SafeConvert.hpp>
#include <core/system/System.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <core/json/JsonRpc.hpp>

#include <core/gwt/GwtSymbolMaps.hpp>

namespace core {
namespace gwt {

namespace {

// symbol maps
SymbolMaps s_symbolMaps;

// client exception
struct ClientException
{
   std::string message;
   std::string strongName;
   std::vector<StackElement> stack;
};

Error parseClientException(const json::Object exJson, ClientException* pEx)
{
   json::Array stackJson;
   Error error = json::readObject(exJson,
                                  "message", &(pEx->message),
                                  "strong_name", &(pEx->strongName),
                                  "stack", &stackJson);
   if (error)
       return error;

   BOOST_FOREACH(const json::Value& elementJson, stackJson)
   {
      if (!json::isType<json::Object>(elementJson))
         return Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);

      StackElement element;
      Error error = json::readObject(elementJson.get_obj(),
                                     "file_name", &element.fileName,
                                     "class_name", &element.className,
                                     "method_name", &element.methodName,
                                     "line_number", &element.lineNumber);
      if (error)
         return error;

      pEx->stack.push_back(element);
   }

   return Success();
}


std::string formatMethod(const std::string& method)
{
   if (!method.empty() && method[0] == '$')
      return method.substr(1);
   else
      return method;
}

bool isExceptionMechanismElement(const StackElement& element)
{
   return boost::algorithm::ends_with(element.methodName, "fillInStackTrace")

          ||

          (boost::algorithm::starts_with(element.fileName,
                                        "com/google/gwt/emul/java/lang/")
          &&

          boost::algorithm::ends_with(element.fileName,
                                      "Exception.java"));
}

void handleLogExceptionRequest(const std::string& username,
                               const std::string& userAgent,
                               const json::JsonRpcRequest& jsonRpcRequest,
                               http::Response* pResponse)
{
   // read client exception json object
   json::Object exJson;
   Error error = json::readParam(jsonRpcRequest.params, 0, &exJson);
   if (error)
   {
      LOG_ERROR(error);
      json::setJsonRpcError(error, pResponse);
      return;
   }
   

   // parse
   ClientException ex;
   error = parseClientException(exJson, &ex);
   if (error)
   {
      LOG_ERROR(error);
      json::setJsonRpcError(error, pResponse);
      return;
   }

   // resymbolize the stack
   std::vector<StackElement> stack = s_symbolMaps.resymbolize(ex.stack,
                                                              ex.strongName);

   // build the log message
   bool printFrame = false;
   std::ostringstream ostr;
   ostr << ex.message << std::endl;
   BOOST_FOREACH(const StackElement& element, stack)
   {
      // skip past java/lang/Exception entries
      if (!printFrame)
      {
         if (!isExceptionMechanismElement(element))
            printFrame = true;
      }

      if (printFrame)
      {
         ostr << element.fileName << "#" << element.lineNumber
              << "::" << formatMethod(element.methodName)
              << std::endl;
      }
   }

   // form the log entry
   boost::format fmt("CLIENT EXCEPTION (%1%-%2%, %3%): %4%");
   std::string logEntry = boost::str(fmt % username
                                         % jsonRpcRequest.clientId
                                         % userAgent
                                         % ostr.str());

   // log it
   core::system::log(core::system::kLogLevelError, logEntry);


   // set void result
   json::setVoidJsonRpcResult(pResponse);
}

void handleLogMessageRequest(const std::string& username,
                             const std::string& userAgent,
                             const json::JsonRpcRequest& jsonRpcRequest,
                             http::Response* pResponse)
{
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
   
   
                                           userAgent);
   // log it
   core::system::log(logLevel, logEntry);
   
   // set void result
   json::setVoidJsonRpcResult(pResponse);
}


} // anonymous namespace

void initializeSymbolMaps(const core::FilePath& symbolMapsPath)
{
   Error error = s_symbolMaps.initialize(symbolMapsPath);
   if (error)
      LOG_ERROR(error);
}

void handleLogRequest(const std::string& username,
                      const http::Request& request, 
                      http::Response* pResponse)
{
   // parse request
   json::JsonRpcRequest jsonRpcRequest;
   Error parseError = parseJsonRpcRequest(request.body(), &jsonRpcRequest) ;
   if (parseError)
   {
      LOG_ERROR(parseError);
      json::setJsonRpcError(parseError, pResponse);
      return;
   }

   // check for supported methods
   if (jsonRpcRequest.method == "log")
   {
      handleLogMessageRequest(username,
                              request.userAgent(),
                              jsonRpcRequest,
                              pResponse);
   }
   else if (jsonRpcRequest.method == "log_exception")
   {
      handleLogExceptionRequest(username,
                                request.userAgent(),
                                jsonRpcRequest,
                                pResponse);
   }
   else
   {
      Error methodError = Error(json::errc::MethodNotFound, ERROR_LOCATION);
      methodError.addProperty("method", jsonRpcRequest.method);
      LOG_ERROR(methodError);
      json::setJsonRpcError(methodError, pResponse);
      return;
   }
}


} // namespace gwt
} // namespace core


