/*
 * ServerInit.cpp
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



#include <shared_core/Error.hpp>

#include <core/http/TcpIpAsyncServer.hpp>
#include <core/http/LocalStreamAsyncServer.hpp>

#include <server/ServerOptions.hpp>

#include "ServerMetrics.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace server {

http::AsyncServer* httpServerCreate(const http::Headers& additionalHeaders)
{
   if (options().wwwSocket() != "") {
      return new http::LocalStreamAsyncServer("RStudio",
                                 std::string(),
                                 core::FileMode::USER_READ_WRITE_EXECUTE,
                                 !options().wwwEnableOriginCheck(),
                                 options().wwwAllowedOrigins(),
                                 additionalHeaders,
                                 options().statsMonitorSeconds(),
                                 metrics::statsProvider());
   } else {
      return new http::TcpIpAsyncServer("RStudio",
                                 std::string(),
                                 !options().wwwEnableOriginCheck(),
                                 options().wwwAllowedOrigins(),
                                 additionalHeaders,
                                 options().statsMonitorSeconds(),
                                 metrics::statsProvider());
   }
}

Error httpServerInit(http::AsyncServer* pAsyncServer)
{
   Options& options = server::options();
   if (options.wwwSocket() != "") {
      FilePath streamPath(options.wwwSocket());
         return dynamic_cast<http::LocalStreamAsyncServer*>(pAsyncServer)->init(
                                 streamPath);
   } else {
         return dynamic_cast<http::TcpIpAsyncServer*>(pAsyncServer)->init(
                                 options.wwwAddress(), options.wwwPort());
   }
}

} // namespace server
} // namespace rstudio

