/*
 * ServerSecureUriHandler.hpp
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

#ifndef SERVER_AUTH_SECURE_URI_HANDLER_HPP
#define SERVER_AUTH_SECURE_URI_HANDLER_HPP

#include <boost/function.hpp>

#include <core/http/UriHandler.hpp>
#include <core/http/AsyncUriHandler.hpp>

namespace rstudio {
namespace core {
namespace http { 
   class Request;
   class Response;
} // namespace http
} // namespace core
} // namespace rstudio

namespace rstudio {
namespace server {
namespace auth {
   
typedef boost::function<void(
                           const std::string& username,
                           const core::http::Request&,
                           core::http::Response*)> SecureUriHandlerFunction;

typedef boost::function<void(
                     const std::string& username,
                     boost::shared_ptr<core::http::AsyncConnection>)>
                                          SecureAsyncUriHandlerFunction;

typedef boost::function<void(
                           const std::string& username,
                           const std::string& userIdentifier,
                           const core::http::Request&,
                           core::http::Response*)> SecureUriHandlerFunctionEx;

typedef boost::function<void(
                     const std::string& username,
                     const std::string& userIdentifier,
                     boost::shared_ptr<core::http::AsyncConnection>)>
                                          SecureAsyncUriHandlerFunctionEx;

typedef boost::function<bool(
                     const std::string& username,
                     const std::string& userIdentifier,
                     boost::shared_ptr<core::http::AsyncConnection>,
                     const std::string&,
                     bool)> SecureAsyncUriUploadHandlerFunctionEx;

typedef boost::variant<SecureAsyncUriHandlerFunctionEx,
                       SecureAsyncUriUploadHandlerFunctionEx> SecureAsyncUriHandlerFunctionExVariant;

      
core::http::UriHandlerFunction secureHttpHandler(
                                    SecureUriHandlerFunction handler,
                                    bool authenticate = false,
                                    bool requireUserListCookie = true);

core::http::UriHandlerFunction secureJsonRpcHandler(
                                    SecureUriHandlerFunction handler);

core::http::UriHandlerFunction secureJsonRpcHandlerEx(
                                    SecureUriHandlerFunctionEx handler);

core::http::UriHandlerFunction secureUploadHandler(
                                    SecureUriHandlerFunction handler);

core::http::AsyncUriHandlerFunction secureAsyncHttpHandler(
                                    SecureAsyncUriHandlerFunction handler,
                                    bool authenticate = false,
                                    bool refreshAuthCookies = true,
                                    bool requireUserListCookie = true);

core::http::AsyncUriHandlerFunction secureAsyncHttpHandler(
                                    SecureAsyncUriHandlerFunction handler,
                                    core::http::AsyncUriHandlerFunction unauthorizedResponseFunction,
                                    bool refreshAuthCookies,
                                    bool requireUserListCookie);

core::http::AsyncUriHandlerFunction secureAsyncJsonRpcHandler(
                                    SecureAsyncUriHandlerFunction handler);

core::http::AsyncUriHandlerFunction secureAsyncJsonRpcHandlerEx(
                                    SecureAsyncUriHandlerFunctionEx handler);

core::http::AsyncUriUploadHandlerFunction secureAsyncUploadHandler(
                                    SecureAsyncUriUploadHandlerFunctionEx handler);


} // namespace auth
} // namespace server
} // namespace rstudio

#endif // SERVER_AUTH_SECURE_URI_HANDLER_HPP

