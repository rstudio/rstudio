/*
 * SocketRpc.cpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <shared_core/Error.hpp>
#include <shared_core/json/Json.hpp>
#include <core/json/JsonRpc.hpp>

#ifndef _WIN32
#include <core/http/LocalStreamBlockingClient.hpp>
#include <core/http/LocalStreamAsyncClient.hpp>
#endif

#include <core/http/HeaderCookieConstants.hpp>
#include <core/http/TcpIpBlockingClient.hpp>
#include <core/http/TcpIpBlockingClientSsl.hpp>
#include <core/http/TcpIpAsyncClient.hpp>
#include <core/http/TcpIpAsyncClientSsl.hpp>

#include <shared_core/SafeConvert.hpp>
#include <core/system/Crypto.hpp>
#include <core/system/Environment.hpp>

#include <core/SocketRpc.hpp>

using namespace rstudio::core;
using namespace boost::placeholders;

namespace rstudio {
namespace core {
namespace socket_rpc {

namespace {

std::string s_sessionSharedSecret;

void constructRequest(const std::string& endpoint,
                      const json::Object& payload,
                      http::Request* pRequest)
{
   // serialize the payload form the request
   pRequest->setMethod("POST");
   pRequest->setUri(endpoint);
   pRequest->setHeader("Connection", "close");
   pRequest->setBody(payload.write());
}

Error handleResponse(const std::string& endpoint,
                     const http::Response& response,
                     json::Value* pResult)
{
   if (response.statusCode() != http::status::Ok)
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
   else if (pResult->parse(response.body()))
   {
      LOG_WARNING_MESSAGE("Received unparseable result from rserver RPC:\n" +
            endpoint + "\n" +
            response.body());
      return Error(json::errc::ParseError, ERROR_LOCATION);
   }

   return Success();
}

#ifndef _WIN32
Error sendRequest(const FilePath& socketPath,
                  const std::string& endpoint,
                  const http::Request& request,
                  json::Value* pResult)
{
   http::Response response;

   Error error = http::sendRequest(socketPath, request, &response);
   if (error)
      return error;

   return handleResponse(endpoint, response, pResult);
}
#endif

Error sendRequest(const std::string& address,
                  const std::string& port,
                  bool useSsl,
                  bool verifySslCerts,
                  const boost::posix_time::time_duration& connectionTimeout,
                  const std::string& endpoint,
                  const http::Request& request,
                  json::Value* pResult)
{
   http::Response response;

   Error error;
   if (useSsl)
      error = http::sendSslRequest(address, port, verifySslCerts, request, &response);
   else
      error = http::sendRequest(address, port, request, &response);

   if (error)
   {
      error.addProperty("address", address);
      error.addProperty("port", port);
      return error;
   }

   return handleResponse(endpoint, response, pResult);
}

void onRpcResponse(const std::string& endpoint,
                   const http::Response& response,
                   const RpcResultHandler& onResult,
                   const RpcErrorHandler& onError)
{
   json::Value value;

   Error error = handleResponse(endpoint, response, &value);
   if (error)
   {
      onError(error);
      return;
   }

   onResult(value);
}

#ifndef _WIN32
void sendRequestAsync(boost::asio::io_service& ioService,
                      const FilePath& socketPath,
                      const std::string& endpoint,
                      const http::Request& request,
                      const RpcResultHandler& onResult,
                      const RpcErrorHandler& onError)
{
   boost::shared_ptr<http::LocalStreamAsyncClient> pClient(
            new http::LocalStreamAsyncClient(ioService, socketPath));

   pClient->request().assign(request);
   pClient->execute(boost::bind(onRpcResponse, endpoint, _1, onResult, onError), onError);
}
#endif

void sendRequestAsync(boost::asio::io_service& ioService,
                      const std::string& address,
                      const std::string& port,
                      bool useSsl,
                      bool verifySslCerts,
                      const boost::posix_time::time_duration& connectionTimeout,
                      const std::string& endpoint,
                      const http::Request& request,
                      const RpcResultHandler& onResult,
                      const RpcErrorHandler& onError)
{
   boost::shared_ptr<http::IAsyncClient> pClient;

   if (useSsl)
   {
      pClient.reset(new http::TcpIpAsyncClientSsl(ioService,
                                                  address,
                                                  port,
                                                  verifySslCerts, 
                                                  std::string(), // cert authority
                                                  connectionTimeout));
   }
   else
   {
      pClient.reset(new http::TcpIpAsyncClient(ioService,
                                               address,
                                               port,
                                               connectionTimeout));
   }

   pClient->request().assign(request);
   pClient->execute(boost::bind(onRpcResponse, endpoint, _1, onResult, onError), onError);
}

#ifndef _WIN32
void constructUnixRequest(const std::string& endpoint,
                          const json::Object& request,
                          http::Request* pRequest)
{
   constructRequest(endpoint, request, pRequest);

   // stamp rpc secret key on header
   // only used with unix sockets
   pRequest->setHeader(kServerRpcSecretHeader, s_sessionSharedSecret);
}
#endif

void constructTcpRequest(const std::string& address,
                         const std::string& endpoint,
                         const json::Object& request,
                         http::Request* pRequest)
{
   constructRequest(endpoint, request, pRequest);

   // stamp auth cookie on the request
   // this lets the server know the RPC is coming from a trusted sourcce,
   // and on behalf of which user
   pRequest->setHeader(kRStudioRpcCookieHeader, core::system::getenv(kRstudioRpcCookieEnvVar));

   // add additional Host header (needed for TCP connections)
   pRequest->setHost(address);
}

} // anonymous namespace

#ifndef _WIN32
Error invokeRpc(const FilePath& socketPath,
                const std::string& endpoint,
                const json::Object& request,
                json::Value *pResult)
{
   http::Request req;
   constructUnixRequest(endpoint, request, &req);
   return sendRequest(socketPath, endpoint, req, pResult);
}

void invokeRpcAsync(boost::asio::io_service& ioService,
                    const FilePath& socketPath,
                    const std::string& endpoint,
                    const json::Object& request,
                    const RpcResultHandler& onResult,
                    const RpcErrorHandler& onError)
{
   http::Request req;
   constructUnixRequest(endpoint, request, &req);
   sendRequestAsync(ioService, socketPath, endpoint, req, onResult, onError);
}
#endif

Error invokeRpc(const std::string& address,
                const std::string& port,
                bool useSsl,
                bool verifySslCerts,
                const boost::posix_time::time_duration& connectionTimeout,
                const std::string& endpoint,
                const json::Object& request,
                json::Value *pResult)
{
   http::Request req;
   constructTcpRequest(address, endpoint, request, &req);
   return sendRequest(address, port, useSsl, verifySslCerts, connectionTimeout, endpoint, req, pResult);
}

void invokeRpcAsync(boost::asio::io_service& ioService,
                    const std::string& address,
                    const std::string& port,
                    bool useSsl,
                    bool verifySslCerts,
                    const boost::posix_time::time_duration& connectionTimeout,
                    const std::string& endpoint,
                    const json::Object& request,
                    const RpcResultHandler& onResult,
                    const RpcErrorHandler& onError)
{
   http::Request req;
   constructTcpRequest(address, endpoint, request, &req);
   sendRequestAsync(ioService, address, port, useSsl, verifySslCerts, connectionTimeout, endpoint, req, onResult, onError);
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

const std::string& secret()
{
   return s_sessionSharedSecret;
}

} // namespace socket_rpc
} // namespace server_core
} // namespace rstudio

