/*
 * PosixNfs.cpp
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

#include <core/system/PosixNfs.hpp>

#include <unistd.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace nfs {
   
// Returns the most up-to-date file statistics available in *pSt, even when
// 'path' resides on NFS. If *pCleared is given, also indicates whether *pSt
// represents the most recent set of attributes.
//
// Returns an error only when attributes cannot be read at all.

core::Error statWithCacheClear(const core::FilePath& path,
                               bool *pCleared, struct stat* pSt)
{

   if (pCleared)
      *pCleared = false;

   // get the file's initial attributes (may be stale)
   if (::stat(path.getAbsolutePath().c_str(), pSt) == -1)
   {
      Error error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", path.getAbsolutePath());
      return error;
   }
   
   // attempt to chown to the same user id--this method of clearing the 
   // cache has the fewest side effects (but isn't guaranteed to succeed for
   // permissions-related reasons)
   if (::chown(
      path.getAbsolutePath().c_str(), pSt->st_uid,
               static_cast<gid_t>(-1)) != 0)
   {
      // failed, fall back on open/closing the file (note that this drops other
      // fcntl locks this process holds for the file so isn't our first choice)
      int fd = ::open(path.getAbsolutePath().c_str(), O_RDONLY);
      if (fd == -1) 
         return core::Success();
      int result = ::close(fd);
      if (result == -1)
         return core::Success();
   }
 
   // we've successfully busted the cache, get the updated attributes
   if (::stat(path.getAbsolutePath().c_str(), pSt) == 0)
   {
      if (pCleared)
         *pCleared = true;
   } 

   return core::Success();
}

} // namespace nfs
} // namespace system
} // namespace core
} // namespace rstudio

