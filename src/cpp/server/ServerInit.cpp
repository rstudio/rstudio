/*
 * ServerInit.cpp
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



#include <core/Error.hpp>

#include <core/http/TcpIpAsyncServer.hpp>

#include <server/ServerOptions.hpp>

using namespace core;

namespace server {

http::AsyncServer* httpServerCreate()
{
   return new http::TcpIpAsyncServer("RStudio");
}

Error httpServerInit(http::AsyncServer* pAsyncServer)
{
   Options& options = server::options();
   return dynamic_cast<http::TcpIpAsyncServer*>(pAsyncServer)->init(
                                 options.wwwAddress(), options.wwwPort());
}

} // namespace server

