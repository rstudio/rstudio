/*
 * JsonRpc.cpp
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

#include <core/json/JsonRpc.hpp>

#include <sstream>

#include <core/Log.hpp>
#include <core/http/Response.hpp>


namespace rstudio {
namespace core {
namespace json {

const char * const kRpcResult = "result";
const char * const kRpcAsyncHandle = "asyncHandle";
const char * const kRpcError = "error";
const char * const kJsonContentType = "application/json" ;   
   
Error parseJsonRpcRequest(const std::string& input, JsonRpcRequest* pRequest) 
{
   // json_spirit is not documented to throw an exceptions but surround 
   // the code with an exception handling block just to be defensive...
   try 
   {
      // parse data and verify it contains an object
      json::Value var;
      if ( !json::parse(input, &var) || 
           (var.type() != json::ObjectType) )
      {
         return Error(errc::InvalidRequest, ERROR_LOCATION) ;
      }

      // extract the fields
      json::Object& requestObject = var.get_obj();
      for (json::Object::const_iterator it = 
            requestObject.begin(); it != requestObject.end(); ++it)
      {
         std::string fieldName = it->first ;
         json::Value fieldValue = it->second ;

         if ( fieldName == "method" )
         {
            if (fieldValue.type() != json::StringType)
               return Error(errc::InvalidRequest, ERROR_LOCATION) ;

            pRequest->method = fieldValue.get_str() ;
         }
         else if ( fieldName == "params" )
         {
            if (fieldValue.type() != json::ArrayType)
               return Error(errc::ParamTypeMismatch, ERROR_LOCATION) ;

            pRequest->params = fieldValue.get_array();
         }
         else if ( fieldName == "kwparams" )
         {
            if (fieldValue.type() != json::ObjectType)
               return Error(errc::ParamTypeMismatch, ERROR_LOCATION) ;

            pRequest->kwparams = fieldValue.get_obj();
         }
         else if (fieldName == "sourceWnd")
         {
            if (fieldValue.type() != json::StringType)
               return Error(errc::InvalidRequest, ERROR_LOCATION);

            pRequest->sourceWindow = fieldValue.get_str();
         }
         else if (fieldName == "clientId" )
         {
            if (fieldValue.type() != json::StringType)
               return Error(errc::InvalidRequest, ERROR_LOCATION);
            
            pRequest->clientId = fieldValue.get_str();
         }
         else if (fieldName == "version" )
         {
            if (!json::isType<double>(fieldValue))
               return Error(errc::InvalidRequest, ERROR_LOCATION);
            
            pRequest->version = fieldValue.get_value<double>();
         }
      }

      // method is required
      if (pRequest->method.empty() )
         return Error(errc::InvalidRequest, ERROR_LOCATION) ;

      return Success() ;
   }
   catch(const std::exception& e)
   {
      Error error = Error(errc::ParseError, ERROR_LOCATION);
      error.addProperty("exception", e.what()) ;
      return error ;
   }
}

bool parseJsonRpcRequestForMethod(const std::string& input, 
                                  const std::string& method,
                                  json::JsonRpcRequest* pRequest,
                                  http::Response* pResponse)
{
   // parse request
   Error parseError = parseJsonRpcRequest(input, pRequest) ;
   if (parseError)
   {
      LOG_ERROR(parseError);
      setJsonRpcError(parseError, pResponse);
      return false;
   }
   
   // check for method
   if (pRequest->method != method)
   {
      Error methodError = Error(errc::MethodNotFound, ERROR_LOCATION);
      methodError.addProperty("method", pRequest->method);
      LOG_ERROR(methodError);
      setJsonRpcError(methodError, pResponse);
      return false;
   }
   
   // success
   return true ;
}

namespace  {

void copyErrorCodeToJsonError(const boost::system::error_code& code,
                              json::Object* pError)
{
   pError->operator[]("code") = code.value();
   pError->operator[]("message") = code.message();
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
   
json::Object JsonRpcResponse::getRawResponse()
{
   return response_;
}
   
void JsonRpcResponse::write(std::ostream& os) const
{
   json::write(response_, os);
}
   
void JsonRpcResponse::setError(const Error& error, const json::Value& clientInfo)
{
   // remove result
   response_.erase(kRpcResult);
   response_.erase(kRpcAsyncHandle);

   const boost::system::error_code& ec = error.code();
   
   if ( ec.category() == jsonRpcCategory() )
   {
      setError(ec);
   }
   else
   {
      // execution error
      json::Object jsonError ;
      copyErrorCodeToJsonError(errc::ExecutionError, &jsonError);
      
      // populate sub-error field with error details
      json::Object executionError;
      executionError["code"] = ec.value();
      std::string errorCategoryName = ec.category().name();
      executionError["category"] = errorCategoryName;
      std::string errorMessage = ec.message();
      executionError["message"] = errorMessage;
      jsonError["error"] = executionError;
      if (!clientInfo.is_null())
      {
         jsonError["client_info"] = clientInfo;
      }

      // set error
      setField(kRpcError, jsonError);
   }      
}
   
void JsonRpcResponse::setError(const Error& error)
{   
   setError(error, json::Value());
}
   
void JsonRpcResponse::setError(const boost::system::error_code& ec)
{
   // remove result
   response_.erase(kRpcResult);
   response_.erase(kRpcAsyncHandle);

   // error from error code
   json::Object error ;
   copyErrorCodeToJsonError(ec, &error);
   
   // sub-error is null
   error["error"] = json::Value();
   
   // set error
   setField(kRpcError, error);
}
   
void JsonRpcResponse::setAsyncHandle(const std::string& handle)
{
   response_.erase(kRpcResult);
   response_.erase(kRpcError);

   setField(kRpcAsyncHandle, handle);
}

void setJsonRpcResponse(const core::json::JsonRpcResponse& jsonRpcResponse,
                        core::http::Response* pResponse)
{
   // no cache!
   pResponse->setNoCacheHeaders();
   
   // set content type if necessary (allows callers to override the 
   // default application/json content-type, which is necessary in some
   // circumstances such as returning results to the GWT FileUpload widget
   // (which expects text/html)
   if (pResponse->contentType().empty())
       pResponse->setContentType(kJsonContentType) ; 
   
   // set body 
   std::stringstream responseStream ;
   jsonRpcResponse.write(responseStream);
   Error error = pResponse->setBody(responseStream);
   
   // report error to client if one occurred
   if (error)
   {
      LOG_ERROR(error);
      pResponse->setError(http::status::InternalServerError,
                          error.code().message());
   }
}     
   
 
class JsonRpcErrorCategory : public boost::system::error_category
{
public:
   virtual const char * name() const BOOST_NOEXCEPT;
   virtual std::string message( int ev ) const;
};

const boost::system::error_category& jsonRpcCategory()
{
   static JsonRpcErrorCategory jsonRpcErrorCategoryConst ;
   return jsonRpcErrorCategoryConst ;
}

const char * JsonRpcErrorCategory::name() const BOOST_NOEXCEPT
{
   return "jsonrpc" ;
}

std::string JsonRpcErrorCategory::message( int ev ) const
{
   switch(ev)
   {
      case errc::Success: 
         return "Method call succeeded" ;

      case errc::ConnectionError:
         return "Unable to connect to service" ;
         
      case errc::Unavailable:
         return "Service currently unavailable";
      
      case errc::Unauthorized:
         return "Client unauthorized" ;
         
      case errc::InvalidClientId:
         return "Invalid client id";
      
      case errc::ParseError:
         return "Invalid json or unexpected error occurred while parsing" ;

      case errc::InvalidRequest:
         return "Invalid json-rpc request" ;

      case errc::MethodNotFound:
         return "Method not found"  ;

      case errc::ParamMissing:
         return "Parameter missing" ;
         
      case errc::ParamTypeMismatch:
         return "Parameter type mismatch" ;
         
      case errc::ParamInvalid:
         return "Parameter value invalid";

      case errc::MethodUnexpected:
         return "Unexpected call to method" ;

      case errc::ExecutionError:
         return "Error occurred while executing method" ;
         
      case errc::TransmissionError:
         return "Error occurred during transmission";
         
      case errc::InvalidClientVersion:
         return "Invalid client version";
         
      case errc::ServerOffline:
         return "Server is offline";

      default:
         BOOST_ASSERT(false);
         return "Unknown error type" ;
   }
}

namespace {

void runSynchronousFunction(const JsonRpcFunction& func,
                            const core::json::JsonRpcRequest& request,
                            const JsonRpcFunctionContinuation& continuation)
{
   core::json::JsonRpcResponse response;
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



