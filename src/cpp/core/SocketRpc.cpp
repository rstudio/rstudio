/*
 * SocketRpc.cpp
 *
 * Copyright (C) 2017 by RStudio, Inc.
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

#include <core/Error.hpp>
#include <core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/http/LocalStreamBlockingClient.hpp>
#include <core/SafeConvert.hpp>
#include <core/SocketRpc.hpp>
#include <core/system/Environment.hpp>

namespace rstudio {
namespace core {
namespace socket_rpc {

namespace {

std::string s_sessionSharedSecret;

} // anonymous namespace


Error invokeRpc(const FilePath& socketPath,
                const std::string& endpoint,
                const json::Object& request,
                json::Value *pResult)
{
   // serialize the payload
   std::ostringstream oss;
   core::json::write(request, oss);

   // form the request 
   core::http::Request req;
   req.setMethod("POST");
   req.setUri(endpoint);
   req.setHeader("Connection", "close");
   req.setHeader(kServerRpcSecretHeader, s_sessionSharedSecret);
   req.setBody(oss.str());

   core::http::Response resp;
   core::http::sendRequest(socketPath, req, &resp);

   if (resp.statusCode() != core::http::status::Ok)
   {
      LOG_WARNING_MESSAGE("Server RPC failed: " + endpoint +
                          safe_convert::numberToString(resp.statusCode()) +
                          "\n" + resp.statusMessage());
      return Error(json::errc::ExecutionError, ERROR_LOCATION);
   }
   else if (resp.body().empty())
   {
      // empty value from server doesn't imply failure, just that there's
      // nothing for us to read
      *pResult = json::Value();
      return Success();
   }
   else if (!json::parse(resp.body(), pResult))
   {
      LOG_WARNING_MESSAGE("Received unparseable result from rserver RPC:\n" +
            endpoint + "\n" +
            resp.body());
      return Error(json::errc::ParseError, ERROR_LOCATION);
   }

   return Success();
}

Error initialize()
{
   // extract shared secret 
   s_sessionSharedSecret = core::system::getenv(kServerRpcSecretEnvVar);
   core::system::unsetenv(kServerRpcSecretEnvVar);

   return Success();
}

} // namespace socket_rpc
} // namespace core
} // namespace rstudio

