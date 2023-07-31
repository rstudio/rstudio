/*
 * PosixGroup.cpp
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

#include <core/system/PosixGroup.hpp>

#include <pwd.h>
#include <grp.h>
#include <unistd.h>

#include <chrono>
#include <iostream>

#include <boost/lexical_cast.hpp>

#include <shared_core/Error.hpp>
#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <shared_core/SafeConvert.hpp>
#include <core/system/System.hpp>
#include <core/system/User.hpp>
#include <core/Thread.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace group {

namespace {

const int kNotFoundError = EACCES;

struct GroupCache {
   Group group;
   std::chrono::steady_clock::time_point cacheTime;
};

std::map<std::string, GroupCache> s_groupNameCache;
std::map<gid_t, GroupCache> s_groupIdCache;

// Stores the list of group ids for a given user
struct UserGroupCache {
   std::vector<GidType> groupIds;
   std::chrono::steady_clock::time_point cacheTime;
};

std::map<std::string, UserGroupCache> s_userGroupCache;

boost::mutex s_groupCacheMutex;

constexpr const std::chrono::duration<double> s_groupCacheDuration = std::chrono::milliseconds(5*60*1000);

template <typename T>
Error groupFrom(const boost::function<int(
                 T, struct group*, char*, size_t, struct group**)>& getGroup,
                T value,
                Group* pGroup)
{
   struct group grp;
   struct group* temp;
   int buffSize = ::sysconf(_SC_GETGR_R_SIZE_MAX); // ask for buffer size
   if (buffSize == -1)
      buffSize = 4096; // some systems return -1, be conservative!

   // keep attempting to read until we pass a buffer of sufficient size
   std::vector<char> buffer;
   int result = 0;
   do
   {
      buffer.resize(buffSize);

      // attempt the read
      result = getGroup(value, &grp, buffer.data(), buffer.size(), &temp);

      // if we fail, double the buffer prior to retry
      if (result == ERANGE)
         buffSize *= 2;

   } while (result == ERANGE);

   if (temp == nullptr)
   {
      if (result == 0) // will happen if group is not found
         result = kNotFoundError;
      Error error = systemError(result, ERROR_LOCATION);
      error.addProperty("group-value", safe_convert::numberToString(value));
      return error;
   }
   else
   {
      pGroup->groupId = grp.gr_gid;
      pGroup->name = grp.gr_name;

      // populate vector of users
      char** pUsers = grp.gr_mem;
      while (*pUsers)
      {
         pGroup->members.push_back(*(pUsers++));
      }
   }
   return Success();
}

int addGroupToCache(const Group& group)
{
   GroupCache cacheEnt;
   cacheEnt.group = group;
   cacheEnt.cacheTime = std::chrono::steady_clock::now();
   LOCK_MUTEX(s_groupCacheMutex)
   {
      s_groupNameCache[group.name] = cacheEnt;
      s_groupIdCache[group.groupId] = cacheEnt;

      return s_groupNameCache.size();
   }
   END_LOCK_MUTEX

   return -1;
}

Error updateGroupCacheByName(const std::string& name, Group* pGroup, const std::string& opName)
{
   Error error = groupFrom<const char *>(::getgrnam_r, name.c_str(), pGroup);

   if (!error)
   {
      int newSize = addGroupToCache(*pGroup);

      LOG_DEBUG_MESSAGE(opName + " to group cache with name: " + name + ":" + std::to_string(pGroup->groupId) +
                        " to cache (size: " + std::to_string(newSize) + ") from name lookup");

      return Success();
   }
   else
   {
      LOG_DEBUG_MESSAGE("Error from groupFromName during: '" + opName + "' group: " + name + ": " + error.asString());

      return error;
   }
}

Error updateGroupCacheById(gid_t gid, Group* pGroup, const std::string& opName)
{
   Error error = groupFrom<gid_t>(::getgrgid_r, gid, pGroup);

   if (!error)
   {
      int newSize = addGroupToCache(*pGroup);

      LOG_DEBUG_MESSAGE(opName + " to group cache with: " + pGroup->name + ":" + std::to_string(gid) +
                        " to cache (size: " + std::to_string(newSize) + ") from id lookup");

      return Success();
   }
   else
   {
      LOG_DEBUG_MESSAGE("Error from groupFromId during: '" + opName + "' group id: " + std::to_string(gid) + ": " + error.asString());

      return error;
   }
}

std::vector<GidType> updateUserGroupCache(const User& user, const std::string& opName)
{
   // define a different gid type if we are on Mac vs Linux
   // BSD expects int values, but Linux expects unsigned ints
#ifndef __APPLE__
   typedef gid_t GIDTYPE;
#else
   typedef int GIDTYPE;
#endif

   const std::string& username = user.getUsername();

   LOG_DEBUG_MESSAGE(opName + " group list for user: " + username);

   // get the groups for the user - we start with 100 groups which should be enough for most cases
   // if it is not, resize the vector with the correct amount of groups and try again
   int numGroups = 100;
   std::vector<GIDTYPE> gids(numGroups);
   int lastNumGroups = numGroups;
   while (getgrouplist(username.c_str(), user.getGroupId(), gids.data(), &numGroups) == -1)
   {
      if (numGroups == lastNumGroups)
      {
         LOG_ERROR_MESSAGE("Error retrieving groups for: " + username + " errno: " + std::to_string(errno));
         break; // Continuing on with no groups for this user - should this return an error to the caller?
      }
      gids.resize(numGroups);
      lastNumGroups = numGroups;
   }

   std::vector<GidType> groupIds;
   groupIds.reserve(numGroups);
   for(int i = 0; i < numGroups; i++) {
      groupIds.push_back(static_cast<GidType>(gids[i]));
   }


   UserGroupCache cacheEnt;
   cacheEnt.groupIds = groupIds;
   cacheEnt.cacheTime = std::chrono::steady_clock::now();
   LOCK_MUTEX(s_groupCacheMutex)
   {
      s_userGroupCache[username] = cacheEnt;
   }
   END_LOCK_MUTEX

   LOG_DEBUG_MESSAGE("Finished group list for user: " + user.getUsername() + " with: " + std::to_string(groupIds.size()) + " groups");
   
   return groupIds;
}

bool getGroupFromNameCache(const std::string& name, Group* pGroup, bool* pExpired)
{
   std::chrono::steady_clock::time_point cacheTime;
   Group cacheGroup;

   LOCK_MUTEX(s_groupCacheMutex)
   {
      auto it = s_groupNameCache.find(name);
      if (it == s_groupNameCache.end())
      {
         *pExpired = false;
         return false;
      }
      else
      {
         cacheGroup = it->second.group;
         cacheTime = it->second.cacheTime;
      }
   }
   END_LOCK_MUTEX

   std::chrono::steady_clock::time_point now = std::chrono::steady_clock::now();
   if (cacheTime + s_groupCacheDuration > now)
   {
      *pGroup = cacheGroup;
      *pExpired = false;
      return true;
   }
   else
      *pExpired = true;
   return false;
}

bool getGroupFromIdCache(gid_t gid, Group* pGroup, bool* pExpired)
{
   std::chrono::steady_clock::time_point cacheTime;
   Group cacheGroup;

   LOCK_MUTEX(s_groupCacheMutex)
   {
      auto it = s_groupIdCache.find(gid);
      if (it == s_groupIdCache.end())
      {
         *pExpired = false;
         return false;
      }
      else
      {
         cacheGroup = it->second.group;
         cacheTime = it->second.cacheTime;
      }
   }
   END_LOCK_MUTEX

   std::chrono::steady_clock::time_point now = std::chrono::steady_clock::now();
   if (cacheTime + s_groupCacheDuration > now)
   {
      *pGroup = cacheGroup;
      *pExpired = false;
      return true;
   }
   else
      *pExpired = true;
   return false;
}

bool getGroupIdsFromUserGroupCache(const std::string& username, std::vector<GidType>* pGroupIds, bool* pExpired)
{
   std::chrono::steady_clock::time_point cacheTime;
   std::vector<GidType> cacheGroupIds;

   LOCK_MUTEX(s_groupCacheMutex)
   {
      auto it = s_userGroupCache.find(username);
      if (it == s_userGroupCache.end())
      {
         *pExpired = false;
         return false;
      }
      else
      {
         cacheGroupIds = it->second.groupIds;
         cacheTime = it->second.cacheTime;
      }
   }
   END_LOCK_MUTEX

   std::chrono::steady_clock::time_point now = std::chrono::steady_clock::now();
   if (cacheTime + s_groupCacheDuration > now)
   {
      *pGroupIds = cacheGroupIds;
      *pExpired = false;
      return true;
   }
   else
      *pExpired = true;
   return false;
}

} // anonymous namespace

Error groupFromName(const std::string& name, Group* pGroup)
{
   bool expired;

   if (!getGroupFromNameCache(name, pGroup, &expired))
      return updateGroupCacheByName(name, pGroup, expired ? "Updating" : "Adding");

   return Success();
}

Error groupFromId(gid_t gid, Group* pGroup)
{
   bool expired;

   if (!getGroupFromIdCache(gid, pGroup, &expired))
      return updateGroupCacheById(gid, pGroup, expired ? "Updating" : "Adding");

   return Success();
}

/**
 * @brief Get all user group Ids
 * @param user user to get groups for
 * @return vector containing the groupIds related to user
 */
std::vector<GidType> userGroupIds(const User& user)
{
   const std::string& username = user.getUsername();

   bool expired;
   std::vector<GidType> groupIds;

   if (!getGroupIdsFromUserGroupCache(username, &groupIds, &expired))
      return updateUserGroupCache(user, expired ? "Updating" : "Adding");
 
   return groupIds;
}

Error userGroups(const std::string& userName, std::vector<Group>* pGroups)
{
   User user;
   Error error = getUserFromUsername(userName, user);
   if (error)
   {
      return error;
   }

   auto groupIds = userGroupIds(user);

   int numGroupErrors = 0;
   Error lastError;
   std::string lastGidStr;

   // create group objects for each returned group
   for (const auto& groupId : groupIds)
   {
      Group group;
      lastError = groupFromId(groupId, &group);

      // It's possible there's one group where this api to get the name/members is not available so rather than fail
      // the entire request, create an empty group with a dummy name. This preserves the privs for the user, if not
      // the names, and we do not use the members field of the groups returned by userGroups.
      if (lastError)
      {
         lastGidStr = std::to_string(groupId);
         group.groupId = groupId;
         group.name = "_pwb_group_" + lastGidStr;
         numGroupErrors++;
      }

      // move the group into the vector
      // (less expensive than regular copy as we do not have to copy the entire group member vector)
      pGroups->emplace_back(std::move(group));
   }

   if (numGroupErrors > 0)
      LOG_DEBUG_MESSAGE("Using placeholder names and missing members for: " + std::to_string(numGroupErrors) + " groups for user: " + userName + ". Last group id: " + lastGidStr +
                        " last error: " + lastError.asString());

   return Success();
}

void removeUserFromGroupCache(const std::string& username)
{
   int newSize;
   LOCK_MUTEX(s_groupCacheMutex)
   {
      auto it = s_userGroupCache.find(username);
      if (it != s_userGroupCache.end())
      {
          s_userGroupCache.erase(username);
      }
      newSize = s_userGroupCache.size();
   }
   END_LOCK_MUTEX

   LOG_DEBUG_MESSAGE("Removed user: " + username + " from group cache - (now: " + std::to_string(newSize) + ")");
}

} // namespace group
} // namespace system
} // namespace core
} // namespace rstudio

