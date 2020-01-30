/*
 * FileMode.hpp
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


#ifndef CORE_SYSTEM_FILE_MODE_HPP
#define CORE_SYSTEM_FILE_MODE_HPP

#ifdef _WIN32
#error FileMode.hpp is is not supported on Windows
#endif

#include <sys/stat.h>
#include <sys/unistd.h>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>

namespace rstudio {
namespace core {
namespace system {

inline Error isFileReadable(const FilePath& filePath, bool* pReadable)
{
   int result = ::access(filePath.getAbsolutePath().c_str(), R_OK);
   if (result == 0) 
   {
      // user has access
      *pReadable = true;
   }
   else if (errno == EACCES)
   {
      // this error is expected when the user doesn't have access to the path
      *pReadable = false;
   }
   else 
   {
      // some other error (unexpected)
      return systemError(errno, ERROR_LOCATION);
   }
   return Success();
}

inline Error isFileWriteable(const FilePath& filePath, bool* pWriteable)
{
   int result = ::access(filePath.getAbsolutePath().c_str(), W_OK);
   if (result == 0)
   {
      // user has access
      *pWriteable = true;
   }
   else if (errno == EACCES)
   {
      *pWriteable = false;
   }
   else
   {
      return systemError(errno, ERROR_LOCATION);
   }
   return Success();
}

} // namespace system
} // namespace core
} // namespace rstudio

#endif // CORE_SYSTEM_FILE_MODE_HPP
