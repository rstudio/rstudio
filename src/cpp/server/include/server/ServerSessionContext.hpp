/*
 * ServerSessionContext.hpp
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

#ifndef SERVER_SESSION_CONTEXT_HPP
#define SERVER_SESSION_CONTEXT_HPP

#include <string>

#include <boost/shared_ptr.hpp>

#include <core/http/AsyncConnection.hpp>

#include <core/json/JsonRpc.hpp>

namespace rstudio {
namespace server {
  
struct SessionContext
{
   SessionContext()
   {
   }

   explicit SessionContext(const std::string& username,
                           const std::string& id = std::string())
      : username(username), id(id)
   {
   }
   std::string username;
   std::string id;

   bool empty() const { return username.empty(); }

   bool operator==(const SessionContext &other) const {
      return username == other.username && id == other.id;
   }

   bool operator<(const SessionContext &other) const {
       return username < other.username ||
              (username == other.username && id < other.id);
   }
};

bool sessionContextForRequest(
      boost::shared_ptr<core::http::AsyncConnection> ptrConnection,
      const std::string& username,
      SessionContext* pSessionContext);

void handleContextInitRequest(const core::json::JsonRpcRequest& request,
                              core::json::JsonRpcResponse* pResponse);

} // namespace server
} // namespace rstudio

#endif // SERVER_SESSION_CONTEXT_HPP

