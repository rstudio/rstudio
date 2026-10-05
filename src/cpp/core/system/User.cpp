/*
 * User.cpp
 * 
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant to the terms of a commercial license agreement
 * with Posit, then this program is licensed to you under the following terms:
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

#include <shared_core/system/User.hpp>

#include <pthread.h>
#include <pwd.h>

#include <boost/algorithm/string.hpp>

#include <chrono>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>
#include <shared_core/system/PosixSystem.hpp>

#include <core/system/PosixGroup.hpp>

#include <core/Log.hpp>

namespace rstudio {
namespace core {
namespace system {

namespace {

struct UserCache {
   User user;
   std::chrono::steady_clock::time_point cacheTime;
};

std::map<std::string, UserCache> s_usernameCache;
std::map<UidType, UserCache> s_userIdCache;

// A plain pthread mutex (rather than boost::mutex) so it can participate in pthread_atfork below: a child forked
// while another thread held this mutex (e.g. rserver launching a session) could otherwise never look up a user.
pthread_mutex_t s_userCacheMutex = PTHREAD_MUTEX_INITIALIZER;

class UserCacheLock
{
public:
   UserCacheLock()
   {
      ::pthread_mutex_lock(&s_userCacheMutex);
   }

   ~UserCacheLock()
   {
      ::pthread_mutex_unlock(&s_userCacheMutex);
   }
};

// Hold the mutex across fork so both the parent and the child resume with it unlocked (the child's sole thread is the
// forking thread, which owns it). Nothing logs under this mutex, but register after the logger's handlers regardless.
struct AtForkRegistration
{
   AtForkRegistration()
   {
      log::registerForkHandlers();
      ::pthread_atfork(lockUserCache, unlockUserCache, unlockUserCache);
   }

private:
   static void lockUserCache()
   {
      ::pthread_mutex_lock(&s_userCacheMutex);
   }

   static void unlockUserCache()
   {
      ::pthread_mutex_unlock(&s_userCacheMutex);
   }
};

AtForkRegistration s_atForkRegistration;

constexpr const std::chrono::duration<double> s_userCacheDuration = std::chrono::milliseconds(5*60*1000);

void addUserToCache(User& user)
{
   UserCache cacheEnt;
   cacheEnt.user = user;
   cacheEnt.cacheTime = std::chrono::steady_clock::now();
   {
      UserCacheLock lock;
      s_usernameCache[user.getUsername()] = cacheEnt;
      s_userIdCache[user.getUserId()] = cacheEnt;
   }
}

bool getUserFromNameCache(const std::string& in_username, User* pUser, bool* pExpired)
{
   std::chrono::steady_clock::time_point cacheTime;
   User cacheUser;

   {
      UserCacheLock lock;
      auto it = s_usernameCache.find(in_username);
      if (it == s_usernameCache.end())
      {
         *pExpired = false;
         return false;
      }
      else
      {
         cacheUser = it->second.user;
         cacheTime = it->second.cacheTime;
      }
   }

   std::chrono::steady_clock::time_point now = std::chrono::steady_clock::now();
   if (cacheTime + s_userCacheDuration > now)
   {
      *pUser = cacheUser;
      *pExpired = false;
      return true;
   }
   else
      *pExpired = true;
   return false;
}

bool getUserFromIdCache(UidType in_userId, User* pUser, bool* pExpired)
{
   std::chrono::steady_clock::time_point cacheTime;
   User cacheUser;

   {
      UserCacheLock lock;
      auto it = s_userIdCache.find(in_userId);
      if (it == s_userIdCache.end())
      {
         *pExpired = false;
         return false;
      }
      else
      {
         cacheUser = it->second.user;
         cacheTime = it->second.cacheTime;
      }
   }

   std::chrono::steady_clock::time_point now = std::chrono::steady_clock::now();
   if (cacheTime + s_userCacheDuration > now)
   {
      *pUser = cacheUser;
      *pExpired = false;
      return true;
   }
   else
      *pExpired = true;
   return false;
}

Error updateUserCacheByUsername(const std::string& in_username, User& out_user, const std::string& opNameForLog)
{
   Error error = User::getUserFromIdentifier(in_username, out_user);
   if (!error)
   {
      addUserToCache(out_user);

      return Success();
   }
   else
   {
      LOG_DEBUG_MESSAGE("Error from getUserFromIdentifier during: '" + opNameForLog + "' user: " + in_username + ": " + error.asString());
      return error;
   }
}

Error updateUserCacheByUserId(const UidType& in_userId, User& out_user, const std::string& opNameForLog)
{
   Error error = User::getUserFromIdentifier(in_userId, out_user);
   if (!error)
   {
      addUserToCache(out_user);

       return Success();
   }
   else
   {
      LOG_DEBUG_MESSAGE("Error from getUserFromIdentifier for: " + std::to_string(in_userId) + ": " + error.asString());
      return error;
   }
}

} // anon namespace 

Error getUserFromUsername(const std::string& in_username, User& out_user)
{
   bool expired;

   if (!getUserFromNameCache(in_username, &out_user, &expired))
      return updateUserCacheByUsername(in_username, out_user, expired ? "Updating" : "Adding");

   return Success();
}

Error getUserFromUserId(UidType in_userId, User& out_user)
{
   bool expired;

   if (!getUserFromIdCache(in_userId, &out_user, &expired))
      return updateUserCacheByUserId(in_userId, out_user, expired ? "Updating" : "Adding");

   return Success();
}

void removeUserFromCache(const std::string& in_username)
{
   int newSize;
   {
      UserCacheLock lock;
      auto it = s_usernameCache.find(in_username);
      if (it != s_usernameCache.end())
      {
          UidType uid = it->second.user.getUserId();
          s_usernameCache.erase(in_username);
          s_userIdCache.erase(uid);
      }
      newSize = s_usernameCache.size();
   }

   group::removeUserFromGroupCache(in_username);

   LOG_DEBUG_MESSAGE("Removed user: " + in_username + " from cache - (now: " + std::to_string(newSize) + ")");
}

} // namespace system
} // namespace core
} // namespace rstudio

