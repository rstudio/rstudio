/*
 * PosixGroup.cpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include <core/system/PosixGroup.hpp>

#include <pwd.h>
#include <grp.h>
#include <unistd.h>

#include <iostream>

#include <boost/lexical_cast.hpp>

#include <core/Error.hpp>
#include <core/Log.hpp>
#include <core/Exec.hpp>
#include <core/SafeConvert.hpp>
#include <core/system/System.hpp>

namespace rstudio {
namespace core {
namespace system {
namespace group {
   
namespace {

const int kNotFoundError = EACCES;

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
      buffer.reserve(buffSize);

      // attempt the read
      result = getGroup(value, &grp, &(buffer[0]), buffSize, &temp);

      // if we fail, double the buffer prior to retry
      if (result == ERANGE)
         buffSize *= 2;

   } while (result == ERANGE);

   if (temp == NULL)
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

} // anonymous namespace


Error groupFromName(const std::string& name, Group* pGroup)
{
   return groupFrom<const char *>(::getgrnam_r, name.c_str(), pGroup);
}

Error groupFromId(gid_t gid, Group* pGroup)
{
   return groupFrom<gid_t>(::getgrgid_r, gid, pGroup);
}

} // namespace group
} // namespace system
} // namespace core
} // namespace rstudio

