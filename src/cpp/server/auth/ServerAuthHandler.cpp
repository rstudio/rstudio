/*
 * ServerAuthHandler.cpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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

#ifndef SERVER_AUTH_HANDLER_CPP
#define SERVER_AUTH_HANDLER_CPP

#include <server/auth/ServerAuthHandler.hpp>

#include <boost/algorithm/string.hpp>

#include <core/DateTime.hpp>
#include <core/FileLock.hpp>
#include <core/FileSerializer.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/PosixUser.hpp>
#include <core/Thread.hpp>
#include <core/PeriodicCommand.hpp>
#include <server/ServerScheduler.hpp>

#include <server_core/ServerDatabase.hpp>
#include <server_core/http/SecureCookie.hpp>

#include <server/ServerConstants.hpp>
#include <server/ServerObject.hpp>
#include <server/ServerOptions.hpp>
#include <server/ServerUriHandlers.hpp>

#include <server/auth/ServerSecureUriHandler.hpp>
#include <server/auth/ServerAuthCommon.hpp>

#include <session/SessionScopes.hpp>

using namespace rstudio::core;
using namespace rstudio::core::database;

namespace rstudio {
namespace server {
namespace auth {
namespace handler {

namespace {

boost::posix_time::ptime cookieExpiration(const std::string& cookie)
{
   std::vector<std::string> cookieParts;
   boost::algorithm::split(cookieParts, cookie, boost::is_any_of("|"));

   if (cookieParts.size() > 1)
   {
      std::string expiration = cookieParts[1];
      std::string expirationStr = http::util::urlDecode(expiration);
      return http::util::parseHttpDate(expirationStr);
   }
   return boost::posix_time::second_clock::universal_time();
}

// map of last user sign in times to prevent users from creating
// inordinate amounts of revocation entries
std::map<std::string, boost::posix_time::ptime> s_loginTimes;

// sorted array of revoked cookies, where the first to expire appears as the first element
// allows for quickly removing the first element
std::deque<RevokedCookie> s_revokedCookies;

// Tracks the set of cookies that are authorized for the user session, so they can all be revoked on signout
std::map<std::string,boost::shared_ptr<UserSession>> s_userSessions;

boost::posix_time::ptime s_lastCookieCheckTime = boost::posix_time::second_clock::universal_time();
boost::posix_time::time_duration s_cookieCheckDuration = boost::posix_time::seconds(5);

// mutex for providing concurrent access to internal structures
// necessary because auth happens on the thread pool
boost::recursive_mutex s_mutex;

// global auth handler
Handler s_handler;

void updateCredentialsNotSupported(
      boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   // alias response
   http::Response* pResponse = &(pConnection->response());

   // gwt form panel requires text/html content type
   pResponse->setContentType("text/html");

   // set method not found error
   Error methodNotFoundError(json::errc::MethodNotFound, ERROR_LOCATION);
   json::setJsonRpcError(methodNotFoundError, pResponse);

   // write response
   pConnection->writeResponse();
}

Error readRevocationListFromDatabase(std::vector<std::string>* pEntries)
{
   // establish a new transaction with the database
   boost::shared_ptr<IConnection> connection = server_core::database::getConnection();
   Transaction transaction(connection);

   // first, delete all stale cookies from the database
   std::string expiration = date_time::format(boost::posix_time::microsec_clock::universal_time(),
                                              date_time::kIso8601Format);
   Query deleteQuery = connection->query("DELETE FROM revoked_cookie WHERE expiration <= :val")
         .withInput(expiration);
   Error error = connection->execute(deleteQuery);
   if (error)
   {
      error.addProperty("description", "Could not delete expired revoked cookies from the database");
      return error;
   }

   // get all cookie entries from the database
   Query fetchQuery = connection->query("SELECT cookie_data FROM revoked_cookie");
   Rowset rowset;
   error = connection->execute(fetchQuery, rowset);
   if (error)
   {
      error.addProperty("description", "Could not retrieve revoked cookies from the database");
      return error;
   }

   for (RowsetIterator it = rowset.begin(); it != rowset.end(); ++it)
   {
      Row& row = *it;
      pEntries->push_back(row.get<std::string>(0));
   }

   transaction.commit();
   return Success();
}

void removeStaleCookieFromDatabase(const RevokedCookie& cookie,
                                   const boost::shared_ptr<IConnection>& connection)
{
   std::string expiration = date_time::format(cookie.expiration, date_time::kIso8601Format);
   Query query = connection->query("DELETE FROM revoked_cookie WHERE cookie_data = :dat")
         .withInput(cookie.cookie);

   Error error = connection->execute(query);
   if (error)
   {
      error.addProperty("description", "Could not delete expired revoked cookie from the database");
      LOG_ERROR(error);
   }
}


Error writeRevokedCookieToDatabase(const RevokedCookie& cookie,
                                   boost::shared_ptr<IConnection> connection = boost::shared_ptr<IConnection>())
{
   std::string expiration = date_time::format(cookie.expiration, date_time::kIso8601Format);

   // use existing connection if passed in, otherwise grab a new one
   if (!connection)
      connection = server_core::database::getConnection();

   Query query = connection->query("INSERT INTO revoked_cookie VALUES (:exp, :dat)")
         .withInput(expiration)
         .withInput(cookie.cookie);

   Error error = connection->execute(query);
   // Ignore duplicate key errors for postgres and sqlite in case another cluster member has already processed this revoke cookie request
   if (error && !boost::algorithm::contains(error.getMessage(), "duplicate key") &&
                !boost::algorithm::contains(error.getMessage(), "UNIQUE constraint"))
   {
      error.addProperty("description", "Could not insert revoked cookie into the database");
      return error;
   }

   return Success();
}

Error writeRevokedCookiesToDatabase()
{
   boost::shared_ptr<IConnection> connection = server_core::database::getConnection();
   Transaction transaction(connection);

   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      for (auto it = s_revokedCookies.begin(); it != s_revokedCookies.end(); ++it)
      {
         const RevokedCookie& cookie = *it;
         Error error = writeRevokedCookieToDatabase(cookie, connection);
         if (error)
            return error;
      }
   }
   END_LOCK_MUTEX

   transaction.commit();
   return Success();
}

Error readRevocationListFromFile(const FilePath& revocationList,
                                 std::vector<std::string>* pEntries)
{
   // read the current revocation list
   Error error = readStringVectorFromFile(revocationList, pEntries);
   if (error)
      return error;

   // remove stale entries from the list
   boost::posix_time::ptime now = boost::posix_time::second_clock::universal_time();
   for (int i = pEntries->size() - 1; i >= 0; i--)
   {
      const std::string& cookie = pEntries->at(i);

      boost::posix_time::ptime expirationDate = cookieExpiration(cookie);

      if (expirationDate <= now)
         pEntries->erase(pEntries->begin() + i);
   }

   return Success();
}

bool invalidateExpiredSessions()
{
   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      if (s_userSessions.size() == 0)
      {
         return true;
      }

      boost::posix_time::time_duration timeoutInterval = common::getCookieExpiry(true).get();

      boost::posix_time::ptime expireTime = boost::posix_time::second_clock::universal_time() - timeoutInterval;
      for (auto it = s_userSessions.begin(); it != s_userSessions.end();)
      {
         const boost::shared_ptr<UserSession> pUserSession = it->second;
         if (pUserSession->lastActiveTime() < expireTime)
         {
            if (pUserSession->numConnections() == 0 || pUserSession->lastSocketActiveTime() < expireTime)
            {
               // Because the user's session has passively expired, we are not going to actively revoke the cookies across the cluster.
               // Each cookie has an expire time built in so it will expire on its own in the browser
               // pUserSession->invalidateSessionCookies();
               LOG_DEBUG_MESSAGE("Expired UserSession for: " + pUserSession->username());

               it = s_userSessions.erase(it);
            }
            else
            {
               LOG_DEBUG_MESSAGE("Preserving expired UserSession with: " + std::to_string(pUserSession->numConnections()) +
                                 " for: " + pUserSession->username());
               ++it;
            }
         }
         else
         {
            ++it;
         }
      }
   }
   END_LOCK_MUTEX

   return true;
}

void addToUserSessionConnections(const std::string& username, int val)
{
   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      boost::shared_ptr<UserSession> pUserSession = UserSession::lookupUserSession(username);
      if (pUserSession)
      {
         int newCt = pUserSession->numConnections() + val;
         if (newCt < 0)
         {
            LOG_ERROR_MESSAGE("Invalid negative UserSession connection count: " + std::to_string(newCt));
            newCt = 0;
         }
         pUserSession->setNumConnections(newCt);
         LOG_DEBUG_MESSAGE("UserSession: " + username + " has: " + std::to_string(newCt) + " socket connections after adding: " + std::to_string(val));
      }
   }
   END_LOCK_MUTEX
}


} // anonymous namespace

bool isCookieRevoked(const std::string& cookie)
{
   if (cookie.empty())
      return true;

   bool attemptedConnection = false;
   boost::shared_ptr<IConnection> connection;
   boost::posix_time::ptime now = boost::posix_time::second_clock::universal_time();

   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      bool removeStaleCookies = now > s_lastCookieCheckTime + s_cookieCheckDuration;

      if (removeStaleCookies)
      {
         s_lastCookieCheckTime = now;
      }

      // check for cookie in revocation list, deleting expired elements as we go
      for (auto it = s_revokedCookies.begin(); it != s_revokedCookies.end();)
      {
         const RevokedCookie& other = *it;
         if (other.cookie == cookie)
         {
            return true;
         }

         if (removeStaleCookies && other.expiration <= now)
         {
            if (!attemptedConnection)
            {
               // grab a connection from the pool, but only wait for a short amount of time
               // this ensures that we do not delay the sign in process too long - deleting stale
               // cookies from the database immediately is not of critical importance, as this operation will
               // be retried on all subsequent auths / process restarts
               server_core::database::getConnection(boost::posix_time::milliseconds(500), &connection);
               attemptedConnection = true;
            }

            if (connection)
            {
               removeStaleCookieFromDatabase(other, connection);

               // we only erase cookies from the memory map if they were also removed from the database
               // to ensure that if we couldn't delete them immediately, we retry the operation later
               it = s_revokedCookies.erase(it);
               continue;
            }
            else
            {
               ++it;
            }
         }
         else
         {
            ++it;
         }
      }
   }
   END_LOCK_MUTEX

   return false;
}

namespace overlay {

Error initialize()
{
   return Success();
}

bool canStaySignedIn()
{
   return true;
}

Error isUserLicensed(const std::string& username,
                     bool* pLicensed)
{
   *pLicensed = true;
   return Success();
}

bool isUserListCookieValid(const std::string& cookieValue)
{
   return true;
}

bool shouldShowUserLicenseWarning()
{
   return false;
}

bool isUserAdmin()
{
   return false;
}

std::string getUserListCookieValue()
{
   return "9c16856330a7400cbbbba228392a5d83";
}

unsigned int getActiveUserCount()
{
   return 0;
}

json::Array getLicensedUsers()
{
   return json::Array();
}

json::Array getAllUsers()
{
   return json::Array();
}

Error lockUser(boost::asio::io_service& ioService,
               const std::string& username)
{
   return Success();
}

Error unlockUser(boost::asio::io_service& ioService,
                 const std::string& username)
{
   return Success();
}

Error setAdmin(boost::asio::io_service& ioService,
               const std::string& username,
               bool isAdmin)
{
   return Success();
}

Error addUser(boost::asio::io_service& ioService,
              const std::string& username,
              bool isAdmin)
{
   return Success();
}

} // namespace overlay

void onCookieRevoked(const std::string& cookie)
{
}

// uri constants
const char * const kSignIn = "/auth-sign-in";
const char * const kSignOut = "/auth-sign-out";
const char * const kRefreshCredentialsAndContinue = "/auth-refresh-credentials";

void onCookieRevoked(const std::string& cookie);

RevokedCookie::RevokedCookie(const std::string& cookie)
{
   this->cookie = cookie;
   this->expiration = cookieExpiration(cookie);
}

boost::shared_ptr<UserSession> UserSession::createUserSession(const std::string& username)
{
   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      boost::shared_ptr<UserSession> pUserSession = UserSession::lookupUserSession(username);
      if (pUserSession)
      {
         LOG_DEBUG_MESSAGE("Reusing active session for: " + username + " that did not expire and was not logged out");
         // This session may have been created from the same user with a different browser, but that user never explicitly signed out and the
         // session itself has not expired. We'll keep the cookies from that session so websockets using them will be closed if the user signs out.
         //UserSession::invalidateUserSession(username);
      }
      else
      {
         pUserSession = boost::make_shared<UserSession>(username);
         s_userSessions[username] = pUserSession;

         LOG_DEBUG_MESSAGE("Created new UserSession for: " + username + " (total: " + std::to_string(s_userSessions.size()) + ")");
      }
      return pUserSession;
   }
   END_LOCK_MUTEX
   return boost::shared_ptr<UserSession>();
}

void UserSession::updateSessionLastActiveTime(const std::string& username)
{
   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      boost::shared_ptr<UserSession> pUserSession = UserSession::getOrCreateUserSession(username);
      if (pUserSession)
      {
         pUserSession->updateLastActiveTime();
      }
   }
   END_LOCK_MUTEX
}

void UserSession::updateSocketLastActiveTime(const std::string& username)
{
   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      boost::shared_ptr<UserSession> pUserSession = UserSession::lookupUserSession(username);
      if (pUserSession)
      {
         pUserSession->updateSocketLastActiveTime();
      }
   }
   END_LOCK_MUTEX
}

boost::shared_ptr<UserSession> UserSession::lookupUserSession(const std::string& username)
{
   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      std::map<std::string, boost::shared_ptr<UserSession>>::iterator it = s_userSessions.find(username);
      if (it == s_userSessions.end())
         return boost::shared_ptr<UserSession>();
      return it->second;
   }
   END_LOCK_MUTEX
   return boost::shared_ptr<UserSession>(); // not reached
}

bool UserSession::userSessionValid(const std::string& username)
{
   boost::shared_ptr<UserSession> pUserSession = lookupUserSession(username);
   return pUserSession != nullptr;
}

void UserSession::addUserSessionConnection(const std::string& username)
{
   LOG_DEBUG_MESSAGE("Adding connection for: " + username);
   addToUserSessionConnections(username, 1);
}

void UserSession::removeUserSessionConnection(const std::string& username)
{
   LOG_DEBUG_MESSAGE("Removing a connection for: " + username);
   addToUserSessionConnections(username, -1);
}

boost::shared_ptr<UserSession> UserSession::getOrCreateUserSession(const std::string& username)
{
   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      boost::shared_ptr<UserSession> pUserSession = lookupUserSession(username);
      if (!pUserSession)
         pUserSession = UserSession::createUserSession(username);
      return pUserSession;
   }
   END_LOCK_MUTEX
   return boost::shared_ptr<UserSession>(); // not reached
}

void UserSession::removeUserSession(const std::string& username)
{
   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      s_userSessions.erase(username);
   }
   END_LOCK_MUTEX
}

bool UserSession::invalidateUserSession(const std::string& username)
{
   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      boost::shared_ptr<UserSession> pUserSession = lookupUserSession(username);
      if (pUserSession)
      {
          pUserSession->invalidateSessionCookies();
          UserSession::removeUserSession(username);
          return true;
      }
   }
   END_LOCK_MUTEX

   return false;
}

void UserSession::invalidateSessionCookies()
{
   for (std::string cookie : sessionCookies_)
   {
      invalidateAuthCookie(cookie);
   }
}

void UserSession::updateLastActiveTime()
{
   lastActiveTime_ = boost::posix_time::microsec_clock::universal_time();
}

void UserSession::updateSocketLastActiveTime()
{
   lastSocketActiveTime_ = boost::posix_time::microsec_clock::universal_time();
}

void UserSession::updateLastCookieRefreshTime()
{
   lastCookieRefreshTime_ = boost::posix_time::microsec_clock::universal_time();
}

std::string getUserIdentifier(const core::http::Request& request,
                              bool requireUserListCookie)
{
   if (isCookieRevoked(request.cookieValue(kUserIdCookie)))
      return std::string();

   std::string userIdentifier = s_handler.getUserIdentifier(request);
   if (userIdentifier.empty())
      return std::string();

   if (requireUserListCookie)
   {
      if (!overlay::isUserListCookieValid(request.cookieValue(kUserListCookie)))
         return std::string();
   }

   return userIdentifier;
}

std::string userIdentifierToLocalUsername(const std::string& userIdentifier)
{
   return s_handler.userIdentifierToLocalUsername(userIdentifier);
}

bool mainPageFilter(const core::http::Request& request,
                    core::http::Response* pResponse)
{
   return s_handler.mainPageFilter(request, pResponse);
}

void signInThenContinue(const core::http::Request& request,
                        core::http::Response* pResponse)
{
   s_handler.signInThenContinue(request, pResponse);
}

void refreshCredentialsThenContinue(
      boost::shared_ptr<core::http::AsyncConnection> pConnection)
{
   s_handler.refreshCredentialsThenContinue(pConnection);
}


// register the auth handler
void registerHandler(const Handler& handler)
{
   // set handler functions
   s_handler = handler;

   // register uri handlers
   uri_handlers::addBlocking(kSignIn, s_handler.signIn);

   // For signout, do not refresh auth cookies right before invalidating them
   uri_handlers::addBlocking(kSignOut,
          auth::secureHttpHandler(boost::bind(s_handler.signOut, _2, _3),
                                  false,   /* authenticate */
                                  true,    /* requireUserListCookie */
                                  false)); /* refreshAuthCookies */

   uri_handlers::add(kRefreshCredentialsAndContinue,
                     s_handler.refreshCredentialsThenContinue);

   uri_handlers::add("/auth-update-credentials",
                     s_handler.updateCredentials ?
                        s_handler.updateCredentials :
                        updateCredentialsNotSupported);
}

// is there a handler already registered?
bool isRegistered()
{
   return ! s_handler.getUserIdentifier.empty();
}

bool canSetSignInCookies()
{
   return !s_handler.setSignInCookies.empty();
}

void setSignInCookies(const core::http::Request& request,
                      const std::string& username,
                      bool persist,
                      core::http::Response* pResponse)
{
   s_handler.setSignInCookies(request, username, persist, pResponse);
}

void signOut(const http::Request& request, http::Response* pResponse)
{
   s_handler.signOut(request, pResponse);
}

bool isUserSignInThrottled(const std::string& user)
{
   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      auto it = s_loginTimes.find(user);

      boost::posix_time::ptime now = boost::posix_time::second_clock::universal_time();
      if (it == s_loginTimes.end())
      {
         s_loginTimes.insert({user, now});
         return false;
      }

      int throttlingSeconds = options().authSignInThrottleSeconds();
      if (it->second >
          now - boost::posix_time::seconds(throttlingSeconds))
      {
         // user is trying to sign back in too quickly
         // prevent the request
         LOG_WARNING_MESSAGE("Too many attempts within " + safe_convert::numberToString(throttlingSeconds) + " seconds for '" + user + "'");
         return true;
      }
      else
      {
         // user is fine to sign in - update their last sign in time to now
         it->second = now;
      }

      return false;
   }
   END_LOCK_MUTEX

   return false;
}

void UserSession::insertSessionCookie(const std::string& userIdentifier, const std::string& cookie)
{
   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      boost::shared_ptr<UserSession> session = UserSession::getOrCreateUserSession(userIdentifier);

      if (session)
      {
         LOG_DEBUG_MESSAGE("Adding session cookie: " + cookie + " for user: " + userIdentifier);
         session->addSessionCookie(cookie);
         session->updateLastCookieRefreshTime();
      }
   }
   END_LOCK_MUTEX
}

void refreshAuthCookies(const std::string& userIdentifier,
                        const http::Request& request,
                        http::Response* pResponse)
{
   if (server::options().authTimeoutMinutes() > 0 &&
       !s_handler.refreshAuthCookies.empty())
   {
      // Allocate new auth cookies periodically, rather than on every request to reduce CPU, and limit the
      // number of cookies to revoke should the user signout.
      boost::shared_ptr<UserSession> pUserSession = UserSession::getOrCreateUserSession(userIdentifier);
      boost::posix_time::ptime now = boost::posix_time::second_clock::universal_time();
      if (now < pUserSession->lastCookieRefreshTime() + boost::posix_time::seconds(30))
      {
         return;
      }

      // clear any existing auth cookies first - this method can be invoked multiple
      // times depending on the handler type (for example, an upload handler)
      pResponse->clearCookies();
      std::string persistCookie = request.cookieValue(kPersistAuthCookie);
      bool persist = persistCookie == "1" ? true : false;

      // We might have long-lasting socket connections using the old cookie, that will need to close
      // and reconnect when we refresh the auth cookie. For example, if a user signs out with a vscode
      // or shiny app running in another tab and the auth cookies are refreshed right before the signout,
      // we only will invalidate the new cookie unless we invalidate this one here.
      std::string currentCookie = request.cookieValue(kUserIdCookie);
      if (!currentCookie.empty())
      {
         LOG_DEBUG_MESSAGE("Refreshing auth: replacing old cookie: " + currentCookie);
      }

      s_handler.refreshAuthCookies(request, userIdentifier, persist, pResponse);
   }
}

void applyRemoteRevokedCookie(const std::string& cookieValue)
{
   insertRevokedCookie(RevokedCookie(cookieValue));
   std::string username = core::http::secure_cookie::readSecureCookie(cookieValue);
   if (!username.empty())
   {
      // We received a revoked cookie for this user from a remote rserver so we interpret that as signing out cluster-wide
      if (UserSession::invalidateUserSession(username))
         LOG_DEBUG_MESSAGE("Invalidated UserSession for: " + username + " from revoked cookie received from remote server");
   }
}


void insertRevokedCookie(const RevokedCookie& cookie)
{
   // do not insert the cookie if it is already expired
   if (cookie.expiration <= boost::posix_time::second_clock::universal_time())
      return;

   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      for (auto it = s_revokedCookies.begin(); it != s_revokedCookies.end(); ++it)
      {
         const RevokedCookie& other = *it;
         if (other.expiration > cookie.expiration)
         {
            s_revokedCookies.insert(it, cookie);
            return;
         }
      }

      // no elements were greater - insert at the end
      s_revokedCookies.insert(s_revokedCookies.end(), cookie);
   }
   END_LOCK_MUTEX
}

void invalidateAuthCookie(const std::string& cookie,
                          ExponentialBackoffPtr backoffPtr)
{
   LOG_DEBUG_MESSAGE("Invalidated auth cookie: " + cookie);

   if (cookie.empty())
      return;

   RevokedCookie revokedCookie(cookie);

   // store the revoked cookie in memory - we only check the memory cache when
   // checking incoming requests to see if the cookie presented has been revoked
   // because it is too expensive to hit the disk every time
   insertRevokedCookie(revokedCookie);

   // attempt to revoke the cookie - if the database insert fails, we still notify
   // other nodes so they can at least update their in-memory cache
   Error error = writeRevokedCookieToDatabase(revokedCookie);
   if (error)
      LOG_ERROR(error);

   onCookieRevoked(cookie);
}

Error initialize()
{
   // initialize by loading the current contents of the revocation list into memory

   // first, look for an existing file-based revocation list
   // RStudio versions prior to 1.4 wrote the list to a file, as database integration did not yet exist
   FilePath rootDir = options().authRevocationListDir();
   FilePath revocationList = rootDir.completeChildPath("revocation-list");
   FilePath revocationLockFile = rootDir.completeChildPath("revocation-list.lock");

   // create a file lock to gain exclusive access to the revocation list
   boost::shared_ptr<FileLock> lock = FileLock::createDefault();
   int numTries = 0;

   bool lockAcquired = false;
   while (numTries < 30)
   {
      // only attempt file locking if the revocation list exists
      // if it does not, then we have already previously migrated to the database
      if (revocationList.exists())
      {
         Error error = lock->acquire(revocationLockFile);
         if (error)
         {
            // if we could not acquire the lock, some other rserver process has
            // keep trying for some time before giving up
            ++numTries;
            boost::this_thread::sleep(boost::posix_time::seconds(1));
            continue;
         }

         lockAcquired = true;

         // successfully acquired lock
         // migrate the revocation list file to the database if it exists

         // read the current revocation list into memory
         std::vector<std::string> revokedCookies;
         error = readRevocationListFromFile(revocationList, &revokedCookies);
         if (error)
         {
            error.addProperty("description", "Could not read revocation list");
            LOG_ERROR(error);
         }

         for (const std::string& cookie : revokedCookies)
            insertRevokedCookie(RevokedCookie(cookie));

         // write the revocation list to the database
         error = writeRevokedCookiesToDatabase();
         if (error)
         {
            error.addProperty("description", "Could not migrate revocation list to the database");
            LOG_ERROR(error);
         }

         // now that the revocation list has been moved to the database, remove the old file-based list
         error = revocationList.removeIfExists();
         if (error)
            LOG_ERROR(error);
      }
      else
      {
         // load the initial revocation list from the database
         std::vector<std::string> revokedCookies;
         Error error = readRevocationListFromDatabase(&revokedCookies);
         if (error)
         {
            error.addProperty("description", "Could not read revoked cookies from the database");
            return error;
         }

         for (const std::string& cookie : revokedCookies)
            insertRevokedCookie(RevokedCookie(cookie));
      }

      if (lockAcquired)
      {
         Error error = lock->release();
         if (error)
            LOG_ERROR(error);
      }

      // Periodically cleanup idle/expired UserSessions, used for validating websocket connections
      scheduler::addCommand(boost::shared_ptr<ScheduledCommand>(
         new PeriodicCommand(boost::posix_time::seconds(10),
                             boost::bind(invalidateExpiredSessions),
                             false)));

      return overlay::initialize();
   }

   // the file lock could not be acquired
   return systemError(boost::system::errc::resource_unavailable_try_again,
                      "Could not acquire revocation list file lock",
                      ERROR_LOCATION);
}

} // namespace handler
} // namespace auth
} // namespace server
} // namespace rstudio

#endif // SERVER_AUTH_HANDLER_CPP


