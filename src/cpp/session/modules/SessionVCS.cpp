/*
 * SessionVCS.cpp
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

#include "SessionVCS.hpp"

#include <boost/foreach.hpp>

#include <core/StringUtils.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

using namespace core;

namespace session {
namespace module_context {

FilePath verifiedDefaultSshKeyPath()
{
   return session::modules::source_control::verifiedDefaultSshKeyPath();
}

std::string detectedVcs(const FilePath& workingDir)
{
   return session::modules::source_control::detectedVcs(workingDir);
}

} // namespace module_context


namespace modules {
namespace source_control {

VCS activeVCS()
{
   return git::isGitEnabled() ? VCSGit : VCSNone;
}

std::string activeVCSName()
{
   if (git::isGitEnabled())
      return "Git";
   else
      return std::string();
}

bool isGitInstalled()
{
   return git::isGitInstalled();
}

bool isSvnInstalled()
{
   if (!userSettings().vcsEnabled())
      return false;

   // TODO
   return false;
}

FilePath getTrueHomeDir()
{
#if _WIN32
   // On Windows, R's idea of "$HOME" is not, by default, the same as
   // $USERPROFILE, which is what we want for ssh purposes
   return FilePath(string_utils::systemToUtf8(core::system::getenv("USERPROFILE")));
#else
   return FilePath(string_utils::systemToUtf8(core::system::getenv("HOME")));
#endif
}


// in RStudio Server mode ensure that there are no group or other permissions
// set for the ssh key (ssh will fail if there are). we do this automagically
// in server mode because often users will upload their ssh keys into the
// .ssh folder (resulting in default permissions) and then still be in an
// inoperable state. the users could figure out how to do system("chmod ...")
// but many will probably end up getting stimied before trying that. we don't
// consider this intrusive because we are resetting the permissions to what
// they would be if the user called ssh-keygen directory and we can't think
// of any good reason why you'd want an ssh key with incorrect/inoperable
// permissions set on it
void ensureCorrectPermissions(const FilePath& sshKeyPath)
{
#ifdef RSTUDIO_SERVER
   const char * path = sshKeyPath.absolutePath().c_str();
   struct stat st;
   if (::stat(path, &st) == -1)
   {
      LOG_ERROR(systemError(errno, ERROR_LOCATION));
      return;
   }

   // check if there are any permissions for group or other defined. if
   // there are then remove them
   mode_t mode = st.st_mode;
   if (mode & S_IRWXG || mode & S_IRWXO)
   {
      mode &= ~S_IRWXG;
      mode &= ~S_IRWXO;
      core::system::safePosixCall<int>(boost::bind(::chmod, path, mode),
                                       ERROR_LOCATION);
   }
#endif
}


// get the current active ssh key path -- first checks for a project
// specific override then falls back to the verified default
FilePath verifiedSshKeyPath()
{
   projects::RProjectVcsOptions vcsOptions;
   Error error = projects::projectContext().readVcsOptions(&vcsOptions);
   if (error)
      LOG_ERROR(error);
   if (!vcsOptions.sshKeyPathOverride.empty())
   {
      FilePath keyPath = module_context::resolveAliasedPath(
                                             vcsOptions.sshKeyPathOverride);
      ensureCorrectPermissions(keyPath);
      return keyPath;
   }
   else
   {
      return source_control::verifiedDefaultSshKeyPath();
   }
}

FilePath verifiedDefaultSshKeyPath()
{
   // if there is user override first try that -- if the override is
   // specified but doesn't exit then advance to auto-resolution logic
   std::string sskKeyPathSetting = userSettings().sshKeyPath();
   FilePath sshKeyPath;
   if (!sskKeyPathSetting.empty())
   {
      sshKeyPath = module_context::resolveAliasedPath(sskKeyPathSetting);
      if (!sshKeyPath.exists())
         sshKeyPath = FilePath();
   }

   // if there isn't a valid user specified default then scan known locations
   if (sshKeyPath.empty())
   {
      FilePath sshKeyDir = defaultSshKeyDir();
      std::vector<FilePath> candidatePaths;
      candidatePaths.push_back(sshKeyDir.childPath("id_rsa"));
      candidatePaths.push_back(sshKeyDir.childPath("id_dsa"));
      candidatePaths.push_back(sshKeyDir.childPath("identity"));
      BOOST_FOREACH(const FilePath& path, candidatePaths)
      {
         if (path.exists())
         {
            sshKeyPath = path;
            break;
         }
      }
   }

   // ensure permissions if we have a path to return
   if (!sshKeyPath.empty())
   {
      ensureCorrectPermissions(sshKeyPath);
      return sshKeyPath;
   }
   else
   {
      return FilePath();
   }
}


FilePath defaultSshKeyDir()
{
   return getTrueHomeDir().childPath(".ssh");
}

std::string detectedVcs(const core::FilePath& workingDir)
{
   return git::detectedVcs(workingDir);
}

core::Error initialize()
{
   return git::initialize();
}

} // namespace source_control
} // namespace modules

} // namespace session
