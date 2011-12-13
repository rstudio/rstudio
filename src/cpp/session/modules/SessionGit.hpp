/*
 * SessionGit.hpp
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

#ifndef SESSION_GIT_HPP
#define SESSION_GIT_HPP

#include <map>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/json/Json.hpp>

namespace session {
namespace modules {
namespace console_process {

class ConsoleProcess;

} // namespace console_process

namespace git {

extern const char * const kVcsId;

// Must stay in sync with VCSStatus enum in VCSStatus.java
class VCSStatus
{
public:
   VCSStatus(const std::string& status=std::string())
   {
      status_ = status;
   }

   std::string status() const { return status_; }
private:
   std::string status_;
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
         filesByPath_[it->path.absolutePath()] = it->status;
      }
   }

   VCSStatus getStatus(const core::FilePath& fileOrDirectory) const;
   std::vector<FileWithStatus> files() const { return files_; }

private:
   std::vector<FileWithStatus> files_;
   std::map<std::string, VCSStatus> filesByPath_;
};

bool isGitInstalled();
bool isGitEnabled();

bool isGitDirectory(const core::FilePath& workingDir);

std::string remoteOriginUrl(const core::FilePath& workingDir);

core::Error initializeGit(const core::FilePath& workingDir);

core::FilePath detectedGitExePath();

std::string nonPathGitBinDir();

core::Error status(const core::FilePath& dir, StatusResult* pStatusResult);
core::Error fileStatus(const core::FilePath& filePath, VCSStatus* pStatus);
core::Error statusToJson(const core::FilePath& path,
                         const VCSStatus& status,
                         core::json::Object* pObject);

core::Error clone(const std::string& sourceWindow,
                  const std::string& url,
                  const std::string dirName,
                  const core::FilePath& parentPath,
                  boost::shared_ptr<console_process::ConsoleProcess>* ppCP);

core::Error initialize();

} // namespace git
} // namespace modules
} // namespace session

#endif // SESSION_GIT_HPP
