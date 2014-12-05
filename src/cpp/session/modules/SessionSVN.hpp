/*
 * SessionSVN.hpp
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

#ifndef SESSION_SVN_HPP
#define SESSION_SVN_HPP

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include "vcs/SessionVCSCore.hpp"

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
   SvnFileDecorationContext(const rscore::FilePath& rootDir);
   virtual ~SvnFileDecorationContext();
   void decorateFile(const rscore::FilePath& filePath,
                     rscore::json::Object* pFileObject);
private:
   source_control::StatusResult vcsResult_;
};

// Returns true if Subversion install is detected
bool isSvnInstalled();

// Returns true if the working directory is in a Subversion tree
bool isSvnDirectory(const rscore::FilePath& workingDir);

std::string repositoryRoot(const rscore::FilePath& workingDir);

bool isSvnEnabled();

rscore::FilePath detectedSvnExePath();

std::string nonPathSvnBinDir();

rscore::Error checkout(const std::string& url,
                     const std::string& username,
                     const std::string dirName,
                     const rscore::FilePath& parentPath,
                     boost::shared_ptr<console_process::ConsoleProcess>* ppCP);

rscore::Error initialize();

// Initialize SVN with the given working directory
rscore::Error initializeSvn(const rscore::FilePath& workingDir);

} // namespace svn
} // namespace modules
} // namespace session

#endif
