/*
 * SessionPanmirrorDOI.cpp
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

#include "SessionPanmirrorDOI.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/Exec.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/http/Util.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncDownloadFile.hpp>

#include "SessionPanmirrorCrossref.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {
namespace doi {

namespace {

using namespace panmirror::crossref;

const char * const kDOIHost = "https://doi.org";
const char * const kCSLJsonFormat = "application/vnd.citationstyles.csl+json";

void fetchDOIRequestHandler(const core::json::Value& value, core::json::JsonRpcResponse* pResponse)
{
   pResponse->setResult(value);
}

void doiFetchCSL(const json::JsonRpcRequest& request,
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

    std::string url;
    http::Fields headers;

    if (module_context::hasMinimumRVersion("3.6"))
    {
       boost::format fmt("%s/%s");
       url = boost::str(fmt % kDOIHost % doi);
       headers.push_back(std::make_pair("Accept", kCSLJsonFormat));
    }
    else
    {
       // Path to DOI metadata works/{doi}/transform/{format} (see: https://citation.crosscite.org/docs.html#sec-5)
       boost::format fmt("%s/%s/transform/%s");
       const std::string resourcePath = boost::str(fmt % kCrossrefWorks % doi % kCSLJsonFormat);
       url = std::string(kCrossrefApiHost) + "/" + resourcePath;
    }

    // make request
    asyncJsonRpcRequest(url, headers, fetchDOIRequestHandler, cont);
}


} // end anonymous namespace

Error initialize()
{
   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(module_context::registerAsyncRpcMethod, "doi_fetch_csl", doiFetchCSL))
   ;
   return initBlock.execute();
}

} // end namespace crossref
} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
