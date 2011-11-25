/*
 * SessionVCS.hpp
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

#ifndef SESSION_VCS_HPP
#define SESSION_VCS_HPP

#include <core/Error.hpp>
#include <core/FilePath.hpp>

#include <session/SessionUserSettings.hpp>

#include "SessionGit.hpp"

namespace session {
namespace modules {
namespace source_control {

enum VCS
{
   VCSNone,
   VCSGit,
   VCSSubversion
};

VCS activeVCS();
std::string activeVCSName();
bool isGitInstalled();
bool isSvnInstalled();

// default directory for reading/writing ssh keys
core::FilePath defaultSshKeyDir();

void enqueueRefreshEvent();

core::Error initialize();

} // namespace source_control
} // namespace modules
} // namesapce session

#endif // SESSION_VCS_HPP
