/*
 * ServerAuthHandler.hpp
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
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
#include <shared_core/system/User.hpp>

#include <server/auth/ServerSecureUriHandler.hpp>

namespace rstudio {


namespace core {
namespace database {
   // Forward declare IConnection.
   class IConnection;
} // namespace database
} // namespace core

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

/*
 * The UserSession class currently is stored one instance per username and represents a valid login to a given
 * rserver. It's main role now is to allow a websocket socket connection to remain open, even after the user's session
 * cookie has expired, or more specifically the user's session cookie that was present when the websocket was created.
 * When auth-timeout-minutes is non-zero, session cookies are allocated with the expiration time of the session and so
 * need to be refreshed periodically to keep the session alive as long as the user interacts with it. A client such
 * as a vscode session or shiny app that uses the websocket though, will not see the new session cookie and so won't be
 * able to reconnect even if they keep that session active. Eventually, we can replace the refreshAuthCookies logic with
 * a UserSession that is backed by the database. In the meantime, this class tracks information so we can keep active websockets
 * alive, but consistently close them when the user signs out from another tab.
 */
class UserSession
{
   public:
      UserSession(const std::string& username) :
        username_(username), numConnections_(0)
      {
         updateLastActiveTime();
         lastCookieRefreshTime_ = lastActiveTime();
         lastSocketActiveTime_ = lastActiveTime();
      }

      /* Tracks the set of cookies that have been replaced due to refreshAuth, so we can revoke all of them when the user signs out */
      void addSessionCookie(const std::string& cookie)
      {
         sessionCookies_.push_back(cookie);
      }

      void sessionInvalidated();
      boost::posix_time::ptime lastActiveTime() { return lastActiveTime_; }
      boost::posix_time::ptime lastCookieRefreshTime() { return lastCookieRefreshTime_; }
      boost::posix_time::ptime lastSocketActiveTime() { return lastSocketActiveTime_; }
      const std::string& username() { return username_; }
      /* The number of currently open socket connections */
      const int numConnections() { return numConnections_; }
      void setNumConnections(const int val) 
      {
         numConnections_ = val;
      }

      static boost::shared_ptr<UserSession> lookupUserSession(const std::string& username);
      static boost::shared_ptr<UserSession> getOrCreateUserSession(const std::string& username);
      static boost::shared_ptr<UserSession> createUserSession(const std::string& username);
      static void removeUserSession(const std::string& username);
      static bool invalidateUserSession(const std::string& username);
      static void updateSessionLastActiveTime(const std::string& username);
      static void updateSocketLastActiveTime(const std::string& username);
      static void addUserSessionConnection(const std::string& username);
      static void removeUserSessionConnection(const std::string& username);
      static bool userSessionValid(const std::string& username);
      static void insertSessionCookie(const std::string& username, const std::string& cookie);

   private:
      void invalidateSessionCookies();
      static void addToUserSessionConnection(const std::string& username, const int val);
      void updateLastActiveTime();
      void updateLastCookieRefreshTime();
      /* Tracks the last time we read data from the client-side of the socket, to detect idle websockets */
      void updateSocketLastActiveTime();

      std::string username_;
      boost::posix_time::ptime lastCookieRefreshTime_;
      boost::posix_time::ptime lastActiveTime_; 
      boost::posix_time::ptime lastSocketActiveTime_; 
      std::vector<std::string> sessionCookies_;
      int numConnections_;
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
void applyRemoteRevokedCookie(const std::string& cookie);

// refreshes the auth cookie silently (without user intervention)
// invoked when the user performs an active action against the system
// which "resets" his idle time, generating a new auth cookie
void refreshAuthCookies(const std::string& userIdentifier,
                        const core::http::Request& request,
                        core::http::Response* pResponse);

void invalidateAuthCookie(const std::string& cookie,
                          core::ExponentialBackoffPtr backoffPtr = core::ExponentialBackoffPtr());

core::Error initialize();

bool isCookieRevoked(const std::string& cookie);

// User functions
core::Error addUser(boost::asio::io_service& ioService, const std::string& username, bool isAdmin = false);
core::json::Array getAllUsers();
core::Error getUserFromDatabase(const boost::shared_ptr<core::database::IConnection>& connection,
                                const core::system::User& user,
                                bool* pLocked,
                                boost::posix_time::ptime* pLastSignin,
                                bool* pExists);
bool isUserActive(const boost::posix_time::ptime& lastSignin);
core::Error updateLastSignin(const boost::shared_ptr<core::database::IConnection>& connection,
                             const core::system::User& user);

// This function does not create a transaction - if needed, it must be created prior to calling and committed after
core::Error addUserToDatabase(const boost::shared_ptr<core::database::IConnection>& connection,
                              const core::system::User& user,
                              bool isAdmin);

core::Error isUserLicensed(const std::string& username,
                           bool* pLicensed);
core::Error isUserLicensed(const core::system::User& user,
                           bool isAdmin,
                           bool* pLicensed);
unsigned int getActiveUserCount();
std::string getExpiredDateStr();
core::Error getNumActiveUsers(const boost::shared_ptr<core::database::IConnection>& connection,
                              size_t* pNumActiveUsers);

namespace overlay {

core::Error initialize();
bool canStaySignedIn();
bool isUserListCookieValid(const std::string& cookieValue);
bool shouldShowUserLicenseWarning();
bool isUserAdmin(const std::string& username);
bool isUserLocked(bool lockedColumn);
std::string getUserListCookieValue();
unsigned int getNamedUserLimit();
core::json::Array getLicensedUsers();
core::Error lockUser(boost::asio::io_service& ioService, const std::string& username);
core::Error unlockUser(boost::asio::io_service& ioService, const std::string& username);
core::Error setAdmin(boost::asio::io_service& ioService, const std::string& username, bool isAdmin);

} // namespace overlay

} // namespace handler
} // namespace auth
} // namespace server
} // namespace rstudio

#endif // SERVER_AUTH_HANDLER_HPP


