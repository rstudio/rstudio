/*
 * SessionServerRpcOverlay.cpp
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

#include <shared_core/Error.hpp>

#include <core/http/AsyncServer.hpp>

#include <server/auth/ServerAuthHandler.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace session_rpc {
namespace overlay {

Error initialize(
   const boost::shared_ptr<http::AsyncServer>& pSessionRpcServer,
   const std::string& sessionSharedSecret)
{
   return Success();
}

void addHandler(
   const std::string& prefix,
   const auth::SecureAsyncUriHandlerFunction& handler,
   bool allowUserAccess)
{
   // Do nothing.
}

void addHttpProxyHandler(
   const std::string& prefix,
   const auth::SecureAsyncUriHandlerFunction& handler)
{
   // Do nothing.
}

} // namespace overlay
} // namespace server_rpc
} // namespace session
} // namespace rstudio