/*
 * ServerAuthHandler.hpp
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

#ifndef SERVER_AUTH_HANDLER_HPP
#define SERVER_AUTH_HANDLER_HPP

#include <string>

#include <boost/function.hpp>

#include <core/ExponentialBackoff.hpp>
#include <core/http/UriHandler.hpp>
#include <core/http/AsyncUriHandler.hpp>

#include <shared_core/json/Json.hpp>

#include <server/auth/ServerSecureUriHandler.hpp>

namespace rstudio {
namespace server {
namespace auth {
namespace handler {

// uri constants
extern const char * const kSignIn;
extern const char * const kSignOut;
extern const char * const kRefreshCredentialsAndContinue;

// functions which can be called on the handler directly
std::string getUserIdentifier(const core::http::Request& request,
                              bool requireUserListCookie);

std::string userIdentifierToLocalUsername(const std::string& userIdentifier);

bool mainPageFilter(const core::http::Request& request,
                    core::http::Response* pResponse);

void signInThenContinue(const core::http::Request& request,
                        core::http::Response* pResponse);

// Special uri handler which attempts to refresh the user's
// credentials then continues on to the originally requested
// URI (or to a special override URI if specified). if the
// auth back-end doesn't support this behavior then it should
// redirect to the sign-in page
void refreshCredentialsThenContinue(
      boost::shared_ptr<core::http::AsyncConnection> pConnection);


// functions which must be provided by an auth handler
struct Handler
{
   boost::function<std::string(const core::http::Request&)> getUserIdentifier;
   boost::function<std::string(const std::string&)> userIdentifierToLocalUsername;
   core::http::UriFilterFunction mainPageFilter;
   core::http::UriHandlerFunction signInThenContinue;
   core::http::AsyncUriHandlerFunction refreshCredentialsThenContinue;
   core::http::AsyncUriHandlerFunction updateCredentials;
   core::http::UriHandlerFunction signIn;
   core::http::UriHandlerFunction signOut;

   boost::function<void(const core::http::Request&,
                        const std::string&,
                        bool,
                        core::http::Response*)> setSignInCookies;

   boost::function<void(const core::http::Request&,
                        const std::string&,
                        bool,
                        core::http::Response*)> refreshAuthCookies;
};

struct RevokedCookie
{
   RevokedCookie(const std::string& cookie);

   std::string cookie;
   boost::posix_time::ptime expiration;
};

// register the auth handler
void registerHandler(const Handler& handler);

// is there a handler already registered?
bool isRegistered();

// set sign in cookies
bool canSetSignInCookies();
void setSignInCookies(const core::http::Request& request,
                      const std::string& username,
                      bool persist,
                      core::http::Response* pResponse);

// sign out
void signOut(const core::http::Request& request,
             core::http::Response* pResponse);

// checks whether the user is attempting to sign in again too rapidly
// used to prevent inordinate generation of expired tokens
bool isUserSignInThrottled(const std::string& user);

void insertRevokedCookie(const RevokedCookie& cookie);

// refreshes the auth cookie silently (without user intervention)
// invoked when the user performs an active action against the system
// which "resets" his idle time, generating a new auth cookie
void refreshAuthCookies(const std::string& userIdentifier,
                        const core::http::Request& request,
                        core::http::Response* pResponse);

void invalidateAuthCookie(const std::string& cookie,
                          core::ExponentialBackoffPtr backoffPtr = core::ExponentialBackoffPtr());

core::Error initialize();

namespace overlay {

core::Error initialize();
bool canStaySignedIn();
core::Error isUserLicensed(const std::string& username,
                           bool* pLicensed);
bool isUserListCookieValid(const std::string& cookieValue);
bool shouldShowUserLicenseWarning();
bool isUserAdmin(const std::string& username);
std::string getUserListCookieValue();
unsigned int getActiveUserCount();
core::json::Array getLicensedUsers();
core::json::Array getAllUsers();
core::Error lockUser(boost::asio::io_service& ioService, const std::string& username);
core::Error unlockUser(boost::asio::io_service& ioService, const std::string& username);
core::Error setAdmin(boost::asio::io_service& ioService, const std::string& username, bool isAdmin);
core::Error addUser(boost::asio::io_service& ioService, const std::string& username, bool isAdmin);

} // namespace overlay

} // namespace handler
} // namespace auth
} // namespace server
} // namespace rstudio

#endif // SERVER_AUTH_HANDLER_HPP


