/*
 * SessionGit.hpp
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

#ifndef SESSION_GIT_HPP
#define SESSION_GIT_HPP

#include <map>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/json/Json.hpp>

#include "vcs/SessionVCSCore.hpp"

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
   GitFileDecorationContext(const rscore::FilePath& rootDir);
   virtual ~GitFileDecorationContext();
   virtual void decorateFile(const rscore::FilePath &filePath,
                             rscore::json::Object *pFileObject);

private:
   source_control::StatusResult vcsStatus_;
   bool fullRefreshRequired_;
};

bool isGitInstalled();
bool isGitEnabled();

bool isGitDirectory(const rscore::FilePath& workingDir);

std::string remoteOriginUrl(const rscore::FilePath& workingDir);

bool isGithubRepository();

rscore::Error initializeGit(const rscore::FilePath& workingDir);

rscore::FilePath detectedGitExePath();

std::string nonPathGitBinDir();

rscore::Error status(const rscore::FilePath& dir,
                   source_control::StatusResult* pStatusResult);
rscore::Error fileStatus(const rscore::FilePath& filePath,
                       source_control::VCSStatus* pStatus);
rscore::Error statusToJson(const rscore::FilePath& path,
                         const source_control::VCSStatus& status,
                         rscore::json::Object* pObject);

rscore::Error clone(const std::string& url,
                  const std::string dirName,
                  const rscore::FilePath& parentPath,
                  boost::shared_ptr<console_process::ConsoleProcess>* ppCP);

rscore::Error initialize();

} // namespace git
} // namespace modules
} // namespace session

#endif // SESSION_GIT_HPP
