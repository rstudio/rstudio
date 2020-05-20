/*
 * SessionVCSCore.hpp
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
#ifndef SESSION_VCS_CORE_HPP
#define SESSION_VCS_CORE_HPP

#include <vector>
#include <string>
#include <map>

#include <boost/noncopyable.hpp>

#include <shared_core/json/Json.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace session {
namespace modules {
namespace source_control {

// The size threshold at which we warn the user that the thing they are
// requesting might slow down the app and are they sure they want to proceed?
const size_t WARN_SIZE = 200 * 1024;

class VCSStatus
{
public:
   VCSStatus(const std::string& status=std::string())
   {
      status_ = status;
   }

   std::string status() const { return status_; }
   // SVN-specific
   std::string changelist() const { return changelist_; }
private:
   std::string status_;
   std::string changelist_;
};

struct FileWithStatus
{
   VCSStatus status;
   core::FilePath path;
};

class StatusResult
{
public:
   StatusResult(const std::vector<FileWithStatus>& files =
                std::vector<FileWithStatus>())
   {
      files_ = files;
      for (std::vector<FileWithStatus>::iterator it = files_.begin();
           it != files_.end();
           it++)
      {
         filesByPath_[it->path.getAbsolutePath()] = it->status;
      }
   }

   VCSStatus getStatus(const core::FilePath& fileOrDirectory) const;
   std::vector<FileWithStatus> files() const { return files_; }

private:
   std::vector<FileWithStatus> files_;
   std::map<std::string, VCSStatus> filesByPath_;
};


class FileDecorationContext : boost::noncopyable
{
public:
   FileDecorationContext() {}
   virtual ~FileDecorationContext() {}

   virtual void decorateFile(const core::FilePath& filePath,
                             core::json::Object* pFileObject) = 0;
};

} // namespace source_control
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_VCS_CORE_HPP
