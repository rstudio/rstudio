/*
 * SessionPanmirrorUtils.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include "SessionPanmirrorUtils.hpp"

#include <core/json/JsonRpc.hpp>

#include <core/system/Process.hpp>

#include <session/SessionAsyncDownloadFile.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {

const char * const kStatusOK = "ok";
const char * const kStatusNotFound = "notfound";
const char * const kStatusNoHost = "nohost";
const char * const kStatusError = "error";

void resolveJsonRpcContinuation(const json::JsonRpcFunctionContinuation& cont,
                                const std::string& status,
                                const json::Value& messageJson,
                                const std::string& error)
{
   json::Object resultJson;
   resultJson["status"] = status;
   resultJson["message"] = messageJson;
   resultJson["error"] = error;
   json::JsonRpcResponse response;
   response.setResult(resultJson);
   cont(Success(), &response);
}

Error handleJsonRpcProcessResult(const core::system::ProcessResult& result,
                                 json::Value* pValue,
                                 const core::ErrorLocation& location)
{
   if (result.exitStatus == EXIT_SUCCESS)
   {
      return pValue->parse(result.stdOut);
   }
   else
   {
      // log if it's not a 404 or host not found error
      if (!is404Error(result.stdErr) && !isHostError(result.stdErr))
         core::log::logErrorMessage("Error fetching CSL for DOI: " + result.stdErr, location);

      // return error
      return systemError(boost::system::errc::state_not_recoverable,
                         result.stdErr,
                         location);
   }
}

void jsonRpcDownloadHandler(const json::JsonRpcFunctionContinuation& cont,
                            const core::system::ProcessResult& result,
                            const core::ErrorLocation& location,
                            const JsonHandler& jsonHandler)
{
   json::Value jsonValue;
   Error error = handleJsonRpcProcessResult(result, &jsonValue, location);
   if (!error)
   {
      if (jsonHandler.empty())
         resolveJsonRpcContinuation(cont, kStatusOK, jsonValue);
      else
         jsonHandler(cont, jsonValue);
   }
   // not found (404)
   else if (is404Error(result.stdErr))
   {
      resolveJsonRpcContinuation(cont, kStatusNotFound);
   }
   // no host (offline?)
   else if (isHostError(result.stdErr))
   {
      resolveJsonRpcContinuation(cont, kStatusNoHost);
   }
   // return error
   else
   {
      resolveJsonRpcContinuation(cont, kStatusError, json::Value(), core::errorDescription(error));
   }
}



} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
