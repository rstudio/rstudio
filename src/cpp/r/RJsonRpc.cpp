/*
 * RJsonRpc.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

/* Support for implementing json-rpc methods directly in R:
   
- Inbound json parameters are converted to R objects as follows:

   1) Both params and kwparams are supported (params are pushed onto the
      call stack first followed by kwparams)

   2) Json null values are converted to R_NilValue

   3) Json values of primitive types are converted to scalar vectors:

         string      - Scalar String (STRSXP)
         integer     - Scalar Integer (INTSXP)
         number      - Scalar Real (REALSXP)
         true/false  - Scalar Logical (LGLSXP)

   4) Json arrays are converted to lists (VECSXP)

   5) Json objects are converted to lists (VECSXP) with field names
      assigned to list elements

- Outbound R objects are converted to json via rules described in RJson.cpp

- Json-rpc functions can signal errors by calling the stop function. note
  that these and any other errors which occur during json rpc calls are
  both reported as errors to the rpc caller as well as printed to the 
  console (this is because we use r::engine::evaluateExpressions for calling
  the method and have yet to figure out how to suppress it from printing
  messages to the console). 
*/

#define R_INTERNAL_FUNCTIONS
#include <r/RJsonRpc.hpp>

#include <boost/bind/bind.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include <r/RExec.hpp>
#include <r/RSourceManager.hpp>
#include <r/RErrorCategory.hpp>
#include <r/RJson.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace r {
namespace json {

namespace {
         
Error setJsonResult(SEXP resultSEXP, core::json::JsonRpcResponse* pResponse)
{   
   // get the result
   core::json::Value resultValue;
   Error error = jsonValueFromObject(resultSEXP, &resultValue);
   if (error)
      return error;
   
   // set the result and return success
   pResponse->setResult(resultValue);
   return Success();
}

bool containsNul(const core::json::Value& value)
{
   if (value.isString())
      return value.getString().find('\0') != std::string::npos;

   if (value.isArray())
   {
      for (const core::json::Value& element : value.getArray())
      {
         if (containsNul(element))
            return true;
      }
   }
   else if (value.isObject())
   {
      for (const core::json::Object::Member& member : value.getObject())
      {
         if (member.getName().find('\0') != std::string::npos || containsNul(member.getValue()))
            return true;
      }
   }

   return false;
}

Error callRHandler(const std::string& functionName,
                   const core::json::JsonRpcRequest& request,
                   SEXP* pResult,
                   sexp::Protect* pProtect)
{
   // R raises an error on a string with an embedded nul. Raised while the
   // arguments below are built, before the call runs protected, it would
   // longjmp past this handler and leave the request unanswered. names are
   // refused too, as R would truncate them at the nul
   if (containsNul(request.params) || containsNul(request.kwparams))
   {
      return Error(core::json::errc::ParamInvalid,
                   "string parameter contains an embedded nul",
                   ERROR_LOCATION);
   }

   // initialize the function
   r::exec::RFunction rFunction(functionName);
   
   // add params
   const core::json::Array& params = request.params;
   for (size_t i = 0; i < params.getSize(); i++)
      rFunction.addParam(params[i]);

   // add kwparams
   const core::json::Object& kwparams = request.kwparams;
   for (const core::json::Object::Member& member : kwparams)
      rFunction.addParam(member.getName(), member.getValue());
   
   // call the function
   return rFunction.call(pResult, pProtect);
}

Error handleRequest(const std::string& rFunctionName,
                    const core::json::JsonRpcRequest& request, 
                    core::json::JsonRpcResponse* pResponse)
{
   // call the function
   sexp::Protect rProtect;
   SEXP resultSEXP;
   Error error = callRHandler(rFunctionName, request, &resultSEXP, &rProtect);
   if (error)
      return error;
   
   // convert from R SEXP to json result
   return setJsonResult(resultSEXP, pResponse);
}

} // anonymous namespace 

   
Error getRpcMethods(core::json::JsonRpcMethods* pMethods)
{
   // find all of the rpc handlers
   std::vector<std::string> rpcHandlers;
   Error error = r::exec::RFunction(".rs.listJsonRpcHandlers").call(
                                                            &rpcHandlers);
   if (error)
      return error;
   
   // populate function map
   pMethods->clear();
   std::string rpcPrefix(".rs.rpc.");
   for (auto it = rpcHandlers.begin();
        it != rpcHandlers.end();
        ++it)
   {
      std::string methodName = it->substr(rpcPrefix.size());
      pMethods->insert(std::make_pair(methodName, 
                                      boost::bind(handleRequest, 
                                                  *it,
                                                  _1,
                                                  _2)));
   }
   
   return Success();
}
      
} // namespace json
} // namespace r
} // namespace rstudio

