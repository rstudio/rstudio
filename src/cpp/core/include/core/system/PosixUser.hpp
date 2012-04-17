/*
 * PosixUser.hpp
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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

namespace core {
   class Error;
   class FilePath;
}

namespace core {
namespace system {
namespace user {

struct UserIdentity
{
   uid_t userId;
   gid_t groupId;
};

UserIdentity currentUserIdentity();
   
core::Error socketPeerIdentity(int socket, UserIdentity* pIdentity);
   
} // namespace user
} // namespace system
} // namespace core

#endif // CORE_SYSTEM_POSIX_USER_HPP

