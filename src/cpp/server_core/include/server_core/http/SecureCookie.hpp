/*
 * SecureCookie.hpp
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

#ifndef CORE_HTTP_SECURE_COOKIE_HPP
#define CORE_HTTP_SECURE_COOKIE_HPP

#include <string>
#include <boost/optional.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

#include <core/http/Cookie.hpp>

namespace rstudio {
namespace core {
   class Error;
   class FilePath;
   namespace http {
      class Request;
      class Response;
   }
}
}

namespace rstudio {
namespace core {
namespace http {
namespace secure_cookie {

// validDuration is the amount of time this cookie is valid from now
// loginExpiry is the time at which the user's login expires
// the difference between loginExpiry and validDuration is that the user's
// auth can be refreshed when validDuration expires but not when loginExpiry
// is hit
http::Cookie createSecureCookie(const std::string& name,
                                const std::string& value,
                                const core::http::Request& request,
                                const boost::posix_time::time_duration& validDuration,
                                const boost::optional<boost::posix_time::ptime>& loginExpiry,
                                const std::string& path = "/",
                                bool secure = false,
                                http::Cookie::SameSite sameSite = http::Cookie::SameSite::Undefined);

std::string readSecureCookie(const core::http::Request& request,
                             const std::string& name);

std::string readSecureCookie(const std::string& signedCookieValue);

// Reads the secure cookie. If reading fails or the cookie is invalid
// pValue will be set to an empty string and pExpires and pLoginExpiry
// will be undefined
void readSecureCookie(
   const std::string& signedCookieValue,
   std::string* pValue,
   boost::posix_time::ptime* pExpires,
   boost::optional<boost::posix_time::ptime>* pLoginExpiry);

core::Error hashWithSecureKey(const std::string& value, std::string* pHMAC);

http::Cookie set(const std::string& name,
         const std::string& value,
         const http::Request& request,
         const boost::posix_time::time_duration& validDuration,
         const boost::optional<boost::posix_time::time_duration>& expiresFromNow,
         const boost::optional<boost::posix_time::ptime>& loginExpiry,
         const std::string& path,
         http::Response* pResponse,
         bool secure,
         http::Cookie::SameSite sameSite);

void remove(const http::Request& request,
            const std::string& name,
            const std::string& path,
            core::http::Response* pResponse,
            bool secure,
            http::Cookie::SameSite sameSite);

// initialize with default secure cookie key file
core::Error initialize();

// initialize with specific secure cookie key file
core::Error initialize(const FilePath& secureKeyFile);
core::Error initialize(bool isLoadBalanced, const FilePath& secureKeyFile);

// initialize with specific secure cookie key and file
core::Error initialize(const std::string& secureKey, const FilePath& secureKeyFile);

const std::string& getKey();
const std::string& getKeyFileUsed();
const std::string& getKeyHash();

} // namespace secure_cookie
} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_SECURE_COOKIE_HPP
