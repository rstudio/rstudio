/*
 * ServerCSRFToken.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include <server/auth/ServerCSRFToken.hpp>
#include <server/ServerOptions.hpp>

#include <core/http/Cookie.hpp>
#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <core/system/PosixSystem.hpp>

#define kCSRFTokenName "csrf-token"

using namespace rstudio::core;

namespace rstudio {
namespace server {
namespace auth {
namespace csrf {

void setCSRFTokenCookie(const http::Request& request, 
                        http::Response* pResponse,
                        const std::string& token)
{
   // generate UUID for token if unspecified
   std::string csrfToken(token);
   if (csrfToken.empty())
      csrfToken = core::system::generateUuid();

   pResponse->addCookie(http::Cookie(
            request, 
            kCSRFTokenName, 
            csrfToken, 
            "/",  // cookie for root path
            false, // can't be HTTP only since it's read by client script
            // secure if delivered via SSL
            options().getOverlayOption("ssl-enabled") == "1"));
}

bool validateCSRFForm(const http::Request& request, 
                      http::Response* pResponse)
{
   // extract token from HTTP cookie (set above)
   std::string headerToken = request.cookieValue(kCSRFTokenName);
   http::Fields fields;

   // parse the form and check for a matching token
   http::util::parseForm(request.body(), &fields);
   std::string bodyToken = http::util::fieldValue<std::string>(fields,
         kCSRFTokenName, "");

   // report an error if they don't match
   if (headerToken.empty() || bodyToken != headerToken) 
   {
      pResponse->setStatusCode(http::status::BadRequest);
      pResponse->setBody("Missing or incorrect token.");
      return false;
   }

   // all is well
   return true;
}

} // namespace csrf
} // namespace auth
} // namespace server
} // namespace rstudio
