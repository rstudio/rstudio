/*
 * ServerAuthCommon.hpp
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

#ifndef SERVER_AUTH_COMMON_HPP
#define SERVER_AUTH_COMMON_HPP

#include <string>

#include <boost/function.hpp>

#include <core/http/UriHandler.hpp>
#include <core/http/AsyncConnection.hpp>
#include <core/http/Response.hpp>

namespace rstudio {
namespace server {
namespace auth {

namespace handler {
    struct Handler;
}

namespace common {

typedef boost::function<std::string(const std::string&)> UserIdentifierToLocalUsernameGetter;
typedef boost::function<std::string(const core::http::Request&)> UserIdentifierGetter;

bool mainPageFilter(const core::http::Request& request,
                    core::http::Response* pResponse,
                    UserIdentifierGetter getUserIdentifier);

void signInThenContinue(const core::http::Request& request,
                        core::http::Response* pResponse);

void refreshCredentialsThenContinue(boost::shared_ptr<core::http::AsyncConnection> pConnection);

void signIn(const core::http::Request& request,
            core::http::Response* pResponse,
            const std::string& templatePath,
            const std::string& formAction,
            std::map<std::string,std::string> variables = std::map<std::string,std::string>());

bool validateSignIn(const core::http::Request& request,
                    core::http::Response* pResponse);

bool doSignIn(const core::http::Request& request,
              core::http::Response* pResponse,
              const std::string& username,
              std::string appUri,
              bool persist,
              bool authenticated = true);

std::string getUserIdentifier(const core::http::Request& request);

std::string signOut(const core::http::Request& request,
                    core::http::Response* pResponse,
                    UserIdentifierGetter getUserIdentifier,
                    std::string signOutUrl);

void clearSignInCookies(const core::http::Request& request,
                        core::http::Response* pResponse);

void setSignInCookies(const core::http::Request& request,
                      const std::string& userIdentifier,
                      bool staySignedIn,
                      core::http::Response* pResponse);

void prepareHandler(handler::Handler& handler,
                    core::http::UriHandlerFunction signIn,
                    const std::string& signOutUrl,
                    UserIdentifierToLocalUsernameGetter userIdentifierToLocalUsername,
                    UserIdentifierGetter getUserIdentifier = NULL);

} // namespace common
} // namespace auth
} // namespace server
} // namespace rstudio

#endif // SERVER_AUTH_COMMON_HPP
