/*
 * FileInfo.cpp
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

#include <core/FileInfo.hpp>

#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {

FileInfo::FileInfo(const FilePath& filePath, bool isSymlink)
   :  absolutePath_(filePath.getAbsolutePath()),
      isDirectory_(filePath.isDirectory()),
      size_(0),
      lastWriteTime_(0),
      isSymlink_(isSymlink)
{
   if (!isDirectory_ && filePath.exists())
   {
      size_ = filePath.getSize();
      lastWriteTime_ = filePath.getLastWriteTime();
   }
}

FileInfo::FileInfo(const std::string& absolutePath,
                   bool isDirectory,
                   bool isSymlink)
 :    absolutePath_(absolutePath),
      isDirectory_(isDirectory),
      size_(0),
      lastWriteTime_(0),
      isSymlink_(isSymlink)
{
   // some file paths might be constructed with trailing nul bytes; remove those here
   absolutePath_ = absolutePath_.c_str();
}
   
FileInfo::FileInfo(const std::string& absolutePath,
                   bool isDirectory,
                   uintmax_t size,
                   std::time_t lastWriteTime,
                   bool isSymlink)
   :  absolutePath_(absolutePath),
      isDirectory_(isDirectory),
      size_(size),
      lastWriteTime_(lastWriteTime),
      isSymlink_(isSymlink)
{
   // some file paths might be constructed with trailing nul bytes; remove those here
   absolutePath_ = absolutePath_.c_str();
}
   
std::ostream& operator << (std::ostream& stream, const FileInfo& fileInfo)
{
   stream << fileInfo.absolutePath();
   return stream;
}

} // namespace core 
} // namespace rstudio



