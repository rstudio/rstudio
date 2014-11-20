/*
 * PosixUser.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

#ifndef CORE_SYSTEM_POSIX_USER_HPP
#define CORE_SYSTEM_POSIX_USER_HPP

#include <string>
#include <unistd.h>

// typdefs (in case we need indirection for porting)
typedef uid_t  UidType;
typedef gid_t  GidType;

namespace rscore {
   class Error;
   class FilePath;
}

namespace rscore {
namespace system {
namespace user {

struct UserIdentity
{
   UidType userId;
   GidType groupId;
};

UserIdentity currentUserIdentity();
   
rscore::Error socketPeerIdentity(int socket, UserIdentity* pIdentity);

struct User
{
   UidType userId;
   GidType groupId;
   std::string username;
   std::string homeDirectory;
};

rscore::Error currentUser(User* pUser);

bool exists(const std::string& username);
rscore::Error userFromUsername(const std::string& username, User* pUser);
rscore::Error userFromId(UidType uid, User* pUser);

   
} // namespace user
} // namespace system
} // namespace rscore

#endif // CORE_SYSTEM_POSIX_USER_HPP

