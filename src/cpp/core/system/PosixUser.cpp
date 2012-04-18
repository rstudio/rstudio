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

#include <core/system/PosixUser.hpp>

#include <pwd.h>
#include <grp.h>
#include <unistd.h>

#include <sys/socket.h>

#include <iostream>

#include <boost/lexical_cast.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/FilePath.hpp>
#include <core/system/System.hpp>

#include "config.h"

namespace core {
namespace system {
namespace user {
   

UserIdentity currentUserIdentity()
{
   UserIdentity userIdentity;
   userIdentity.userId = ::geteuid();
   userIdentity.groupId = ::getegid();
   return userIdentity;
}

#if defined(HAVE_SO_PEERCRED)

Error socketPeerIdentity(int socket, UserIdentity* pIdentity)
{
   struct ucred cred;
   socklen_t length = sizeof(struct ucred);
   if (::getsockopt(socket, SOL_SOCKET, SO_PEERCRED, &cred, &length) < 0)
      return systemError(errno, ERROR_LOCATION);
      
   pIdentity->userId = cred.uid;
   pIdentity->groupId = cred.gid;
   return Success();
}

#elif defined(HAVE_GETPEEREID)
   
Error socketPeerIdentity(int socket, UserIdentity* pIdentity)
{
   uid_t uid;
   gid_t gid;
   if (::getpeereid(socket, &uid, &gid) < 0)
      return systemError(errno, ERROR_LOCATION);
   
   pIdentity->userId = uid;
   pIdentity->groupId = gid;
   return Success();
}

#else
   #error "No way to discover socket peer identity found on this platform"
#endif


} // namespace user
} // namespace system
} // namespace core

