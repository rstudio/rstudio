/*
 * ServerSecureUriHandler.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef SERVER_AUTH_SECURE_URI_HANDLER_HPP
#define SERVER_AUTH_SECURE_URI_HANDLER_HPP

#include <boost/function.hpp>

#include <core/http/UriHandler.hpp>
#include <core/http/AsyncUriHandler.hpp>

namespace core {
namespace http { 
   class Request;
   class Response;
} // namespace http
} // namespace core

namespace server {
namespace auth {
   
typedef boost::function<void(
                           const std::string& username,
                           const core::http::Request&,
                           core::http::Response*)> SecureUriHandlerFunction ;

typedef boost::function<void(
                     const std::string& username,
                     boost::shared_ptr<core::http::AsyncConnection>)>
                                          SecureAsyncUriHandlerFunction;

      
core::http::UriHandlerFunction secureHttpHandler(
                                    SecureUriHandlerFunction handler,
                                    bool authenticate = false);

core::http::UriHandlerFunction secureJsonRpcHandler(
                                    SecureUriHandlerFunction handler);

core::http::UriHandlerFunction secureUploadHandler(
                                    SecureUriHandlerFunction handler);

core::http::AsyncUriHandlerFunction secureAsyncHttpHandler(
                                    SecureAsyncUriHandlerFunction handler,
                                    bool authenticate = false);

core::http::AsyncUriHandlerFunction secureAsyncJsonRpcHandler(
                                    SecureAsyncUriHandlerFunction handler);

core::http::AsyncUriHandlerFunction secureAsyncUploadHandler(
                                    SecureAsyncUriHandlerFunction handler);

} // namespace auth
} // namespace server

#endif // SERVER_AUTH_SECURE_URI_HANDLER_HPP

