/*
 * PosixUser.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include <server/util/system/User.hpp>

#include <pwd.h>
#include <grp.h>
#include <unistd.h>


#include <boost/function.hpp>
#include <boost/lexical_cast.hpp>

#include <core/Log.hpp>
#include <core/Error.hpp>

using namespace core;

namespace server {
namespace util {
namespace system {
namespace user {

namespace {

const int kNotFoundError = EACCES;

// re-use scaffolding for calls to getpwnam_r and getpwuid_r
template <typename T>
Error userFrom(const boost::function<int(
                 T, struct passwd*, char*, size_t, struct passwd**)>& getPasswd,
               T value,
               User* pUser)
{
   struct passwd pwd;
   struct passwd* ptrPwd = &pwd;
   struct passwd* tempPtrPwd ;
   int buffSize = ::sysconf(_SC_GETPW_R_SIZE_MAX);
   if (buffSize == -1)
      buffSize = 4096; // some systems return -1, be conservative!
   std::vector<char> buffer(buffSize);
   int result = getPasswd(value, ptrPwd, &(buffer[0]), buffSize, &tempPtrPwd) ;
   if (tempPtrPwd == NULL)
   {
      if (result == 0) // will happen if user is simply not found
         result = kNotFoundError;
      Error error = systemError(result, ERROR_LOCATION);
      error.addProperty("user-value", boost::lexical_cast<std::string>(value));
      return error;
   }
   else
   {
      pUser->userId = pwd.pw_uid;
      pUser->groupId = pwd.pw_gid;
      pUser->username = pwd.pw_name;
      pUser->homeDirectory = pwd.pw_dir;
      return Success();
   }
}

} // anonymous namespace

Error currentUser(User* pUser)
{
   return userFromId(::geteuid(), pUser);
}

bool exists(const std::string& username)
{
   User user;
   Error error = userFromUsername(username, &user);
   return !error;
}

Error userFromUsername(const std::string& username, User* pUser)
{
   return userFrom<const char *>(::getpwnam_r, username.c_str(), pUser);
}

Error userFromId(uid_t uid, User* pUser)
{
   return userFrom<uid_t>(::getpwuid_r, uid, pUser);
}


} // namespace user
} // namespace system
} // namespace util
} // namespace server

