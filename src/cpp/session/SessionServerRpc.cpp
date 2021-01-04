/*
 * SessionServerRpc.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#include <string>

#include <core/json/JsonRpc.hpp>
#include <core/http/URL.hpp>
#include <core/Thread.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionOptionsOverlay.hpp>
#include <session/SessionServerRpc.hpp>

#include "SessionRpc.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace server_rpc {

namespace {

// invoke an rserver HTTP RPC directly from R.
SEXP rs_invokeServerRpc(SEXP name, SEXP args)
{
   // generate RPC request from this R command
   json::JsonRpcRequest request;
   rpc::formatRpcRequest(name, args, &request);

   // call RPC
   json::Value rpcResult;
   Error error = invokeServerRpc(request.method, request.toJsonObject(), &rpcResult);
   if (error)
   {
      r::exec::error(log::errorAsLogEntry(error));
   }

   // convert result to rpc response
   core::json::JsonRpcResponse response;
   bool success = json::JsonRpcResponse::parse(rpcResult, &response);
   if (!success)
   {
      r::exec::error("Could not parse RPC response");
   }

   // emit formatted response if enabled
   if (!core::system::getenv("RSTUDIO_SESSION_RPC_DEBUG").empty())
   {
      std::cout << "<<<" << std::endl;
      response.getRawResponse().writeFormatted(std::cout);
      std::cout << std::endl;
   }

   // convert JSON response back to R
   SEXP result = R_NilValue;
   r::sexp::Protect protect;
   result = r::sexp::create(rpcResult, &protect);

   // log an R error if the RPC returns an error
   if (response.error().isObject())
   {
      r::exec::error(response.error().write());
   }

   return result;
}

// once flag for lazy initializing async RPC thread
boost::once_flag s_threadOnce = BOOST_ONCE_INIT;

// io_service for performing RPC work on the thread
boost::asio::io_service s_ioService;

void rpcWorkerThreadFunc()
{
   boost::asio::io_service::work work(s_ioService);
   s_ioService.run();
}

} // anonymous namespace

Error invokeServerRpc(const std::string& endpoint,
                      const json::Object& request,
                      json::Value* pResult)
{
   std::string serverAddress = options().getOverlayOption(kRServerAddress);

   if (serverAddress.empty())
   {
#ifdef _WIN32
       return systemError(boost::system::errc::not_supported, ERROR_LOCATION);
#else
      FilePath rpcSocket(core::system::getenv(kServerRpcSocketPathEnvVar));
      return socket_rpc::invokeRpc(rpcSocket, endpoint, request, pResult);
#endif
   }
   else
   {
      http::URL url(serverAddress);

      if (!url.isValid())
      {
         // not a valid url - we assume this is just a hostname or IP address
         std::string tcpPort = options().getOverlayOption(kRServerTcpPort);
         return socket_rpc::invokeRpc(serverAddress, tcpPort, false, false, endpoint, request, pResult);
      }
      else
      {
         // valid url - combine url path with requested endpoint
         bool verifySslCerts = options().getOverlayOption(kRServerVerifySslCerts) == "1";
         return socket_rpc::invokeRpc(url.hostname(), url.portStr(), url.protocol() == "https",
                                      verifySslCerts, url.path() + endpoint, request, pResult);
      }
   }
}



void invokeServerRpcAsync(const std::string& endpoint,
                          const json::Object& request,
                          const socket_rpc::RpcResultHandler& onResult,
                          const socket_rpc::RpcErrorHandler& onError)
{
   // start RPC worker thread if it hasn't already been started
   boost::call_once(s_threadOnce,
                    boost::bind(core::thread::safeLaunchThread,
                                rpcWorkerThreadFunc,
                                nullptr));

   std::string serverAddress = options().getOverlayOption(kRServerAddress);

   if (serverAddress.empty())
   {
#ifdef _WIN32
       onError(systemError(boost::system::errc::not_supported, ERROR_LOCATION));
       return;
#else
      FilePath rpcSocket(core::system::getenv(kServerRpcSocketPathEnvVar));
      return socket_rpc::invokeRpcAsync(s_ioService,
                                        rpcSocket,
                                        endpoint,
                                        request,
                                        onResult,
                                        onError);
#endif
   }
   else
   {
      http::URL url(serverAddress);

      if (!url.isValid())
      {
         // not a valid url - we assume this is just a hostname or IP address
         std::string tcpPort = options().getOverlayOption(kRServerTcpPort);
         return socket_rpc::invokeRpcAsync(s_ioService,
                                           serverAddress,
                                           tcpPort,
                                           false,
                                           false,
                                           endpoint,
                                           request,
                                           onResult,
                                           onError);
      }
      else
      {
         // valid url - combine url path with requested endpoint
         bool verifySslCerts = options().getOverlayOption(kRServerVerifySslCerts) == "1";
         return socket_rpc::invokeRpcAsync(s_ioService,
                                           url.hostname(),
                                           url.portStr(),
                                           url.protocol() == "https",
                                           verifySslCerts,
                                           url.path() + endpoint,
                                           request,
                                           onResult,
                                           onError);
      }
   }
}

Error initialize()
{
   RS_REGISTER_CALL_METHOD(rs_invokeServerRpc);

   return Success();
}

} // namespace server_rpc
} // namespace session
} // namespace rstudio

