/*
 * SessionHttpConnectionUtils.hpp
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

#ifndef SESSION_HTTP_CONNECTION_UTILS_HPP
#define SESSION_HTTP_CONNECTION_UTILS_HPP

#include <session/SessionHttpConnection.hpp>

#include <string>

#include <boost/function.hpp>

namespace rstudio {
namespace core {
namespace http {
   class Request;
}
}
}

namespace rstudio {
namespace session {
namespace connection {

std::string rstudioRequestIdFromRequest(const core::http::Request& request);

bool isMethod(boost::shared_ptr<HttpConnection> ptrConnection,
              const std::string& method);


bool isGetEvents(boost::shared_ptr<HttpConnection> ptrConnection);

void handleAbortNextProjParam(
               boost::shared_ptr<HttpConnection> ptrConnection);

bool checkForAbort(boost::shared_ptr<HttpConnection> ptrConnection,
                   const boost::function<void()> cleanupHandler);

bool checkForSuspend(boost::shared_ptr<HttpConnection> ptrConnection);

bool checkForInterrupt(boost::shared_ptr<HttpConnection> ptrConnection);

bool authenticate(boost::shared_ptr<HttpConnection> ptrConnection,
                  const std::string& secret);


} // namespace connection
} // namespace session
} // namespace rstudio

#endif // SESSION_HTTP_CONNECTION_HPP

