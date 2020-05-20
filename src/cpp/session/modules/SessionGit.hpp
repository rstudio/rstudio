/*
 * SessionGit.hpp
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

#ifndef SESSION_GIT_HPP
#define SESSION_GIT_HPP

#include <map>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/json/Json.hpp>

#include "vcs/SessionVCSCore.hpp"

namespace rstudio {
namespace session {

namespace console_process {

class ConsoleProcess;

} // namespace console_process

namespace modules {

namespace git {

extern const char * const kVcsId;

class GitFileDecorationContext : public source_control::FileDecorationContext
{
public:
   GitFileDecorationContext(const core::FilePath& rootDir);
   virtual ~GitFileDecorationContext();
   virtual void decorateFile(const core::FilePath &filePath,
                             core::json::Object *pFileObject);

private:
   source_control::StatusResult vcsStatus_;
   bool fullRefreshRequired_;
};

bool isGitInstalled();
bool isGitEnabled();
bool isWithinGitRoot(const core::FilePath& filePath);

bool isGitDirectory(const core::FilePath& workingDir);

std::string remoteOriginUrl(const core::FilePath& workingDir);

bool isGithubRepository();

core::Error initializeGit(const core::FilePath& workingDir);

core::FilePath detectedGitExePath();

std::string nonPathGitBinDir();

core::Error status(const core::FilePath& dir,
                   source_control::StatusResult* pStatusResult);
core::Error fileStatus(const core::FilePath& filePath,
                       source_control::VCSStatus* pStatus);
core::Error statusToJson(const core::FilePath& path,
                         const source_control::VCSStatus& vcsStatus,
                         core::json::Object* pObject);

core::Error clone(const std::string& url,
                  const std::string dirName,
                  const core::FilePath& parentPath,
                  boost::shared_ptr<console_process::ConsoleProcess>* ppCP);

core::Error initialize();

} // namespace git
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_GIT_HPP
