/*
 * ServerPortToken.hpp
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

#ifndef SERVER_PORT_TOKEN_HPP
#define SERVER_PORT_TOKEN_HPP

#include <string>
#include <boost/optional.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>

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
namespace port {

void setPortTokenCookie(const core::http::Request& request, 
      boost::optional<boost::gregorian::days> expiry,
      core::http::Response* pResponse);


} // namespace port
} // namespace auth

} // namespace server
} // namespace rstudio

#endif // SERVER_PORT_TOKEN_HPP

