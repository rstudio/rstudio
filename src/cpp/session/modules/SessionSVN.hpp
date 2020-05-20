/*
 * SessionSVN.hpp
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

#ifndef SESSION_SVN_HPP
#define SESSION_SVN_HPP

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

#include "vcs/SessionVCSCore.hpp"

namespace rstudio {
namespace session {

namespace console_process {

class ConsoleProcess;

} // namespace console_process

namespace modules {

namespace svn {

extern const char * const kVcsId;

class SvnFileDecorationContext : public source_control::FileDecorationContext
{
public:
   SvnFileDecorationContext(const core::FilePath& rootDir);
   virtual ~SvnFileDecorationContext();
   void decorateFile(const core::FilePath& filePath,
                     core::json::Object* pFileObject);
private:
   source_control::StatusResult vcsResult_;
};

// Returns true if Subversion install is detected
bool isSvnInstalled();

// Returns true if the working directory is in a Subversion tree
bool isSvnDirectory(const core::FilePath& workingDir);

std::string repositoryRoot(const core::FilePath& workingDir);

bool isSvnEnabled();

core::FilePath detectedSvnExePath();

std::string nonPathSvnBinDir();

core::Error checkout(const std::string& url,
                     const std::string& username,
                     const std::string dirName,
                     const core::FilePath& parentPath,
                     boost::shared_ptr<console_process::ConsoleProcess>* ppCP);

core::Error initialize();

// Initialize SVN with the given working directory
core::Error initializeSvn(const core::FilePath& workingDir);

} // namespace svn
} // namespace modules
} // namespace session
} // namespace rstudio

#endif
