/*
 * JsonRpc.hpp
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

#ifndef CORE_JSON_RPC_HPP
#define CORE_JSON_RPC_HPP

#include <boost/system/error_code.hpp>

#include <core/type_traits/TypeTraits.hpp>
#include <core/json/JsonRpc.hpp>

#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
namespace system {
   struct ProcessResult;
}
namespace json {
namespace errc {

enum errc_t {
   Success = 0,           // request succeeded

   //
   //	Invocation Errors -- All of these errors are guaranteed to have occurred
   //	prior to the execution of the method on the service
   //
   ConnectionError = 1,   // unable to connect to the service
   Unavailable = 2,       // service is currently unavailable
   Unauthorized = 3,      // client does not have required credentials
   InvalidClientId = 4,   // provided client id is invalid
   ParseError = 5,        // invalid json or an unexpected error during parsing
   InvalidRequest = 6,    // invalid json-rpc request
   MethodNotFound = 7,    // specified method not found on the server
   ParamMissing = 8,      // parameter missing
   ParamTypeMismatch = 9, // parameter type mismatch
   ParamInvalid = 10,     // parameter invalid
   MethodUnexpected = 11, // method unexpected for current application state
   InvalidClientVersion = 12, // client is running an invalid version
   ServerOffline = 13,    // server is offline
   InvalidSession = 14,   // target session is invalid
   MaxSessionsReached = 15,     // license error - max sessions reached
   MaxUsersReached = 16,  // license error - max users reached

   // launcher session parameters not found and should be resent to implicitly resume the session
   LaunchParametersMissing = 17,

   // profile errors - those imposed by limits in user profiles
   LimitSessionsReached = 20,

   // Execution errors -- These errors occurred during execution of the method.
   // Application state is therefore known based on the expected behavior
   // of the error which occurred. More details are provided within the
   // optional "error" field of the result
   ExecutionError = 100,

   // Transmission errors -- These errors leave the application in an unknown
   // state (it is not known whether the method finished all, some, or none
   // of its work).
   TransmissionError = 200
};

} // namespace errc
} // namespace json
} // namespace core
} // namespace rstudio

namespace RSTUDIO_BOOST_NAMESPACE {
namespace system {
template <>
struct is_error_code_enum<rstudio::core::json::errc::errc_t>
{
   static const bool value = true;
};

} // namespace system
} // namespace boost

#include <string>

#include <boost/bind.hpp>
#include <boost/function.hpp>
#include <boost/optional.hpp>
#include <boost/unordered_map.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
namespace http {
   class Response;
}
}
}

namespace rstudio {
namespace core {
namespace json {

// constants
extern const char * const kRpcResult;
extern const char * const kRpcError;
extern const char * const kJsonContentType;
extern const char * const kRpcAsyncHandle;

// jsonRpcCategory
const boost::system::error_category& jsonRpcCategory();


//
// json error codes
//
namespace errc {

inline boost::system::error_code make_error_code( errc_t e )
{
   return boost::system::error_code( e, jsonRpcCategory() ); }

inline boost::system::error_condition make_error_condition( errc_t e )
{
   return boost::system::error_condition( e, jsonRpcCategory() );
}

} // namespace errc
} // namespace json
} // namespace core
} // namespace rstudio

namespace rstudio {
namespace core {
namespace json {

struct JsonRpcRequest
{
   JsonRpcRequest() : version(0), isBackgroundConnection(false) {}
   
   std::string method;
   Array params;
   Object kwparams;
   std::string sourceWindow;
   std::string clientId;
   double version;
   std::string clientVersion;
   bool isBackgroundConnection;

   bool empty() const { return method.empty(); }
   
   void clear() 
   {
      method.clear();
      params.clear();
      kwparams.clear();
   }

   Object toJsonObject()
   {
      Object obj;
      obj["method"] = method;
      obj["params"] = params;
      obj["kwparams"] = kwparams;
      obj["sourceWindow"] = sourceWindow;
      obj["clientId"] = clientId;
      obj["version"] = version;
      obj["isBackgroundConnection"] = isBackgroundConnection;

      return obj;
   }
};

// 
// json request parsing
//
Error parseJsonRpcRequest(const std::string& input, JsonRpcRequest* pRequest);

bool parseJsonRpcRequestForMethod(const std::string& input, 
                                  const std::string& method,
                                  JsonRpcRequest* pRequest,
                                  http::Response* pResponse);
   
   
//
// json parameter reading helpers
//
   

inline core::Error readParam(const Array& params,
                             unsigned int index, 
                             Value* pValue)
{
   if (index >= params.getSize())
      return core::Error(json::errc::ParamMissing, ERROR_LOCATION);
   
   *pValue = params[index];
   return Success();
}

template <typename T>
core::Error readParam(const Array& params, unsigned int index, T* pValue)
{
   if (index >= params.getSize())
      return core::Error(json::errc::ParamMissing, ERROR_LOCATION);

   if (!isType<T>(params[index]))
      return core::Error(json::errc::ParamTypeMismatch, ERROR_LOCATION);

   *pValue = params[index].getValue<T>();

   return Success();
}

template <typename T, typename... Args>
core::Error readParams(const Array& params, T* pValue, Args... args)
{
   unsigned int index = 0;

   Error error = readParam(params, index++, pValue);
   if (error)
      return error;

   error = readParams(params, index, args...);
   if (error)
      return error;

   return Success();
}
   
template <typename T, typename... Args>
core::Error readParams(const Array& params, unsigned int index, T* pValue, Args... args)
{
   Error error = readParam(params, index++, pValue);
   if (error)
      return error;

   error = readParams(params, index, args...);
   if (error)
      return error;

   return Success();
}

template <typename T>
core::Error readParams(const Array& params, unsigned int index, T* pValue)
{
   return readParam(params, index, pValue);
}

template <typename T1>
core::Error readParams(const json::Array& params, T1* pValue1)
{
   return readParam(params, 0, pValue1);
}

namespace errors {

inline Error paramMissing(const std::string& name,
                          const ErrorLocation& location)
{
   Error error(json::errc::ParamTypeMismatch, location);
   error.addProperty("description", "no such parameter '" + name + "'");
   return error;
}

inline Error typeMismatch(const Value& value,
                          Type expectedType,
                          const ErrorLocation& location)
{
   Error error(json::errc::ParamTypeMismatch, location);
   
   std::string description = std::string("expected ") +
         "'" + typeAsString(expectedType) + "'" +
         "; got " +
         "'" + typeAsString(value) + "'";
   error.addProperty("description", description);
   return error;
}

} // namespace errors

template <typename T, typename... Args>
core::Error readObjectParam(const Array& params,
                            unsigned int index,
                            const std::string& name,
                            T* pValue,
                            Args... args)
{
   Error error = readObjectParam(params, index, name, pValue);
   if (error)
      return error;

   error = readObjectParam(params, index, args...);
   if (error)
      return error;

   return Success();
}

template <typename T>
core::Error readObjectParam(const Array& params,
                            unsigned int index, 
                            const std::string& name, 
                            T* pValue)
{
   Object object;
   Error error = readParam(params, index, &object);
   if (error)
      return error;
   
   return readObject(object, name, *pValue);
}

template <typename T>
core::Error getOptionalParam(const Object& json, const std::string& param,
                             const T& defaultValue, T* outParam)
{
   boost::optional<T> paramVal;
   Error error = readObject(json, param, paramVal);
   if (error)
      return error;

   *outParam = paramVal.get_value_or(defaultValue);

   return Success();
}

template <typename T>
core::Error getOptionalParam(const Object& json,
                             const std::string& param,
                             boost::optional<T>* pOutParam)
{
   return readObject(json, param, *pOutParam);
}

// json rpc response
         
class JsonRpcResponse
{
public:
   JsonRpcResponse() : suppressDetectChanges_(false)
   {
      setResult(Value());
   }

   // COPYING: via compiler (copyable members)
   
public:
   
   template <typename T>
   void setResult(const T& result)
   {
      setField(json::kRpcResult, result);
   }

   Value result()
   {
      return response_[json::kRpcResult];
   }

   Value error()
   {
      return response_[json::kRpcError];
   }
   
   void setError(const core::Error& error,
                 bool includeErrorProperties = false);
   
   void setError(const core::Error& error,
                 const char* message,
                 bool includeErrorProperties = false);
   
   void setError(const core::Error& error,
                 const std::string& message,
                 bool includeErrorProperties = false);

   void setError(const core::Error& error,
                 const Value& clientInfo,
                 bool includeErrorProperties = false);

   void setError(const boost::system::error_code& ec,
                 const Value& clientInfo = Value());

   void setRedirectError(const core::Error& error,
                         const std::string& redirectUrl);

   void setAsyncHandle(const std::string& handle);

   void setField(const std::string& name, const Value& value)
   { 
      response_[name] = value;
   }             
                
   template <typename T>
   void setField(const std::string& name, const T& value) 
   { 
      setField(name, Value(value));
   }

   template <typename T>
   Error getField(const std::string& name,
                 T* pValue)
   {
      return readObject(response_, name, *pValue);
   }
   
   // low level hook to set the full response
   void setResponse(const Object& response)
   {
      response_ = response;
   }
   
   // specify a function to run after the response
   void setAfterResponse(const boost::function<void()>& afterResponse);
   bool hasAfterResponse() const;
   void runAfterResponse();

   bool suppressDetectChanges() { return suppressDetectChanges_; }
   void setSuppressDetectChanges(bool suppress)
   {
      suppressDetectChanges_ = suppress;
   }

   Object getRawResponse();
   
   void write(std::ostream& os) const;

   static bool parse(const std::string& input,
                     JsonRpcResponse* pResponse);

   static bool parse(const Value& value,
                     JsonRpcResponse* pResponse);
   
private:
   Object response_;
   boost::function<void()> afterResponse_;
   bool suppressDetectChanges_;
};
   
   
// convenience functions for sending json-rpc responses
   
void setJsonRpcResponse(const JsonRpcResponse& jsonRpcResponse,
                        http::Response* pResponse);


inline void setVoidJsonRpcResult(http::Response* pResponse)
{
   JsonRpcResponse jsonRpcResponse;
   setJsonRpcResponse(jsonRpcResponse, pResponse);
}   


template <typename T>
void setJsonRpcResult(const T& result, http::Response* pResponse)
{
   JsonRpcResponse jsonRpcResponse;
   jsonRpcResponse.setResult(result);
   setJsonRpcResponse(jsonRpcResponse, pResponse);
}   

template <typename T>
void setJsonRpcError(const T& error, core::http::Response* pResponse, bool includeErrorProperties = false)
{   
   JsonRpcResponse jsonRpcResponse;
   jsonRpcResponse.setError(error, includeErrorProperties);
   setJsonRpcResponse(jsonRpcResponse, pResponse);
}

template <typename T>
void setJsonRpcRedirectError(const T& error,
                             const std::string& redirectUrl,
                             core::http::Response* pResponse)
{
   JsonRpcResponse jsonRpcResponse;
   jsonRpcResponse.setRedirectError(error, redirectUrl);
   setJsonRpcResponse(jsonRpcResponse, pResponse);
}

// helpers for populating an existing response object with errors
void setErrorResponse(const core::Error& error, core::json::JsonRpcResponse* pResponse);
void setProcessErrorResponse(const core::system::ProcessResult& result,
                             const core::ErrorLocation& location,
                             core::json::JsonRpcResponse* pResponse);

// helper for reading a json value (and setting an error response if it
// doesn't parse or is of the wrong type)
template <typename T>
bool parseJsonForResponse(const std::string& output, T* pVal, json::JsonRpcResponse* pResponse)
{
   using namespace json;
   T jsonValue;
   Error error = jsonValue.parse(output);
   if (error)
   {
      Error parseError(boost::system::errc::state_not_recoverable,
                       errorMessage(error),
                       ERROR_LOCATION);
      json::setErrorResponse(parseError, pResponse);
      return false;
   }
   else if (!isType<T>(jsonValue))
   {
      Error outputError(boost::system::errc::state_not_recoverable,
                       "Unexpected JSON output from pandoc",
                       ERROR_LOCATION);
      json::setErrorResponse(outputError, pResponse);
      return false;
   }
   else
   {
      *pVal = jsonValue;
      return true;
   }
}



// convenience typedefs for managing a map of json rpc functions
typedef boost::function<core::Error(const JsonRpcRequest&, JsonRpcResponse*)>
      JsonRpcFunction;
typedef std::pair<std::string,JsonRpcFunction>
      JsonRpcMethod;
typedef boost::unordered_map<std::string,JsonRpcFunction>
      JsonRpcMethods;

/*
   Async method support -- JsonRpcAsyncFunction is intended for potentially
   long running operations that need to keep the HTTP connection open until
   their work is done. (See registerRpcAsyncCoupleMethod for a different
   mechanism that provides similar functionality, but closes the HTTP
   connection and uses an event to simulate returning a result to the client.)
*/

// JsonRpcFunctionContinuation is what a JsonRpcAsyncFunction needs to call
// when its work is complete
typedef boost::function<void(const core::Error&, JsonRpcResponse*)>
      JsonRpcFunctionContinuation;
typedef boost::function<void(const JsonRpcRequest&, const JsonRpcFunctionContinuation&)>
      JsonRpcAsyncFunction;
// The bool in the next two typedefs specifies whether the function wants the
// HTTP connection to stay open until the method finishes executing (direct return),
// or for the HTTP connection to immediate return with an "asyncHandle" value that
// can be used to look in the event stream later when the method completes (indirect
// return). Direct return provides lower latency for short operations, and indirect
// return must be used for longer-running operations to prevent the browser from
// being starved of available HTTP connections to the server.
typedef std::pair<std::string,std::pair<bool, JsonRpcAsyncFunction> >
      JsonRpcAsyncMethod;
typedef boost::unordered_map<std::string,std::pair<bool, JsonRpcAsyncFunction> >
      JsonRpcAsyncMethods;

JsonRpcAsyncFunction adaptToAsync(JsonRpcFunction synchronousFunction);
JsonRpcAsyncMethod adaptMethodToAsync(JsonRpcMethod synchronousMethod);

} // namespace json
} // namespace core
} // namespace rstudio



#endif // CORE_JSON_RPC_HPP

