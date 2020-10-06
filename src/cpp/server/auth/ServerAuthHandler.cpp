/*
 * ServerAuthHandler.cpp
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

#include <server_core/ServerDatabase.hpp>

#include <server/ServerConstants.hpp>
#include <server/ServerObject.hpp>
#include <server/ServerOptions.hpp>
#include <server/ServerUriHandlers.hpp>

#include <server/auth/ServerSecureUriHandler.hpp>

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

// mutex for providing concurrent access to internal structures
// necessary because auth happens on the thread pool
boost::mutex s_mutex;

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
   if (error)
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

   LOCK_MUTEX(s_mutex)
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

bool isCookieRevoked(const std::string& cookie)
{
   bool attemptedConnection = false;
   boost::shared_ptr<IConnection> connection;
   boost::posix_time::ptime now = boost::posix_time::second_clock::universal_time();

   LOCK_MUTEX(s_mutex)
   {
      // check for cookie in revocation list, deleting expired elements as we go
      for (auto it = s_revokedCookies.begin(); it != s_revokedCookies.end();)
      {
         const RevokedCookie& other = *it;
         if (other.cookie == cookie)
            return true;

         if (other.expiration <= now)
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

} // anonymous namespace

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

   uri_handlers::addBlocking(kSignOut,
                             auth::secureHttpHandler(
                                boost::bind(s_handler.signOut, _2, _3)));

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
   LOCK_MUTEX(s_mutex)
   {
      auto it = s_loginTimes.find(user);

      boost::posix_time::ptime now = boost::posix_time::second_clock::universal_time();
      if (it == s_loginTimes.end())
      {
         s_loginTimes.insert({user, now});
         return false;
      }

      if (it->second >
          now - boost::posix_time::seconds(options().authSignInThrottleSeconds()))
      {
         // user is trying to sign back in too quickly
         // prevent the request
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

void refreshAuthCookies(const std::string& userIdentifier,
                        const http::Request& request,
                        http::Response* pResponse)
{
   if (server::options().authTimeoutMinutes() > 0 &&
       !s_handler.refreshAuthCookies.empty())
   {
      // clear any existing auth cookies first - this method can be invoked multiple
      // times depending on the handler type (for example, an upload handler)
      pResponse->clearCookies();
      std::string persistCookie = request.cookieValue(kPersistAuthCookie);
      bool persist = persistCookie == "1" ? true : false;
      s_handler.refreshAuthCookies(request, userIdentifier, persist, pResponse);
   }
}

void insertRevokedCookie(const RevokedCookie& cookie)
{
   // do not insert the cookie if it is already expired
   if (cookie.expiration <= boost::posix_time::second_clock::universal_time())
      return;

   LOCK_MUTEX(s_mutex)
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


