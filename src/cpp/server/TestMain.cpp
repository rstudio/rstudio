/*
 * TestMain.cpp
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

#include <core/http/AsyncServer.hpp>

#include <server/auth/ServerAuthHandler.hpp>
#include <server/auth/ServerSecureUriHandler.hpp>

#include <tests/TestMain.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace uri_handlers {

/* The following method stubs provide definitions for global
 * functions that may be called from server code that is normally
 * defined in ServerMain.cpp, which is replaced with TestMain.cpp
 * when built as the test binary. These functions must not be called
 * by test code, as they will become do-nothing impls. Code under test
 * should not assume the ability to directly interact with the HTTP server.
 */

void add(const std::string& prefix,
         const http::AsyncUriHandlerFunction& handler)
{
}

void addUploadHandler(const std::string& prefix,
         const http::AsyncUriUploadHandlerFunction& handler)
{
}

void addProxyHandler(const std::string& prefix,
                     const http::AsyncUriHandlerFunction& handler)
{
}

void addBlocking(const std::string& prefix,
                 const http::UriHandlerFunction& handler)
{
}

void setDefault(const http::AsyncUriHandlerFunction& handler)
{
}

void setBlockingDefault(const http::UriHandlerFunction& handler)
{
}

void setRequestFilter(const core::http::RequestFilter& filter)
{
}

void setResponseFilter(const core::http::ResponseFilter& filter)
{
}

} // namespace uri_handlers

namespace scheduler {

void addCommand(boost::shared_ptr<ScheduledCommand> pCmd)
{
}

} // namespace scheduler

boost::shared_ptr<http::AsyncServer> server()
{
   return boost::shared_ptr<http::AsyncServer>();
}

} // namespace server
} // namespace rstudio
