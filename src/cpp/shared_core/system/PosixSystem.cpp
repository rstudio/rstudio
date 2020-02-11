/*
 * PosixSystem.cpp
 *
 * Copyright (C) 2009-20 by RStudio, PBC
 *
 * Unless you have received this program directly from RStudio pursuant to the terms of a commercial license agreement
 * with RStudio, then this program is licensed to you under the following terms:
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

#include <shared_core/system/PosixSystem.hpp>

#include <csignal>
#include <grp.h>
#include <memory.h>
#include <pwd.h>
#include <sys/prctl.h>

#include <shared_core/Error.hpp>
#include <shared_core/system/User.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace posix {

Error enableCoreDumps()
{
#ifndef __APPLE__
   int res = ::prctl(PR_SET_DUMPABLE, 1);
   if (res == -1)
      return systemError(errno, ERROR_LOCATION);
#endif

   return Success();
}

Error ignoreSignal(int in_signal)
{
   struct sigaction sa;
   ::memset(&sa, 0, sizeof(sa));

   sa.sa_handler = SIG_IGN;
   int result = ::sigaction(in_signal, &sa, nullptr);
   if (result != 0)
   {
      Error error = systemError(result, ERROR_LOCATION);
      error.addProperty("signal", in_signal);
      return error;
   }

   return Success();
}

bool realUserIsRoot()
{
   return ::getuid() == 0;
}

Error restoreRoot()
{
   // Reset error state.
   errno = 0;

   // Change the effective user to root.
   if (::seteuid(0) < 0)
      return systemError(errno, ERROR_LOCATION);
   // Verify
   if (::geteuid() != 0)
      return systemError(EACCES, ERROR_LOCATION);

   // Get user info to use in group calls
   struct passwd* pPrivPasswd = ::getpwuid(0);
   if (pPrivPasswd == nullptr)
      return systemError(errno, ERROR_LOCATION);

   // Supplemental groups
   if (::initgroups(pPrivPasswd->pw_name, pPrivPasswd->pw_gid) < 0)
      return systemError(errno, ERROR_LOCATION);

   // Set effective group
   if (::setegid(pPrivPasswd->pw_gid) < 0)
      return systemError(errno, ERROR_LOCATION);
   // Verify
   if (::getegid() != pPrivPasswd->pw_gid)
      return systemError(EACCES, ERROR_LOCATION);

   return Success();
}

Error temporarilyDropPriv(const system::User& in_user)
{
   // clear error state
   errno = 0;

   // init supplemental group list
   // NOTE: if porting to CYGWIN may need to call getgroups/setgroups
   // after initgroups -- more research required to confirm
   if (::initgroups(in_user.getUsername().c_str(), in_user.getGroupId()) < 0)
      return systemError(errno, ERROR_LOCATION);

   // set group and verify
   if (::setresgid(-1, in_user.getGroupId(), ::getegid()) < 0)
      return systemError(errno, ERROR_LOCATION);
   if (::getegid() != in_user.getGroupId())
      return systemError(EACCES, ERROR_LOCATION);

   // set user and verify
   if (::setresuid(-1, in_user.getUserId(), ::geteuid()) < 0)
      return systemError(errno, ERROR_LOCATION);
   if (::geteuid() != in_user.getUserId())
      return systemError(EACCES, ERROR_LOCATION);

   // success
   return Success();
}

} // namespace posix
} // namespace system
} // namespace core
} // namespace rstudio
