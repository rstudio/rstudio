/*
 * ServerUriHandlers.hpp
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

#ifndef SERVER_URI_HANDLERS_HPP
#define SERVER_URI_HANDLERS_HPP

#include <string>

#include <core/http/UriHandler.hpp>
#include <core/http/AsyncUriHandler.hpp>

namespace rstudio {
namespace server {
namespace uri_handlers {

// add async uri handler
void add(const std::string& prefix,
         const core::http::AsyncUriHandlerFunction& handler);

void addUploadHandler(const std::string& prefix,
                      const core::http::AsyncUriUploadHandlerFunction& handler);

// add proxy handler
// proxy handlers have special behavior to allow them to route all traffic
void addProxyHandler(const std::string& prefix,
                     const core::http::AsyncUriHandlerFunction& handler);

// add blocking uri handler
void addBlocking(const std::string& prefix,
                 const core::http::UriHandlerFunction& handler);

// set async default handler
void setDefault(const core::http::AsyncUriHandlerFunction& handler);

// set blocking default handler
void setBlockingDefault(const core::http::UriHandlerFunction& handler);

void setRequestFilter(const core::http::RequestFilter& filter);
void setResponseFilter(const core::http::ResponseFilter& filter);

} // namespace uri_handlers
} // namespace server
} // namespace rstudio

#endif // SERVER_URI_HANDLERS_HPP

