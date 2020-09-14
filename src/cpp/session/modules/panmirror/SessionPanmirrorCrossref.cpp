/*
 * SessionPanmirrorCrossref.cpp
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

#include "SessionPanmirrorCrossref.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/http/Util.hpp>

#include <r/ROptions.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncDownloadFile.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {
namespace crossref {

namespace {

void crossrefContentRequestHandler(const core::json::Value& value, core::json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(value);
}

void crossrefApiRequestHandler(const core::json::Value& value, core::json::JsonRpcResponse* pResponse)
{   
   if (json::isType<json::Object>(value))
   {
      json::Object responseJson = value.getObject();
      std::string status;
      json::Object message;
      Error error = json::readObject(responseJson, "status", status,
                                                   "message", message);
      if (error)
      {
         json::setErrorResponse(error, pResponse);
      }
      else if (status != "ok")
      {
         Error error = systemError(boost::system::errc::state_not_recoverable,
                                   "Unexpected status from crossref api: " + status,
                                   ERROR_LOCATION);
         json::setErrorResponse(error, pResponse);
      }
      else
      {
         pResponse->setResult(message);
      }
   }
   else
   {
      Error error = systemError(boost::system::errc::state_not_recoverable,
                                "Unexpected response from crossref api",
                                ERROR_LOCATION);
      json::setErrorResponse(error, pResponse);
   }
}

void crossrefRequest(const std::string& resource,
                     const http::Fields& params,
                     const session::JsonRpcResponseHandler& handler,
                     const json::JsonRpcFunctionContinuation& cont)
{
   // email address
   std::string email = r::options::getOption<std::string>("rstudio.crossref_email",
                                                          "crossref@rstudio.com", false);

   // build user agent
   std::string userAgent = r::options::getOption<std::string>("HTTPUserAgent", "RStudio") +
                           "; RStudio Crossref Cite (mailto:" + email + ")";

   // build query string
   std::string queryString;
   core::http::util::buildQueryString(params, &queryString);
   if (queryString.length() > 0)
      queryString = "?" + queryString;

   // build the url and make the request
   boost::format fmt("%s/%s%s");
   const std::string url = boost::str(fmt % kCrossrefApiHost % resource % queryString);

   http::Fields headers;
   asyncJsonRpcRequest(url, userAgent, headers, handler, cont);
}


void crossrefWorks(const json::JsonRpcRequest& request,
                   const json::JsonRpcFunctionContinuation& cont)
{
   // extract query
   std::string query;
   Error error = json::readParams(request.params, &query);
   if (error)
   {
     json::JsonRpcResponse response;
     setErrorResponse(error, &response);
     cont(Success(), &response);
     return;
   }

   // build params
   core::http::Fields params;
   params.push_back(std::make_pair("query", query));

   // make the request
   crossrefRequest(kCrossrefWorks, params, crossrefApiRequestHandler, cont);
}

void crossrefDoi(const json::JsonRpcRequest& request,
                 const json::JsonRpcFunctionContinuation& cont)
{
    std::string doi;
    Error error = json::readParams(request.params, &doi);
    if (error)
    {
      json::JsonRpcResponse response;
      setErrorResponse(error, &response);
      cont(Success(), &response);
      return;
    }
    
    // Path to DOI metadata works/{doi}/transform/{format} (see: https://citation.crosscite.org/docs.html#sec-5)
    const char * const kCitationFormat = "application/vnd.citationstyles.csl+json";
    boost::format fmt("%s/%s/transform/%s");
    const std::string resourcePath = boost::str(fmt % kCrossrefWorks % doi % kCitationFormat);
    
    // No parameters
    core::http::Fields params;

    // make the request
    crossrefRequest(resourcePath, params, crossrefContentRequestHandler, cont);
}


} // end anonymous namespace

Error initialize()
{
   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(module_context::registerAsyncRpcMethod, "crossref_works", crossrefWorks))
      (boost::bind(module_context::registerAsyncRpcMethod, "crossref_doi", crossrefDoi))
   ;
   return initBlock.execute();
}

} // end namespace crossref
} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
