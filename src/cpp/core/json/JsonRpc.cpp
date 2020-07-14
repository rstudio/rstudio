/*
 * JsonRpc.cpp
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

#include <core/json/JsonRpc.hpp>

#include <sstream>

#include <core/Log.hpp>
#include <core/system/Process.hpp>
#include <core/http/Response.hpp>

namespace rstudio {
namespace core {
namespace json {

const char * const kRpcResult = "result";
const char * const kRpcAsyncHandle = "asyncHandle";
const char * const kRpcError = "error";
const char * const kJsonContentType = "application/json";

Error parseJsonRpcRequest(const std::string& input, JsonRpcRequest* pRequest) 
{
   try 
   {
      // parse data and verify it contains an object
      Value var;
      if ( var.parse(input) || !var.isObject() )
      {
         return Error(json::errc::InvalidRequest, ERROR_LOCATION);
      }

      // extract the fields
      const Object& requestObject = var.getObject();
      for (Object::Iterator it =
            requestObject.begin(); it != requestObject.end(); ++it)
      {
         std::string fieldName = (*it).getName();
         Value fieldValue = (*it).getValue();

         if ( fieldName == "method" )
         {
            if (!fieldValue.isString())
               return Error(json::errc::InvalidRequest, ERROR_LOCATION);

            pRequest->method = fieldValue.getString();
         }
         else if ( fieldName == "params" )
         {
            if (!fieldValue.isArray())
               return Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);

            pRequest->params = fieldValue.getValue<json::Array>();
         }
         else if ( fieldName == "kwparams" )
         {
            if (!fieldValue.isObject())
               return Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);

            pRequest->kwparams = fieldValue.getValue<json::Object>();
         }
         else if (fieldName == "sourceWnd")
         {
            if (!fieldValue.isString())
               return Error(json::errc::InvalidRequest, ERROR_LOCATION);

            pRequest->sourceWindow = fieldValue.getString();
         }
         else if (fieldName == "clientId" )
         {
            if (!fieldValue.isString())
               return Error(json::errc::InvalidRequest, ERROR_LOCATION);
            
            pRequest->clientId = fieldValue.getString();
         }
         // legacy version field
         else if (fieldName == "version" )
         {
            if (isType<double>(fieldValue))
               pRequest->version = fieldValue.getDouble();
            else
               pRequest->version = 0;
         }
         // new version field
         else if (fieldName == "clientVersion")
         {
            if (!fieldValue.isString())
               pRequest->clientVersion = fieldValue.getString();
            else
               pRequest->clientVersion = std::string();
         }
      }

      // method is required
      if (pRequest->method.empty() )
         return Error(json::errc::InvalidRequest, ERROR_LOCATION);

      return Success();
   }
   catch(const std::exception& e)
   {
      Error error = Error(json::errc::ParseError, ERROR_LOCATION);
      error.addProperty("exception", e.what());
      return error;
   }
}

void setErrorResponse(const core::Error& error, core::json::JsonRpcResponse* pResponse)
{
   pResponse->setError(error, core::errorMessage(error));
}

void setProcessErrorResponse(const core::system::ProcessResult& result,
                             const core::ErrorLocation& location,
                             core::json::JsonRpcResponse* pResponse)
{
   Error error = systemError(boost::system::errc::state_not_recoverable, result.stdErr, location);
   pResponse->setError(error, json::Value(result.stdErr), true);
}

bool parseJsonRpcRequestForMethod(const std::string& input, 
                                  const std::string& method,
                                  JsonRpcRequest* pRequest,
                                  http::Response* pResponse)
{
   // parse request
   Error parseError = parseJsonRpcRequest(input, pRequest);
   if (parseError)
   {
      LOG_ERROR(parseError);
      setJsonRpcError(parseError, pResponse);
      return false;
   }
   
   // check for method
   if (pRequest->method != method)
   {
      Error methodError = Error(json::errc::MethodNotFound, ERROR_LOCATION);
      methodError.addProperty("method", pRequest->method);
      LOG_ERROR(methodError);
      setJsonRpcError(methodError, pResponse);
      return false;
   }
   
   // success
   return true;
}

namespace  {

void copyErrorCodeToJsonError(const boost::system::error_code& code,
                              Object* pError)
{
   pError->operator[]("code") = code.value();
   pError->operator[]("message") = code.message();
}   

void setErrorProperties(Object& jsonError,
                        const Error& error)
{
   if (error.getProperties().empty())
      return;

   Object properties;
   for (const std::pair<std::string, std::string>& property : error.getProperties())
   {
      properties[property.first] = property.second;
   }

   jsonError["properties"] = properties;
}

}   

void JsonRpcResponse::setAfterResponse(
                           const boost::function<void()>& afterResponse)
{
   afterResponse_ = afterResponse;
}
   
bool JsonRpcResponse::hasAfterResponse() const
{
   return afterResponse_;
}
   
   
void JsonRpcResponse::runAfterResponse()
{
   if (afterResponse_)
      afterResponse_();
}
   
Object JsonRpcResponse::getRawResponse()
{
   return response_;
}
   
void JsonRpcResponse::write(std::ostream& os) const
{
   response_.write(os);
}
   
void JsonRpcResponse::setError(const Error& error,
                               const Value& clientInfo,
                               bool includeErrorProperties)
{
   // remove result
   response_.erase(json::kRpcResult);
   response_.erase(json::kRpcAsyncHandle);
   
   if (error.getName() == json::jsonRpcCategory().name())
   {
      setError(boost::system::error_code(error.getCode(), json::jsonRpcCategory()),
               json::Value());
   }
   else
   {
      // execution error
      Object jsonError;
      copyErrorCodeToJsonError(json::errc::ExecutionError, &jsonError);
      
      // populate sub-error field with error details
      Object executionError;
      executionError["code"] = error.getCode();
      
      std::string errorCategoryName = error.getName();
      executionError["category"] = errorCategoryName;
      
      std::string errorMessage = error.getMessage();
      executionError["message"] = errorMessage;
      
      if (error.getLocation().hasLocation())
      {
         std::string errorLocation = error.getLocation().asString();
         executionError["location"] = errorLocation;
      }
      
      jsonError["error"] = executionError;
      if (!clientInfo.isNull())
      {
         jsonError["client_info"] = clientInfo;
      }

      // add error properties if requested
      if (includeErrorProperties)
         setErrorProperties(jsonError, error);

      // set error
      setField(json::kRpcError, jsonError);
   }      
}
   
void JsonRpcResponse::setError(const Error& error,
                               bool includeErrorProperties)
{   
   setError(error, Value(), includeErrorProperties);
}

void JsonRpcResponse::setError(const Error& error,
                               const char* message,
                               bool includeErrorProperties)
{   
   setError(error, Value(message), includeErrorProperties);
}

void JsonRpcResponse::setError(const Error& error,
                               const std::string& message,
                               bool includeErrorProperties)
{   
   setError(error, Value(message), includeErrorProperties);
}

   
void JsonRpcResponse::setError(const boost::system::error_code& ec,
                               const Value& clientInfo)
{
   // remove result
   response_.erase(json::kRpcResult);
   response_.erase(json::kRpcAsyncHandle);

   // error from error code
   Object error;
   copyErrorCodeToJsonError(ec, &error);

   // client info if provided
   if (!clientInfo.isNull())
   {
      error["client_info"] = clientInfo;
   }
   
   // sub-error is null
   error["error"] = Value();

   // set error
   setField(json::kRpcError, error);
}

void JsonRpcResponse::setRedirectError(const Error& error,
                                       const std::string& redirectUrl)
{
   // set a standard error
   setError(error);

   Object errorObj;
   getField(json::kRpcError, &errorObj);

   // extend the error with redirect information
   errorObj["redirect_url"] = redirectUrl;
   setField(json::kRpcError, errorObj);
}
   
void JsonRpcResponse::setAsyncHandle(const std::string& handle)
{
   response_.erase(json::kRpcResult);
   response_.erase(json::kRpcError);

   setField(json::kRpcAsyncHandle, handle);
}

void setJsonRpcResponse(const JsonRpcResponse& jsonRpcResponse,
                        core::http::Response* pResponse)
{
   // no cache!
   pResponse->setNoCacheHeaders();
   
   // set content type if necessary (allows callers to override the 
   // default application/json content-type, which is necessary in some
   // circumstances such as returning results to the GWT FileUpload widget
   // (which expects text/html)
   if (pResponse->contentType().empty())
       pResponse->setContentType(json::kJsonContentType);
   
   // set body 
   std::stringstream responseStream;
   jsonRpcResponse.write(responseStream);
   Error error = pResponse->setBody(responseStream);
   
   // report error to client if one occurred
   if (error)
   {
      LOG_ERROR(error);
      pResponse->setError(http::status::InternalServerError,
                          error.getMessage());
   }
}     

bool JsonRpcResponse::parse(const std::string& input,
                            JsonRpcResponse* pResponse)
{
   Value value;
   if (value.parse(input))
      return false;

   return parse(value, pResponse);
}

bool JsonRpcResponse::parse(const Value& value,
                            JsonRpcResponse* pResponse)
{
   if (!value.isObject())
      return false;

   pResponse->response_ = value.getValue<json::Object>();
   return true;
}

class JsonRpcErrorCategory : public boost::system::error_category
{
public:
   virtual const char * name() const BOOST_NOEXCEPT;
   virtual std::string message( int ev ) const;
};

const boost::system::error_category& jsonRpcCategory()
{
   static JsonRpcErrorCategory jsonRpcErrorCategoryConst;
   return jsonRpcErrorCategoryConst;
}

const char * JsonRpcErrorCategory::name() const BOOST_NOEXCEPT
{
   return "jsonrpc";
}

std::string JsonRpcErrorCategory::message( int ev ) const
{
   switch(ev)
   {
      case errc::Success:
         return "Method call succeeded";

      case errc::ConnectionError:
         return "Unable to connect to service";

      case errc::Unavailable:
         return "Service currently unavailable";

      case errc::Unauthorized:
         return "Client unauthorized";

      case errc::InvalidClientId:
         return "Invalid client id";

      case errc::ParseError:
         return "Invalid json or unexpected error occurred while parsing";

      case errc::InvalidRequest:
         return "Invalid json-rpc request";

      case errc::MethodNotFound:
         return "Method not found";

      case errc::ParamMissing:
         return "Parameter missing";

      case errc::ParamTypeMismatch:
         return "Parameter type mismatch";

      case errc::ParamInvalid:
         return "Parameter value invalid";

      case errc::MethodUnexpected:
         return "Unexpected call to method";

      case errc::ExecutionError:
         return "Error occurred while executing method";

      case errc::TransmissionError:
         return "Error occurred during transmission";

      case errc::InvalidClientVersion:
         return "Invalid client version";

      case errc::ServerOffline:
         return "Server is offline";

      case errc::InvalidSession:
         return "Invalid session";

      case errc::MaxSessionsReached:
         return "The maximum amount of concurrent sessions for this license has been reached";

      case errc::MaxUsersReached:
         return "The maximum amount of concurrent users for this license has been reached";

      case errc::LaunchParametersMissing:
         return "Launch parameters for launcher session missing and should be resent";

      case errc::LimitSessionsReached:
         return "The maximum amount of concurrent session allowed for the user profile has been reached";

      default:
         BOOST_ASSERT(false);
         return "Unknown error type";
   }
}

namespace {

void runSynchronousFunction(const JsonRpcFunction& func,
                            const JsonRpcRequest& request,
                            const JsonRpcFunctionContinuation& continuation)
{
   JsonRpcResponse response;
   if (request.isBackgroundConnection)
      response.setSuppressDetectChanges(true);
   core::Error error = func(request, &response);
   continuation(error, &response);
}

} // anonymous namespace

JsonRpcAsyncFunction adaptToAsync(JsonRpcFunction synchronousFunction)
{
   return boost::bind(runSynchronousFunction, synchronousFunction, _1, _2);
}

JsonRpcAsyncMethod adaptMethodToAsync(JsonRpcMethod synchronousMethod)
{
   return JsonRpcAsyncMethod(
         synchronousMethod.first,
         std::make_pair(true, adaptToAsync(synchronousMethod.second)));
}

} // namespace json
} // namespace core
} // namespace rstudio



