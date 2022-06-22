/*
 * CSRFToken.cpp
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

#include <core/http/CSRFToken.hpp>

#include <boost/algorithm/string/predicate.hpp>

#include <core/http/Request.hpp>
#include <core/http/Response.hpp>

#include <core/system/System.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace core {
namespace http {

std::string setCSRFTokenCookie(const http::Request& request,
                               const boost::optional<boost::posix_time::time_duration>& expiresFromNow,
                               const std::string& token,
                               const std::string& path,
                               bool secure,
                               http::Cookie::SameSite sameSite,
                               http::Response* pResponse)
{
   // generate UUID for token if unspecified
   std::string csrfToken(token);
   if (csrfToken.empty())
      csrfToken = core::system::generateUuid();

   // generate CSRF token cookie
   http::Cookie cookie(
            request,
            kCSRFTokenCookie,
            csrfToken,
            path,
            sameSite,
            true, // HTTP only
            secure);

   // NOTE: Remove block when Ghost Orchid 2021.09 is not supported ======================================
   // Set the old cookie as well, for backward compatibility
   http::Cookie oldCookie(
            request,
            kOldCSRFTokenCookie,
            csrfToken,
            path,
            sameSite,
            true, // HTTP only
            secure);
   // ====================================================================================================

   // set expiration for cookie
   if (expiresFromNow.is_initialized())
   {
      cookie.setExpires(*expiresFromNow);
      oldCookie.setExpires(*expiresFromNow);
   }

   pResponse->addCookie(cookie);
   pResponse->addCookie(oldCookie);
   return csrfToken;
}

std::string getCSRFTokenCookie(const Request& request)
{
   std::string tokenCookie = request.cookieValue(kCSRFTokenCookie);
   // NOTE: Remove block when Ghost Orchid 2021.09 is not supported ======================================
   // If the token is empty, extract the old version
   if (tokenCookie.empty())
      tokenCookie = request.cookieValue(kOldCSRFTokenCookie);
   // ====================================================================================================

   return tokenCookie;
}

std::string getCSRFTokenHeader(const Request& request)
{
   std::string headerToken = request.headerValue(kCSRFTokenHeader);

   // NOTE: Remove block when Ghost Orchid 2021.09 is not supported ======================================
   // Fallback on the old version for backward compatibility
   if (headerToken.empty())
      headerToken = request.headerValue(kOldCSRFTokenHeader);
   // ====================================================================================================

   return headerToken;
}

bool validateCSRFForm(const http::Request& request, 
                      http::Response* pResponse)
{
   // extract token from HTTP cookie (set above)
   std::string headerToken = getCSRFTokenCookie(request);


   http::Fields fields;

   // parse the form and check for a matching token
   http::util::parseForm(request.body(), &fields);
   std::string bodyToken = http::util::fieldValue<std::string>(fields,
         kCSRFTokenCookie, "");

   // NOTE: Remove block when Ghost Orchid 2021.09 is not supported ======================================
   // If the token is empty, extract the old version
   if (bodyToken.empty())
      bodyToken = http::util::fieldValue<std::string>(fields, kOldCSRFTokenCookie, "");
   // ====================================================================================================

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

bool validateCSRFHeaders(const http::Request& request)
{

   std::string cookieToken = getCSRFTokenCookie(request);
   std::string headerToken = getCSRFTokenHeader(request);

   if (headerToken.empty() || headerToken != cookieToken)
      return false;

   return true;
}

} // namespace http
} // namespace core
} // namespace rstudio
