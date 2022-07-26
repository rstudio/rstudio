/*
 * SocketRpc.hpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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

#ifndef CORE_SOCKET_RPC_HPP
#define CORE_SOCKET_RPC_HPP

#include <boost/asio/io_service.hpp>
#include <boost/function.hpp>
#include <shared_core/json/Json.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#define kServerRpcSecretEnvVar        "RS_SESSION_SERVER_RPC_SECRET"
#define kRstudioRpcCookieEnvVar       "RS_SESSION_RPC_COOKIE"
#define kServerRpcSocketPathEnvVar    "RS_SERVER_RPC_SOCKET_PATH"

namespace rstudio {
namespace core {
class Error;
class FilePath;
}

namespace core {

namespace socket_rpc {

typedef boost::function<void(const core::json::Value&)> RpcResultHandler;
typedef boost::function<void(const core::Error&)> RpcErrorHandler;

core::Error initialize();
core::Error initializeSecret(const std::string& rpcSecret);

#ifndef _WIN32
core::Error invokeRpc(const core::FilePath& socketPath,
                      const std::string& endpoint,
                      const core::json::Object& request,
                      core::json::Value* pResult);

void invokeRpcAsync(boost::asio::io_service& ioService,
                    const core::FilePath& socketPath,
                    const std::string& endpoint,
                    const core::json::Object& request,
                    const RpcResultHandler& onResult,
                    const RpcErrorHandler& onError);
#endif

core::Error invokeRpc(const std::string& address,
                      const std::string& port,
                      bool useSsl,
                      bool verifySslCerts,
                      const boost::posix_time::time_duration& connectionTimeout,
                      const std::string& endpoint,
                      const core::json::Object& request,
                      core::json::Value* pResult);

void invokeRpcAsync(boost::asio::io_service& ioService,
                    const std::string& address,
                    const std::string& port,
                    bool useSsl,
                    bool verifySslCerts,
                    const boost::posix_time::time_duration& connectionTimeout,
                    const std::string& endpoint,
                    const core::json::Object& request,
                    const RpcResultHandler& onResult,
                    const RpcErrorHandler& onError);

const std::string& secret();

} // namespace socket_rpc
} // namespace server_core
} // namespace rstudio

#endif // CORE_SOCKET_RPC_HPP


