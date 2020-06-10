/*
 * ServerEval.hpp
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

#ifndef SERVER_EVAL_HPP
#define SERVER_EVAL_HPP

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
namespace eval {
   
bool expirationFilter(const core::http::Request& request,
                      core::http::Response* pResponse);

} // namespace eval
} // namespace server
} // namespace rstudio

#endif // SERVER_EVAL_HPP

