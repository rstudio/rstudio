/*
 * FileMode.hpp
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


#ifndef CORE_SYSTEM_FILE_MODE_HPP
#define CORE_SYSTEM_FILE_MODE_HPP

#ifdef _WIN32
#error FileMode.hpp is is not supported on Windows
#endif

#include <sys/stat.h>

#include <core/Error.hpp>
#include <core/FilePath.hpp>

namespace core {
namespace system {

enum FileMode
{
   UserReadWriteMode,
   UserReadWriteGroupReadMode,
   EveryoneReadWriteMode,
   EveryoneReadWriteExecuteMode
};

inline Error changeFileMode(const FilePath& filePath, FileMode fileMode)
{
   mode_t mode ;
   switch(fileMode)
   {
      case UserReadWriteMode:
         mode = S_IRUSR | S_IWUSR;
         break;

      case UserReadWriteGroupReadMode:
         mode = S_IRUSR | S_IWUSR | S_IRGRP ;
         break;

      case EveryoneReadWriteMode:
         mode = S_IRUSR | S_IWUSR | S_IRGRP | S_IWGRP | S_IROTH | S_IWOTH;
         break;

      case EveryoneReadWriteExecuteMode:
         mode = S_IRWXU | S_IRWXG | S_IRWXO;
         break;

      default:
         return systemError(ENOTSUP, ERROR_LOCATION);
   }

   // change the mode
   errno = 0;
   if (::chmod(filePath.absolutePath().c_str(), mode) < 0)
      return systemError(errno, ERROR_LOCATION);
   else
      return Success();
}




} // namespace system
} // namespace core

#endif // CORE_SYSTEM_FILE_MODE_HPP
