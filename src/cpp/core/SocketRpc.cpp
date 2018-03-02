/*
 * SocketRpc.cpp
 *
 * Copyright (C) 2017-18 by RStudio, Inc.
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
#include <core/http/TcpIpBlockingClient.hpp>
#include <core/SafeConvert.hpp>
#include <core/SocketRpc.hpp>
#include <core/system/Crypto.hpp>
#include <core/system/Environment.hpp>

namespace rstudio {
namespace core {
namespace socket_rpc {

namespace {

std::string s_sessionSharedSecret;

void constructRequest(const std::string& endpoint,
                      const json::Object& payload,
                      http::Request* pRequest)
{
   // serialize the payload
   std::ostringstream oss;
   core::json::write(payload, oss);

   // form the request
   pRequest->setMethod("POST");
   pRequest->setUri(endpoint);
   pRequest->setHeader("Connection", "close");
   pRequest->setBody(oss.str());
}

Error signRequest(http::Request& request)
{
   std::string date = http::util::httpDate();

   // compute hmac for the request
   std::string payload =  date +
                          "\n" +
                          request.body();
   std::vector<unsigned char> hmac;
   Error error = core::system::crypto::HMAC_SHA2(payload, s_sessionSharedSecret, &hmac);
   if (error)
      return error;

   // base 64 encode it
   std::string signature;
   error = core::system::crypto::base64Encode(hmac, &signature);
   if (error)
      return error;

   // stamp the signature on the request
   request.setHeader(kRstudioMessageSignature, signature);

   return Success();
}

Error handleResponse(const std::string& endpoint,
                     const http::Response& response,
                     json::Value* pResult)
{
   if (response.statusCode() != core::http::status::Ok)
   {
      LOG_WARNING_MESSAGE("Server RPC failed: " + endpoint + " " +
                          safe_convert::numberToString(response.statusCode()) +
                          " " + response.statusMessage() + "\n" +
                          response.body());
      return Error(json::errc::ExecutionError, ERROR_LOCATION);
   }
   else if (response.body().empty())
   {
      // empty value from server doesn't imply failure, just that there's
      // nothing for us to read
      *pResult = json::Value();
      return Success();
   }
   else if (!json::parse(response.body(), pResult))
   {
      LOG_WARNING_MESSAGE("Received unparseable result from rserver RPC:\n" +
            endpoint + "\n" +
            response.body());
      return Error(json::errc::ParseError, ERROR_LOCATION);
   }

   return Success();
}

Error sendRequest(const FilePath& socketPath,
                  const std::string& endpoint,
                  const http::Request& request,
                  json::Value* pResult)
{
   core::http::Response response;
   core::http::sendRequest(socketPath, request, &response);

   return handleResponse(endpoint, response, pResult);
}

Error sendRequest(const std::string& tcpAddress,
                  const std::string& port,
                  const std::string& endpoint,
                  const http::Request& request,
                  json::Value* pResult)
{
   core::http::Response response;
   core::http::sendRequest(tcpAddress, port, request, &response);

   return handleResponse(endpoint, response, pResult);
}

} // anonymous namespace


Error invokeRpc(const FilePath& socketPath,
                const std::string& endpoint,
                const json::Object& request,
                json::Value *pResult)
{
   http::Request req;
   constructRequest(endpoint, request, &req);

   // stamp rpc secret key on header
   // only used with unix sockets
   req.setHeader(kServerRpcSecretHeader, s_sessionSharedSecret);

   return sendRequest(socketPath, endpoint, req, pResult);
}

Error invokeRpc(const std::string& tcpAddress,
                const std::string& port,
                const std::string& endpoint,
                const json::Object& request,
                json::Value *pResult)
{
   http::Request req;
   constructRequest(endpoint, request, &req);

   // sign request with secret key
   // we do this instead of sending the secret key in the header
   // since we are connecting over an untrusted TCP connection
   Error error = signRequest(req);
   if (error)
      return error;

   // add additional Host header (needed for tcp connections)
   req.setHost(tcpAddress);

   return sendRequest(tcpAddress, port, endpoint, req, pResult);
}

Error initialize()
{
   // extract shared secret
   s_sessionSharedSecret = core::system::getenv(kServerRpcSecretEnvVar);
   core::system::unsetenv(kServerRpcSecretEnvVar);

   return Success();
}

Error initializeSecret(const std::string& rpcSecret)
{
   s_sessionSharedSecret = rpcSecret;

   return Success();
}

} // namespace socket_rpc
} // namespace core
} // namespace rstudio

