/*
 * ServerCSRFToken.hpp
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

#ifndef SERVER_CSRF_TOKEN_HPP
#define SERVER_CSRF_TOKEN_HPP

#include <string>

namespace rstudio {
namespace core {
   namespace http {
      class Request;
      class Response;
   }
}
}

namespace rstudio {
namespace server {
namespace auth {
namespace csrf {

// Adds a CSRF (cross site request forgery) cookie. This is simply a cookie with
// a random value (token).
void setCSRFTokenCookie(const core::http::Request&, core::http::Response*,
      const std::string& token = std::string());

// Validates an HTTP POST request by ensuring that the submitted fields include
// a valid CSRF token.
bool validateCSRFForm(const core::http::Request&, core::http::Response*);

} // namespace csrf
} // namespace auth
} // namespace server
} // namespace rstudio

#endif // SERVER_CSRF_TOKEN_HPP

