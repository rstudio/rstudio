/*
 * FileInfo.cpp
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

#include <core/FileInfo.hpp>

#include <core/FilePath.hpp>

namespace core {

FileInfo::FileInfo(const FilePath& filePath)
   :  absolutePath_(filePath.absolutePath()),
      isDirectory_(filePath.isDirectory()),
      size_(0),
      lastWriteTime_(0)
{
   if (!isDirectory_)
   {
      size_ = filePath.size();
      lastWriteTime_ = filePath.lastWriteTime();
   }
}

FileInfo::FileInfo(const std::string& absolutePath, bool isDirectory)
 :    absolutePath_(absolutePath),
      isDirectory_(isDirectory),
      size_(0),
      lastWriteTime_(0)
{
}
   
FileInfo::FileInfo(const std::string& absolutePath,
                   bool isDirectory,
                   uintmax_t size,
                   std::time_t lastWriteTime)
   :  absolutePath_(absolutePath),
      isDirectory_(isDirectory),
      size_(size),
      lastWriteTime_(lastWriteTime)
{
}
   
std::ostream& operator << (std::ostream& stream, const FileInfo& fileInfo)
{
   stream << fileInfo.absolutePath();
   return stream ;
}
   
   

   
} // namespace core 



