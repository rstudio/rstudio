/*
 * SessionVCS.hpp
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

#ifndef SESSION_VCS_HPP
#define SESSION_VCS_HPP

#include <boost/shared_ptr.hpp>

#include <core/json/Json.hpp>
#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <session/SessionUserSettings.hpp>

#include "vcs/SessionVCSCore.hpp"

#include "SessionGit.hpp"

namespace rstudio {
namespace session {
namespace modules {
namespace source_control {

enum VCS
{
   VCSNone,
   VCSGit,
   VCSSubversion
};

boost::shared_ptr<FileDecorationContext> fileDecorationContext(
                                            const core::FilePath& rootDir);

VCS activeVCS();
std::string activeVCSName();
bool isGitInstalled();
bool isSvnInstalled();

// default directory for reading/writing ssh keys
core::FilePath defaultSshKeyDir();

void enqueueRefreshEvent();

core::Error fileStatus(const core::FilePath& filePath,
                       source_control::VCSStatus* pStatus);

core::Error initialize();

} // namespace source_control
} // namespace modules
} // namespace session
} // namespace rstudio

#endif // SESSION_VCS_HPP
