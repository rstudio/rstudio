/*
 * ServerSecureCookie.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#ifndef SERVER_AUTH_SECURE_COOKIE_HPP
#define SERVER_AUTH_SECURE_COOKIE_HPP

#include <string>
#include <boost/optional.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

namespace core {
   class Error;
   namespace http {
      class Request;
      class Response;
   }
}

using namespace core;

namespace server {
namespace auth {
namespace secure_cookie {

std::string readSecureCookie(const core::http::Request& request,
                             const std::string& name);

void set(const std::string& name,
         const std::string& value,
         const http::Request& request,
         const boost::posix_time::time_duration& validDuration,
         const std::string& path,
         http::Response* pResponse);

void set(const std::string& name,
         const std::string& value,
         const http::Request& request,
         const boost::posix_time::time_duration& validDuration,
         const boost::optional<boost::gregorian::days>& cookieExpiresDays,
         const std::string& path,
         http::Response* pResponse);

void remove(const http::Request& request,
            const std::string& name,
            const std::string& path,
            core::http::Response* pResponse);

core::Error initialize();

} // namespace secure_cookie
} // namespace auth
} // namespace server

#endif // SERVER_AUTH_SECURE_COOKIE_HPP
