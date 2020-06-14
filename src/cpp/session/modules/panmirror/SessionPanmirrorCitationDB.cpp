/*
 * SessionPanmirrorCitationDB.cpp
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

#include "SessionPanmirrorCitationDB.hpp"

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>

#include <core/Exec.hpp>


#include <session/SessionModuleContext.hpp>

#include "SessionPanmirror.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace panmirror {
namespace citation_db {

namespace {

void crossrefWorks(const json::JsonRpcRequest& request,
                   const json::JsonRpcFunctionContinuation& cont)
{
   // response object
   json::JsonRpcResponse response;

   // extract query
   std::string query;
   Error error = json::readParams(request.params, &query);
   if (error)
   {
     setErrorResponse(error, &response);
     cont(Success(), &response);
     return;
   }

   json::Array worksJson;

   json::Object workJson;
   workJson["publisher"] = "RStudio";
   workJson["url"] = "https://www.rstudio.com";

   worksJson.push_back(workJson);

   response.setResult(worksJson);

   cont(Success(), &response);


}


} // end anonymous namespace

Error initialize()
{
   ExecBlock initBlock;
   initBlock.addFunctions()
      (boost::bind(module_context::registerAsyncRpcMethod, "crossref_works", crossrefWorks))
   ;
   return initBlock.execute();
}

} // end namespace citation_db
} // end namespace panmirror
} // end namespace modules
} // end namespace session
} // end namespace rstudio
