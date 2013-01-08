/*
 * FileUtils.cpp
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

#include <core/FileUtils.hpp>
#include <core/FilePath.hpp>

#include <core/system/System.hpp>

namespace core {
namespace file_utils {

FilePath uniqueFilePath(const FilePath& parent, const std::string& prefix)
{
   // try up to 100 times then fallback to a uuid
   for (int i=0; i<100; i++)
   {
      // get a shortened uuid
      std::string shortentedUuid = core::system::generateShortenedUuid();

      // form full path
      FilePath uniqueDir = parent.childPath(prefix + shortentedUuid);

      // return if it doesn't exist
      if (!uniqueDir.exists())
         return uniqueDir;
   }

   // if we didn't succeed then return prefix + uuid
   return parent.childPath(prefix + core::system::generateUuid(false));
}

} // namespace file_utils
} // namespace core
