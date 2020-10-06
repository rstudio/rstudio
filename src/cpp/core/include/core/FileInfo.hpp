/*
 * FileInfo.hpp
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

#ifndef CORE_FILE_INFO_HPP
#define CORE_FILE_INFO_HPP

#include <stdint.h>
#include <ctime>
#include <string.h>

#include <string>
#include <iosfwd>

#include <shared_core/FilePath.hpp>

// TODO: satisfy outselves that it is safe to query for symlink status
// in all cases and eliminate its "optional" semantics

namespace rstudio {
namespace core {

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
   

   // NOTE: this constructor will NOT read symlink info from the passed
   // FilePath object. this is because we want to restrict reading of
   // symlink to status to funcitons that are expressly symlink aware
   // (this is because the behavior of reading symlink status is not
   // fully known and we don't want to make a change underneath our
   // entire codebase which does this universally (note that we've been
   // burned by boost filesystem having nasty beahvior for seemingly
   // innocuous operations before!)
   explicit FileInfo(const FilePath& filePath,
                     bool isSymlink = false);
   
   FileInfo(const std::string& absolutePath,
            bool isDirectory,
            bool isSymlink = false);
   
   FileInfo(const std::string& absolutePath,
            bool isDirectory,
            uintmax_t size,
            std::time_t lastWriteTime,
            bool isSymlink = false);
   
   virtual ~FileInfo()
   {
   }

   // COPYING: via compliler (copyable members)

public:
   bool empty() const { return absolutePath_.empty(); }
   
   // NOTE: because symlink status is optional, it is NOT taken
   // into account for equality tests
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
   std::string absolutePath() const { return absolutePath_; }
   bool isDirectory() const { return isDirectory_; }
   uintmax_t size() const { return size_; }
   std::time_t lastWriteTime() const { return lastWriteTime_; }
   bool isSymlink() const { return isSymlink_; }
   
private:
   std::string absolutePath_;
   bool isDirectory_;
   uintmax_t size_;
   std::time_t lastWriteTime_;
   bool isSymlink_;
};
   
inline int fileInfoPathCompare(const FileInfo& a, const FileInfo& b)
{
   int result = ::strcmp(a.absolutePath().c_str(), b.absolutePath().c_str());

   if (result != 0)
      return result;

   if (a.isDirectory() == b.isDirectory())
      return 0;

   return a.isDirectory() ? -1 : 1;
}

inline bool fileInfoPathLessThan(const FileInfo& a, const FileInfo& b)
{
   return fileInfoPathCompare(a, b) < 0;
}


inline bool fileInfoHasPath(const FileInfo& fileInfo, const std::string& path)
{
   return fileInfo.absolutePath() == path;
}

inline FilePath toFilePath(const FileInfo& fileInfo)
{
   return FilePath(fileInfo.absolutePath());
}

inline FileInfo toFileInfo(const FilePath& filePath)
{
   return FileInfo(filePath);
}

inline std::string fileInfoAbsolutePath(const FileInfo& fileInfo)
{
   return fileInfo.absolutePath();
}

inline bool fileInfoIsDirectory(const FileInfo& fileInfo)
{
   return fileInfo.isDirectory();
}
   
std::ostream& operator << (std::ostream& stream, const FileInfo& fileInfo);

   
} // namespace core 
} // namespace rstudio


#endif // CORE_FILE_INFO_HPP

