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
   GitFileDecorationContext(const rstudiocore::FilePath& rootDir);
   virtual ~GitFileDecorationContext();
   virtual void decorateFile(const rstudiocore::FilePath &filePath,
                             rstudiocore::json::Object *pFileObject);

private:
   source_control::StatusResult vcsStatus_;
   bool fullRefreshRequired_;
};

bool isGitInstalled();
bool isGitEnabled();

bool isGitDirectory(const rstudiocore::FilePath& workingDir);

std::string remoteOriginUrl(const rstudiocore::FilePath& workingDir);

bool isGithubRepository();

rstudiocore::Error initializeGit(const rstudiocore::FilePath& workingDir);

rstudiocore::FilePath detectedGitExePath();

std::string nonPathGitBinDir();

rstudiocore::Error status(const rstudiocore::FilePath& dir,
                   source_control::StatusResult* pStatusResult);
rstudiocore::Error fileStatus(const rstudiocore::FilePath& filePath,
                       source_control::VCSStatus* pStatus);
rstudiocore::Error statusToJson(const rstudiocore::FilePath& path,
                         const source_control::VCSStatus& status,
                         rstudiocore::json::Object* pObject);

rstudiocore::Error clone(const std::string& url,
                  const std::string dirName,
                  const rstudiocore::FilePath& parentPath,
                  boost::shared_ptr<console_process::ConsoleProcess>* ppCP);

rstudiocore::Error initialize();

} // namespace git
} // namespace modules
} // namespace session

#endif // SESSION_GIT_HPP
