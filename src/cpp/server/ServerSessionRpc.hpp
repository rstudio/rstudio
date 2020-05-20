/*
 * ServerSessionRpc.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#ifndef SERVER_SESSION_RPC_HPP
#define SERVER_SESSION_RPC_HPP

#include <core/http/AsyncUriHandler.hpp>
#include <core/PeriodicCommand.hpp>

#include <server/auth/ServerSecureUriHandler.hpp>

#define kSessionRpcCmdPeriodMs  50
#define kSessionServerRpcSocket "session-server-rpc.socket"

namespace rstudio {
namespace core {
   class Error;
}
}

namespace rstudio {
namespace server {
namespace session_rpc {
   
core::Error initialize();
core::Error startup();

// use to add session-rpc handler requiring RPC secret to be present; this is what
// you should use unless there's a well-understood scenario where relaxed
// security is acceptable (see addBrowserHttpProxyhandler)
void addHandler(const std::string& prefix,
                const auth::SecureAsyncUriHandlerFunction& handler);

// use to add session-rpc handler that falls back to checking for regular
// login cookie when RPC secret not found
void addHttpProxyHandler(const std::string &prefix,
                         const auth::SecureAsyncUriHandlerFunction &handler);

void addPeriodicCommand(boost::shared_ptr<core::PeriodicCommand> pCmd);

} // namespace acls
} // namespace server
} // namespace rstudio

#endif // SERVER_SESSION_RPC_HPP

