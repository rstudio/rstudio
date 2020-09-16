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
#include <core/system/Process.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionAsyncDownloadFile.hpp>

#include "SessionPanmirrorCrossref.hpp"
#include "SessionPanmirrorUtils.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {
namespace doi {

namespace {

using namespace panmirror::crossref;

const char * const kDOIHost = "https://doi.org";
const char * const kDataCiteHost = "https://data.datacite.org";
const char * const kCSLJsonFormat = "application/vnd.citationstyles.csl+json";


void crossrefDownloadHandler(const std::string& doi,
                             const json::JsonRpcFunctionContinuation& cont,
                             const core::system::ProcessResult& result)
{
   json::Value cslJson;
   Error error = handleJsonRpcProcessResult(result, &cslJson, ERROR_LOCATION);
   if (!error)
   {
      resolveJsonRpcContinuation(cont, kStatusOK, cslJson);
   }
   else
   {
      // do a datacite lookup (see: https://citation.crosscite.org/docs.html#sec-5)
      boost::format fmt("%s/%s/%s");
      std::string url = boost::str(fmt % kDataCiteHost % kCSLJsonFormat % doi);
      asyncDownloadFile(url, boost::bind(jsonRpcDownloadHandler, cont, _1, ERROR_LOCATION, jsonPassthrough));
   }
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

    if (module_context::hasMinimumRVersion("3.6"))
    {
       std::string url;
       http::Fields headers;
       boost::format fmt("%s/%s");
       url = boost::str(fmt % kDOIHost % doi);
       headers.push_back(std::make_pair("Accept", kCSLJsonFormat));
       asyncDownloadFile(url, headers, boost::bind(jsonRpcDownloadHandler, cont, _1, ERROR_LOCATION, jsonPassthrough));
    }
    else
    {
       // Path to DOI metadata works/{doi}/transform/{format} (see: https://citation.crosscite.org/docs.html#sec-5)
       boost::format fmt("%s/%s/transform/%s");
       const std::string resourcePath = boost::str(fmt % kCrossrefWorks % doi % kCSLJsonFormat);
       std::string url = std::string(kCrossrefApiHost) + "/" + resourcePath;
       asyncDownloadFile(url, boost::bind(crossrefDownloadHandler, doi, cont, _1));
    }
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
