/*
 * FileInfo.hpp
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

#ifndef CORE_FILE_INFO_HPP
#define CORE_FILE_INFO_HPP

#include <stdint.h>
#include <ctime>

#include <string>
#include <iosfwd>

namespace core {

class FilePath;

class FileInfo
{
public:
   FileInfo()
      : absolutePath_(), 
        isDirectory_(false), 
        size_(0), 
        lastWriteTime_(0)
   {
   }
   
   explicit FileInfo(const FilePath& filePath) ;
   
   FileInfo(const std::string& absolutePath, bool isDirectory);
   
   FileInfo(const std::string& absolutePath,
            bool isDirectory,
            uintmax_t size,
            std::time_t lastWriteTime);
   
   virtual ~FileInfo()
   {
   }

   // COPYING: via compliler (copyable members)

public:
   bool empty() const { return absolutePath_.empty(); }
   
   bool operator==(const FileInfo& other) const
   {
      return absolutePath_ == other.absolutePath_ &&
             isDirectory_ == other.isDirectory_ &&
             size_ == other.size_ &&
             lastWriteTime_ == other.lastWriteTime_;
   }
   
   bool operator!=(const FileInfo& other) const
   {
      return !(*this == other); 
   }
   
public:
   std::string absolutePath() const { return absolutePath_.c_str(); }
   bool isDirectory() const { return isDirectory_; }
   uintmax_t size() const { return size_; }
   std::time_t lastWriteTime() const { return lastWriteTime_; }
   
private:
   std::string absolutePath_;
   bool isDirectory_;
   uintmax_t size_;
   std::time_t lastWriteTime_;
};
   
inline bool compareFileInfoPaths(const FileInfo& fileInfo1, 
                                 const FileInfo& fileInfo2)
{
   return fileInfo1.absolutePath() < fileInfo2.absolutePath();
}

inline bool fileInfoHasPath(const FileInfo& fileInfo, const std::string& path)
{
   return fileInfo.absolutePath() == path;
}
   
std::ostream& operator << (std::ostream& stream, const FileInfo& fileInfo) ;

   
} // namespace core 


#endif // CORE_FILE_INFO_HPP

