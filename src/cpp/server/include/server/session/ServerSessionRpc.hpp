/*
 * ServerSessionRpc.hpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SERVER_SESSION_RPC_HPP
#define SERVER_SESSION_RPC_HPP

#include <core/PeriodicCommand.hpp>
#include <core/http/AsyncUriHandler.hpp>

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
// allowUserAccess indicates whether or not regular users (without access to RPC secret)
// are allowed to invoke the RPC - should be set to false for security-sensitive RPCs
void addHandler(const std::string& prefix,
                const auth::SecureAsyncUriHandlerFunction& handler,
                bool allowUserAccess = true);

// use to add session-rpc handler that falls back to checking for regular
// login cookie when RPC secret not found
void addHttpProxyHandler(const std::string &prefix,
                         const auth::SecureAsyncUriHandlerFunction &handler);

void addPeriodicCommand(boost::shared_ptr<core::PeriodicCommand> pCmd);

} // namespace acls
} // namespace server
} // namespace rstudio

#endif // SERVER_SESSION_RPC_HPP

