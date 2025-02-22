/*
 * ServerAuthHandler.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
#include <server/auth/ServerAuthHandlerOverlay.hpp>

#include <boost/algorithm/string.hpp>

#include <core/DateTime.hpp>
#include <core/FileLock.hpp>
#include <core/FileSerializer.hpp>
#include <core/Log.hpp>
#include <core/json/JsonRpc.hpp>
#include <core/system/PosixUser.hpp>
#include <core/system/User.hpp>
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

#include "../ServerMetrics.hpp"

using namespace rstudio::core;
using namespace rstudio::core::database;

namespace rstudio {
namespace server {
namespace auth {
namespace handler {

namespace {

Error dbUnavailableError(const std::string& msg,
                         const std::string& username,
                         const ErrorLocation& location)
{
   Error error = systemError(
            boost::system::errc::resource_unavailable_try_again,
            msg + " - user storage table temporarily unavailable",
            location);
   error.addProperty("user", username);
   return error;
}

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

// Stores revoked cookies that have been expired and can be purged from the db
std::vector<RevokedCookie> s_expiredCookies;

// Tracks the set of cookies that are authorized for the user session, so they can all be revoked on signout
std::map<std::string,boost::shared_ptr<UserSession>> s_userSessions;

std::map<std::string,UidType> s_usernameToUID;

std::map<UidType,std::string> s_UIDToUsername;

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

   Query query = connection->query("INSERT INTO revoked_cookie (expiration, cookie_data) VALUES (:exp, :dat)")
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
   int expiredSessions = 0;
   int preservedSessions = 0;
   int activeSessions = 0;

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
               expiredSessions++;

               it = s_userSessions.erase(it);
            }
            else
            {
               preservedSessions++;
               ++it;
            }
         }
         else
         {
            ++it;
         }
      }
      activeSessions = s_userSessions.size();
   }
   END_LOCK_MUTEX

   if (expiredSessions > 0)
      LOG_DEBUG_MESSAGE("Expired " + std::to_string(expiredSessions) + " sessions");
   if (preservedSessions > 0)
      LOG_DEBUG_MESSAGE("Preserved " + std::to_string(preservedSessions) + " expired sessions with open streaming connections");

   metrics::setActiveUserSessionCount(activeSessions);

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

void removeExpiredCookies()
{
   std::vector<RevokedCookie> toRemove;

   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      if (s_expiredCookies.size() == 0)
         return;

      // Get cookies to remove and clear the old list
      toRemove.swap(s_expiredCookies);
   }
   END_LOCK_MUTEX

   boost::shared_ptr<IConnection> connection;
   server_core::database::getConnection(boost::posix_time::milliseconds(500), &connection);

   if (connection)
   {
      LOG_DEBUG_MESSAGE("Begin expiring " + std::to_string(toRemove.size()) + " cookies");

      for (auto it = toRemove.begin(); it != toRemove.end(); it++)
      {
         const RevokedCookie& toRemove = *it;
         removeStaleCookieFromDatabase(toRemove, connection);
      }
      LOG_DEBUG_MESSAGE("Finished expiring cookies");
   }
   else
   {
      LOG_WARNING_MESSAGE("Failed to get db connection in 500ms - delaying expired cookie removal");

      RECURSIVE_LOCK_MUTEX(s_mutex)
      {
         for (auto it = toRemove.begin(); it != toRemove.end(); it++)
            s_expiredCookies.push_back(*it);
      }
      END_LOCK_MUTEX
   }
}

} // anonymous namespace

bool isCookieExpired(const std::string& cookie, boost::optional<boost::posix_time::ptime>* pLoginExpiry = nullptr)
{ 
   boost::posix_time::ptime now = boost::posix_time::second_clock::universal_time();
   std::string value;
   boost::posix_time::ptime expiry;
   boost::optional<boost::posix_time::ptime> loginExpiry;
   http::secure_cookie::readSecureCookie(cookie, &value, &expiry, &loginExpiry);

   if (pLoginExpiry)
      *pLoginExpiry = loginExpiry;

   return (loginExpiry && loginExpiry.get() < now) || (expiry < now);
}

bool isCookieRevoked(const std::string& cookie)
{
   if (cookie.empty())
      return true;

   boost::posix_time::ptime now = boost::posix_time::second_clock::universal_time();

   bool removeStaleCookies;
   bool expireCookies = false;

   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      removeStaleCookies = now > s_lastCookieCheckTime + s_cookieCheckDuration;

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
            expireCookies = true;
            // Put it in the list to remove from the DB
            s_expiredCookies.push_back(other);
            // Remove it from this list
            it = s_revokedCookies.erase(it);
         }
         else
         {
            ++it;
         }
      }
   }
   END_LOCK_MUTEX

   if (expireCookies)
      removeExpiredCookies();

   return false;
}

Error getUserFromDatabase(const boost::shared_ptr<IConnection>& connection,
                          const system::User& user,
                          bool* pLocked,
                          boost::posix_time::ptime* pLastSignin,
                          bool* pExists)
{
   LOG_DEBUG_MESSAGE("User db query start: " + user.getUsername());
   
   *pLocked = true;

   const auto usernameColName = overlay::getUsernameDbColumnName();
   Query userQuery = connection
                         ->query("SELECT " + usernameColName +
                                 ", user_id, last_sign_in, locked FROM "
                                 "licensed_users WHERE user_id = :uid OR " +
                                 usernameColName + " = :un")
                         .withInput(user.getUserId())
                         .withInput(user.getUsername());

   Rowset rows;
   Error error = connection->execute(userQuery, rows);
   if (error)
   {
      error.addProperty("description", "Could not get user from database due to database error");
      return error;
   }

   // Old versions of RSW didn't set the posix User ID. If we don't have the ID, update it  now.
   bool foundUser = false;
   for (RowsetIterator itr = rows.begin(); itr != rows.end(); ++itr)
   {
      Row& row = *itr;

      // Check for null in the username, could happen if the user was added by an older version of RSW
      std::string username;
      error = overlay::checkForUninitializedUsername(connection, row, user, &username);
      if (error)
         return error;

      int uid = row.get<int>(1);
      bool locked = row.get<int>(3) == 1;

      // Check the last signin time. If we haven't seen a row for this user yet, update the time. 
      // Otherwise, if there are somehow multiple rows for this user, update
      // Ignore rows without a perfect username/uid match (except with uinitialized uids [<0])
      // Safe static cast, because we already handled uid < 0
      if ((username == user.getUsername()) && ((uid < 0) || (static_cast<UidType>(uid) == user.getUserId())))
      {
         boost::posix_time::ptime lastSignin = parseDateStr(row.get<std::string>(2));
         if (!foundUser || (lastSignin > *pLastSignin))
            *pLastSignin = lastSignin;

         // If any matching user row is not locked, the user is not locked.
         *pLocked = (overlay::isUserLocked(locked) && *pLocked);

         foundUser = true;
      }

      // If the user ID was uninitialized, update it now.
      if (row.get<int>(1) < 0)
      {
         LOG_DEBUG_MESSAGE("Updating posix user ID in database: " + user.getUsername());
   
         Query setUidQuery = connection->query("UPDATE licensed_users SET user_id = :uid WHERE user_name = :username and user_id = :olduid")
            .withInput(user.getUserId())
            .withInput(username)
            .withInput(uid);

         error = connection->execute(setUidQuery);
         if (error)
            return error;
      }
   }

   *pExists = foundUser;

   if (foundUser)
      LOG_DEBUG_MESSAGE("Found user in db: " + user.getUsername());
   else
      LOG_DEBUG_MESSAGE("No record found for user in db: " + user.getUsername());
   return Success();
}

Error addUser(boost::asio::io_context& ioContext,
              const std::string& username,
              bool isAdmin)
{
   const auto result = overlay::addUser(ioContext, username, isAdmin);
   Error overlayError;
   bool wasHandled;
   std::tie(overlayError, wasHandled) = result;
   if (overlayError)
   {
      return overlayError;
   }
   if (wasHandled)
   {
      return Success();
   }

   boost::shared_ptr<IConnection> connection;
   if (!server_core::database::getConnection(boost::posix_time::seconds(server::options().dbConnectionTimeout()), &connection))
   {
      return dbUnavailableError("Cannot add user", username, ERROR_LOCATION);
   }
   
   system::User user;
   Error error = system::getUserFromUsername(username, user);
   if (error)
      return error;

   // NOTE: this implementation runs the same query twice - once here in the call to 
   // getUserFromDatabase and once in isUserLicensed; however, this function is only called by the 
   // rstudio-server add-user command, so the slightly worse performance should not
   // be a concern.
   // check if user already exists - if they do, there's nothing for us to do
   bool exists = false, locked;
   boost::posix_time::ptime lastSignin;
   error = getUserFromDatabase(connection, user, &locked, &lastSignin, &exists);
   if (error)
      return error;

   if (!exists)
   {
      // user doesn't exist - check to see if we can add them
      bool isLicensed;
      Error error = isUserLicensed(user, isAdmin, &isLicensed, false);
      if (error)
      {
         return error;
      }

      if (!isLicensed)
      {
         return systemError(boost::system::errc::operation_canceled,
                            "Could not add user - there are not enough free slots for this license",
                            ERROR_LOCATION);
      }

      return Success();
   }
   else
   {
      return systemError(boost::system::errc::operation_canceled,
                         "Could not add user - user already exists",
                         ERROR_LOCATION);
   }
}

json::Array getAllUsers()
{
   boost::shared_ptr<IConnection> connection;
   if (!server_core::database::getConnection(boost::posix_time::seconds(server::options().dbConnectionTimeout()), &connection))
   {
      LOG_ERROR_MESSAGE("Could not get licensed users - "
                        "timed out while attempting to get database connection");
      return json::Array();
   }

   const auto usernameColName = overlay::getUsernameDbColumnName();
   Query query = connection->query("SELECT " + usernameColName + ", locked, last_sign_in, is_admin FROM licensed_users");

   Rowset rows;
   Error error = connection->execute(query, rows);
   if (error)
   {
      error.addProperty("description",
                        "Could not get licensed users due to database error");
      LOG_ERROR(error);
   }

   json::Array licensedUsers;
   for (RowsetIterator it = rows.begin(); it != rows.end(); ++it)
   {
      Row& row = *it;
      std::string username = row.get<std::string>(0);
      bool locked = static_cast<bool>(row.get<int>(1));
      std::string lastSignin = row.get<std::string>(2);
      bool isAdmin = static_cast<bool>(row.get<int>(3));

      std::string status;
      if (locked)
         status = "Locked";
      else
         status = overlay::isUserActive(parseDateStr(lastSignin), username)
                      ? "Active"
                      : "Inactive";

      json::Object user;
      user["username"] = username;
      user["status"] = status;
      user["isAdmin"] = isAdmin;

      licensedUsers.push_back(user);
   }

   return licensedUsers;
}

Error updateLastSignin(const boost::shared_ptr<IConnection>& connection,
                       const system::User& user)
{
   LOG_DEBUG_MESSAGE("Begin update last signIn time");
   std::string currentTime = date_time::format(boost::posix_time::microsec_clock::universal_time(),
                                               date_time::kIso8601Format);
   const auto usernameColName = overlay::getUsernameDbColumnName();
   Query updateSignin =
         connection->query("UPDATE licensed_users SET last_sign_in = :val WHERE user_id = :uid OR " + usernameColName + " = :pn ")
            .withInput(currentTime)
            .withInput(user.getUserId())
            .withInput(user.getUsername());
   LOG_DEBUG_MESSAGE("End update last signIn time");
   return connection->execute(updateSignin);
}

Error addUserToDatabase(const boost::shared_ptr<IConnection>& connection,
                        const system::User& user,
                        bool isAdmin)
{ 
   LOG_DEBUG_MESSAGE("Adding user to database: " + user.getUsername());

   Error error = overlay::addUserToDatabase(connection, user, isAdmin);

   if (error)
   {
      // if we cannot insert the user into the database, we count this as a hard failure
      error.addProperty("description",
                        "Could not add user to database: " + user.getUsername());
      return error;
   }

   return Success();
}

Error isUserLicensed(const std::string& username,
                     bool* pLicensed)
{
   system::User user;
   Error error = system::getUserFromUsername(username, user);
   if (error)
      return error;

   error = isUserLicensed(user, false, pLicensed);
   if (overlay::getNamedUserLimit() == 0)
   {
      if (error)
         LOG_ERROR(error);

      // when named user licensing is disabled, all users can sign in
      // there is no limit on active users, so swallow the result of
      // the licensing check
      *pLicensed = true;
      return Success();
   }
   else
   {
      if (!*pLicensed)
      {
         LOG_WARNING_MESSAGE("User '" + username + "' is locked or there is no license available");
      }
      // named user licensing is in place, so forward the result
      // of the licensing check
      return error;
   }
}

Error isUserLicensed(const system::User& user,
                     bool isAdmin,
                     bool* pLicensed,
                     bool isSigningIn)
{
   const unsigned int userLimit = overlay::getNamedUserLimit();

   boost::shared_ptr<IConnection> connection;
   if (!server_core::database::getConnection(boost::posix_time::seconds(server::options().dbConnectionTimeout()), &connection))
      return dbUnavailableError("Cannot check user license state", user.getUsername(), ERROR_LOCATION);

   // check to see if the user is in the list of named users
   boost::posix_time::ptime lastSignin;
   bool exists = false, locked = false;
   Error error = getUserFromDatabase(connection, user, &locked, &lastSignin, &exists);
   if (error)
      return error;

   // If they already exist, just update their last sign-in
   if (exists)
   {
      if (locked)
      {
         *pLicensed = false;
         return Success();
      }

      if (overlay::isUserActive(lastSignin, user.getUsername()))
      {
         *pLicensed = true;

         error = updateLastSignin(connection, user);
         if (error)
         {
            // we do not consider this update to be a hard failure, so simply log it
            // this is because the user has to be inactive for an entire year for this
            // to count against them, so no need to prevent them from signing in over this
            error.addProperty("description",
                              "Could not update last sign in time for user: " + user.getUsername());
            LOG_ERROR(error);
         }

         return Success();
      }
      else
      {
         // user is not active, meaning they haven't signed in for over a year
         // check to make sure this doesn't cause us to go over user limit
         // then attempt to making them active again by updating their signin time

         size_t numActiveUsers = 0;
         std::tie(numActiveUsers, error) = overlay::getActiveUserCount(connection);
         if (error)
            return error;

         if ((userLimit > 0) && (numActiveUsers >= userLimit))
         {
            *pLicensed = false;
            return Success();
         }
         else
         {
            error = updateLastSignin(connection, user);
            if (error)
            {
               // since we're trying to reactivate an old user, consider this a hard failure
               // as we could not be able to enforce the user limit if we allowed this sign
               // in to proceed
               error.addProperty(
                   "description",
                   "Could not reactivate user: " + user.getUsername() +
                       " - Unable to update last sign in time, which is "
                       "required to enforce license limits");
               return error;
            }

            // there was room for this user, so let them sign in
            *pLicensed = true;
            return Success();
         }
      }
   }
   else if (overlay::isUserProvisioningEnabled())
   {
      // When users are provisioned from an external identity provider, only
      // existing users can be considered licensed.
      *pLicensed = false;
      return Success();
   }
   else
   {
      // since the user is not already a named user, we need to either make them one
      // (by assigning them as an active user), or we've run out of spaces for the
      // license and auth should fail
      size_t numActiveUsers = 0;
      std::tie(numActiveUsers, error) = overlay::getActiveUserCount(connection);
      if (error)
         return error;

      if ((userLimit > 0) && (numActiveUsers >= userLimit))
      {
         // no more space for this user, so don't let them sign in
         *pLicensed = false;
         return Success();
      }
      
      error = addUserToDatabase(connection, user, isAdmin);
      if (error)
         return error;

      if(isSigningIn)
      {
         // If the user is signing in, we need to update their last sign-in time
         error = updateLastSignin(connection, user);
         if (error)
            return error;
      }
      
      // added successfully, and there's space for the user
      *pLicensed = true;
      return Success();
   }
}

Result<int> getUserId(const std::string& username,
                      database::DatabaseConnection connection)
{
   if (!connection)
      connection = server_core::database::getConnection();

   int userId = -1;
   std::string statement = "SELECT id FROM licensed_users";
   auto query = overlay::addUsernameCheckToQuery(connection, statement, username);
   query.withOutput(userId);

   Error error = connection->execute(query);
   if (error)
      return Unexpected(error);

   if (userId == -1)
      return Unexpected(systemError(boost::system::errc::invalid_argument,
                                    "User " + username + " does not exist",
                                    ERROR_LOCATION));

   return userId;
}

std::string getExpiredDateStr()
{
   boost::posix_time::ptime oneYearAgo = boost::posix_time::microsec_clock::universal_time() -
         boost::posix_time::time_duration(kHoursInOneYear, 0, 0);
   return date_time::format(oneYearAgo, date_time::kIso8601Format);
}

boost::posix_time::ptime parseDateStr(const std::string& strTime)
{
   boost::posix_time::ptime time;
   date_time::parseUtcTimeFromIso8601String(strTime, &time);
   return time;
}

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

void ensureDatabaseUser(const std::string& username)
{
   // Lookup the user in the database
   system::User user;
   Error error = system::getUserFromUsername(username, user);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   bool locked, exists;
   boost::posix_time::ptime lastSignIn;
   boost::shared_ptr<IConnection> connection = server_core::database::getConnection();
   error = getUserFromDatabase(connection, user, &locked, &lastSignIn, &exists);
   if (error)
   {
      LOG_ERROR(error);
   }
   else if (!exists)
   {
      error = addUserToDatabase(connection, user, false);
      if (error)
      {
         LOG_ERROR(error);
      }
   }
}

boost::shared_ptr<UserSession> UserSession::createUserSession(const std::string& username)
{
   boost::shared_ptr<UserSession> pUserSession;
   int activeSessions;
   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      pUserSession = UserSession::lookupUserSession(username);
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
      activeSessions = s_userSessions.size();
   }
   END_LOCK_MUTEX

   // Make sure there's a database record for the user when creating the session
   // *unless* users can be provisioned only by an external identity provider.
   if (!overlay::isUserProvisioningEnabled())
   {
      ensureDatabaseUser(username);
   }

   metrics::setActiveUserSessionCount(activeSessions);

   return pUserSession;
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
      bool sessionFound = it != s_userSessions.end();

      if (!sessionFound)
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
   int activeSessions;
   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      s_userSessions.erase(username);
      activeSessions = s_userSessions.size();
   }
   END_LOCK_MUTEX

   metrics::setActiveUserSessionCount(activeSessions);

   // Remove the user from the username and uid caches so we revalidate from scratch next time
   core::system::removeUserFromCache(username);
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
   std::string cookieValue = request.cookieValue(kUserIdCookie);
   if (isCookieRevoked(cookieValue) || isCookieExpired(cookieValue))
      return std::string();

   std::string userIdentifier = s_handler.getUserIdentifier(request);
   if (userIdentifier.empty())
      return std::string();

   if (requireUserListCookie)
   {
      std::string userListCookie = request.cookieValue(kUserListCookie);
      if (!overlay::isUserListCookieValid(userListCookie))
      {
         if (userListCookie.empty())
         {
            LOG_WARNING_MESSAGE("Request contains a user-identifier but is missing the "
                                "user-list-id cookie required for named user licensing.");
         }
         else
         {
            LOG_WARNING_MESSAGE("Request contains a user-indentifier with an invalid user-list-id "
                                "cookie");
         }
         return std::string();
      }
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

bool refreshAuthCookies(const std::string& userIdentifier,
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
         // No action, but the auth cookies are still valid
         return true;
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
         LOG_DEBUG_MESSAGE("Refreshing auth: uri: " + request.uri() + ": replacing old cookie: " + currentCookie);
      }

      boost::optional<boost::posix_time::ptime> loginExpiry;
      if (isCookieExpired(currentCookie, &loginExpiry))
      {
         // The cookie has hit the active expiry, so we should not refresh it
         LOG_DEBUG_MESSAGE("Not refreshing auth: uri: " + request.uri() + ": cookie has expired: " + currentCookie);
         return false;
      }

      s_handler.refreshAuthCookies(request, userIdentifier, persist, loginExpiry, pResponse);
   }

   // Cookies are valid
   return true;
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


bool insertRevokedCookie(const RevokedCookie& cookie)
{
   // do not insert the cookie if it is already expired
   if (cookie.expiration <= boost::posix_time::second_clock::universal_time())
      return false;

   RECURSIVE_LOCK_MUTEX(s_mutex)
   {
      for (auto it = s_revokedCookies.begin(); it != s_revokedCookies.end(); ++it)
      {
         const RevokedCookie& other = *it;
         if (other.expiration > cookie.expiration)
         {
            s_revokedCookies.insert(it, cookie);
            return true;
         }
      }

      // no elements were greater - insert at the end
      s_revokedCookies.insert(s_revokedCookies.end(), cookie);
   }
   END_LOCK_MUTEX

   return true;
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
   if (insertRevokedCookie(revokedCookie))
   {
      // attempt to revoke the cookie - if the database insert fails, we still notify
      // other nodes so they can at least update their in-memory cache
      Error error = writeRevokedCookieToDatabase(revokedCookie);
      if (error)
         LOG_ERROR(error);

      onCookieRevoked(cookie);
   }
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
