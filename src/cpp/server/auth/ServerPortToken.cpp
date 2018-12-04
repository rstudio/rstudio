/*
 * ServerPortToken.cpp
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

#include <server/auth/ServerPortToken.hpp>
#include <server/ServerOptions.hpp>

#include <core/http/Cookie.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>
#include <core/system/PosixSystem.hpp>

#include <server_core/UrlPorts.hpp>

#include <random>

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace auth {
namespace port {

void setPortTokenCookie(const http::Request& request, 
                        boost::optional<boost::gregorian::days> expiry,
                        http::Response* pResponse)
{
   http::Cookie cookie(
            request, 
            kPortTokenCookie, 
            server_core::generateNewPortToken(), 
            "/",  // cookie for root path
            true, // HTTP only -- client doesn't get to read this token
            options().getOverlayOption("ssl-enabled") == "1");

   // set expiration for cookie
   if (expiry.is_initialized())
      cookie.setExpires(*expiry);

   pResponse->addCookie(cookie);
}


} // namespace port
} // namespace auth
} // namespace server
} // namespace rstudio
