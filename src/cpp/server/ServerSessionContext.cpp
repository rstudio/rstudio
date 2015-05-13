/*
 * ServerSessionContext.cpp
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

#include <server/ServerSessionContext.hpp>

#include <core/Error.hpp>

#include <core/http/URL.hpp>
#include <core/http/Util.hpp>
#include <core/http/Response.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {

bool sessionContextForRequest(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const std::string& username,
      SessionContext* pSessionContext)
{
   *pSessionContext = SessionContext(username);
   return true;
}

void handleContextInitRequest(const json::JsonRpcRequest& request,
                              json::JsonRpcResponse* pResponse)
{
   // read parameters
   std::string hostPageUrl;
   Error error = json::readParams(request.params, &hostPageUrl);
   if (error)
   {
      pResponse->setError(error);
      return;
   }

   // supress polling for events after this RPC request
   pResponse->setField("ep", "false");

   // no redirect for now
   pResponse->setResult(json::Value());
}

Error initializeSessionContext()
{
   return Success();
}

} // namespace server
} // namespace rstudio

