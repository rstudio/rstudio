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

#include "vcs/SessionVCSUtils.hpp"
#include "SessionSVN.hpp"

using namespace core;

namespace session {
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
   else if (svn::isSvnEnabled())
      return "Subversion";
   else
      return std::string();
}

bool isGitInstalled()
{
   return git::isGitInstalled();
}

bool isSvnInstalled()
{
   return svn::isSvnInstalled();
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

FilePath defaultSshKeyDir()
{
   return getTrueHomeDir().childPath(".ssh");
}

// try to detect a terminal on linux desktop
FilePath detectedTerminalPath()
{
#if defined(_WIN32) || defined(__APPLE__)
   return FilePath();
#else
   if (session::options().programMode() == kSessionProgramModeDesktop)
   {
      std::vector<FilePath> terminalPaths;
      terminalPaths.push_back(FilePath("/usr/bin/gnome-terminal"));
      terminalPaths.push_back(FilePath("/usr/bin/konsole"));
      terminalPaths.push_back(FilePath("/usr/bin/xfce4-terminal"));
      terminalPaths.push_back(FilePath("/usr/bin/xterm"));

      BOOST_FOREACH(const FilePath& terminalPath, terminalPaths)
      {
         if (terminalPath.exists())
            return terminalPath;
      }

      return FilePath();
   }
   else
   {
      return FilePath();
   }
#endif
}

void enqueueRefreshEvent()
{
   vcs_utils::enqueueRefreshEvent();
}

core::Error initialize()
{
   git::initialize();
   svn::initialize();

   // If VCS is disabled, or we're not in a project, do nothing
   const projects::ProjectContext& projContext = projects::projectContext();
   FilePath workingDir = projContext.directory();

   if (!userSettings().vcsEnabled() || workingDir.empty())
      return Success();


   // If Git or SVN was explicitly specified, choose it if valid
   projects::RProjectVcsOptions vcsOptions;
   if (projContext.hasProject())
   {
      Error vcsError = projContext.readVcsOptions(&vcsOptions);
      if (vcsError)
         LOG_ERROR(vcsError);
   }

   if (vcsOptions.vcsOverride == "none")
   {
      return Success();
   }
   else if (vcsOptions.vcsOverride == "git")
   {
      if (git::isGitInstalled() && git::isGitDirectory(workingDir))
         return git::initializeGit(workingDir);
      return Success();
   }
#ifdef SUBVERSION
   else if (vcsOptions.vcsOverride == "svn")
   {
      if (svn::isSvnInstalled() && svn::isSvnDirectory(workingDir))
         return svn::initializeSvn(workingDir);
      return Success();
   }
#endif

   if (git::isGitInstalled() && git::isGitDirectory(workingDir))
      return git::initializeGit(workingDir);
#ifdef SUBVERSION
   else if (svn::isSvnInstalled() && svn::isSvnDirectory(workingDir))
      return svn::initializeSvn(workingDir);
#endif
   else
      return Success();  // none specified or detected
}

} // namespace source_control
} // namespace modules
} // namespace session

namespace session {
namespace module_context {

VcsContext vcsContext(const FilePath& workingDir)
{
   using namespace session::modules;
   using namespace session::modules::source_control;

   // inspect current vcs state (underlying functions execute child
   // processes so we want to be sure to only call them once)
   bool gitInstalled = isGitInstalled();
   bool isGitDirectory = gitInstalled && git::isGitDirectory(workingDir);
   bool svnInstalled = isSvnInstalled();
   bool isSvnDirectory = svnInstalled && svn::isSvnDirectory(workingDir);

   // detected vcs
   VcsContext context;
   if (isGitDirectory)
      context.detectedVcs = "git";
   else if (isSvnDirectory)
      context.detectedVcs = "svn";
   else
      context.detectedVcs = "none";

   // applicable vcs
   if (gitInstalled)
      context.applicableVcs.push_back("git");
   if (isSvnDirectory)
      context.applicableVcs.push_back("svn");

   // remote urls
   if (isGitDirectory)
      context.gitRemoteOriginUrl = git::remoteOriginUrl(workingDir);
   if (isSvnDirectory)
      context.svnRepositoryRoot = svn::repositoryRoot(workingDir);

   return context;
}

} // namespace module_context
} // namespace session
