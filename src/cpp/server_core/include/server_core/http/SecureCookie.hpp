/*
 * SecureCookie.hpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

http::Cookie createSecureCookie(const std::string& name,
                                const std::string& value,
                                const core::http::Request& request,
                                const boost::posix_time::time_duration& validDuration,
                                const std::string& path = "/",
                                bool secure = false,
                                http::Cookie::SameSite sameSite = http::Cookie::SameSite::Undefined);

std::string readSecureCookie(const core::http::Request& request,
                             const std::string& name);

std::string readSecureCookie(const std::string& signedCookieValue);

core::Error hashWithSecureKey(const std::string& value, std::string* pHMAC);

void set(const std::string& name,
         const std::string& value,
         const http::Request& request,
         const boost::posix_time::time_duration& validDuration,
         const boost::optional<boost::posix_time::time_duration>& expiresFromNow,
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

const std::string& getKey();

} // namespace secure_cookie
} // namespace http
} // namespace core
} // namespace rstudio

#endif // CORE_HTTP_SECURE_COOKIE_HPP
