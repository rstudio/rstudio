/*
 * User.hpp
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

#ifndef SERVER_UTIL_SYSTEM_USER_HPP
#define SERVER_UTIL_SYSTEM_USER_HPP

#include <string>

#include <core/Error.hpp>

// typdefs (in case we need indirection for porting)
typedef uid_t  UidType;
typedef gid_t  GidType;

namespace server {
namespace util {
namespace system {
namespace user {

struct User
{
   UidType userId;
   GidType groupId;
   std::string username;
   std::string homeDirectory;
};

core::Error currentUser(User* pUser);

bool exists(const std::string& username);
core::Error userFromUsername(const std::string& username, User* pUser);
core::Error userFromId(UidType uid, User* pUser);


} // namespace user
} // namespace system
} // namespace util
} // namespace server

#endif // SERVER_UTIL_SYSTEM_USER_HPP

