/*
 * SessionVCS.cpp
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

#include "SessionVCS.hpp"

#include <core/Exec.hpp>
#include <core/StringUtils.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>
#include <session/SessionConsoleProcess.hpp>
#include <session/prefs/UserPrefs.hpp>

#include "vcs/SessionVCSUtils.hpp"

#include "SessionSVN.hpp"
#include "SessionGit.hpp"

#include "SessionAskPass.hpp"

#include "session-config.h"

#ifdef RSTUDIO_SERVER
#include <core/system/Crypto.hpp>
#endif

using namespace rstudio::core;

namespace rstudio {
namespace session {

namespace {
   const char * const kVcsIdNone = "none";
} // anonymous namespace

namespace module_context {

// if we change the name of one of the VCS systems then there will
// be persisted versions of the name on disk we need to deal with
// migrating. This function can do that migration -- note the initial
// default implementation is to return "none" for unrecognized options
std::string normalizeVcsOverride(const std::string& vcsOverride)
{
   if (vcsOverride == modules::git::kVcsId)
      return vcsOverride;
   else if (vcsOverride == modules::svn::kVcsId)
      return vcsOverride;
   else if (vcsOverride == kVcsIdNone)
      return vcsOverride;
   else
      return "";
}

} // namespace module_context

namespace modules {   
namespace source_control {

namespace {

Error vcsClone(const json::JsonRpcRequest& request,
               json::JsonRpcResponse* pResponse)
{
   std::string vcsName;
   std::string url;
   std::string username;
   std::string dirName;
   std::string parentDir;
   Error error = json::readObjectParam(request.params, 0,
                                       "vcs_name", &vcsName,
                                       "repo_url", &url,
                                       "username", &username,
                                       "directory_name", &dirName,
                                       "parent_path", &parentDir);
   if (error)
      return error;

   ask_pass::setActiveWindow(request.sourceWindow);

   FilePath parentPath = module_context::resolveAliasedPath(parentDir);

   boost::shared_ptr<console_process::ConsoleProcess> pCP;
   if (vcsName == git::kVcsId)
   {
      Error error = git::clone(url,
                               dirName,
                               parentPath,
                               &pCP);
      if (error)
         return error;
   }
   else if (vcsName == svn::kVcsId)
   {
      Error error = svn::checkout(url,
                                  username,
                                  dirName,
                                  parentPath,
                                  &pCP);
      if (error)
         return error;
   }
   else
   {
      return systemError(json::errc::ParamInvalid, ERROR_LOCATION);
   }

   pResponse->setResult(pCP->toJson(console_process::ClientSerialization));

   return Success();
}

class NullFileDecorationContext : public FileDecorationContext
{
   void decorateFile(const FilePath&, json::Object*)
   {
   }
};

} // anonymous namespace

boost::shared_ptr<FileDecorationContext> fileDecorationContext(
      const core::FilePath& rootDir,
      bool implicit)
{
   if (implicit && !prefs::userPrefs().vcsAutorefresh())
   {
      return boost::shared_ptr<FileDecorationContext>(
               new NullFileDecorationContext());
   }
   else if (git::isWithinGitRoot(rootDir))
   {
      return boost::shared_ptr<FileDecorationContext>(
               new git::GitFileDecorationContext(rootDir));
   }
   else if (svn::isSvnEnabled())
   {
      return boost::shared_ptr<FileDecorationContext>(
               new svn::SvnFileDecorationContext(rootDir));
   }
   else
   {
      return boost::shared_ptr<FileDecorationContext>(
               new NullFileDecorationContext());
   }
}

VCS activeVCS()
{
   return git::isGitEnabled() ? VCSGit : VCSNone;
}

std::string activeVCSName()
{
   if (git::isGitEnabled())
      return git::kVcsId;
   else if (svn::isSvnEnabled())
      return svn::kVcsId;
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
   return getTrueHomeDir().completeChildPath(".ssh");
}

void enqueueRefreshEvent()
{
   vcs_utils::enqueueRefreshEvent();
}



core::Error initialize()
{
   git::initialize();
   svn::initialize();

   // http endpoints
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "vcs_clone", vcsClone));
   Error error = initBlock.execute();
   if (error)
      return error;

   // If VCS is disabled, or we're not in a project, do nothing
   const projects::ProjectContext& projContext = projects::projectContext();
   FilePath workingDir = projContext.directory();

   if (!session::options().allowVcs() || !prefs::userPrefs().vcsEnabled() || workingDir.isEmpty())
      return Success();


   // If Git or SVN was explicitly specified, choose it if valid
   projects::RProjectVcsOptions vcsOptions;
   if (projContext.hasProject())
   {
      Error vcsError = projContext.readVcsOptions(&vcsOptions);
      if (vcsError)
         LOG_ERROR(vcsError);
   }

   if (vcsOptions.vcsOverride == kVcsIdNone)
   {
      return Success();
   }
   else if (vcsOptions.vcsOverride == git::kVcsId)
   {
      if (git::isGitInstalled() && git::isGitDirectory(workingDir))
         return git::initializeGit(workingDir);
      return Success();
   }
   else if (vcsOptions.vcsOverride == svn::kVcsId)
   {
      if (svn::isSvnInstalled() && svn::isSvnDirectory(workingDir))
         return svn::initializeSvn(workingDir);
      return Success();
   }

   if (git::isGitInstalled() && git::isGitDirectory(workingDir))
   {
      return git::initializeGit(workingDir);
   }
   else if (svn::isSvnInstalled() && svn::isSvnDirectory(workingDir))
   {
      return svn::initializeSvn(workingDir);
   }
   else
   {
      return Success();  // none specified or detected
   }
}

} // namespace source_control
} // namespace modules
} // namespace session
} // namespace rstudio

namespace rstudio {
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
      context.detectedVcs = git::kVcsId;
   else if (isSvnDirectory)
      context.detectedVcs = svn::kVcsId;
   else
      context.detectedVcs = kVcsIdNone;

   // applicable vcs
   if (gitInstalled)
      context.applicableVcs.push_back(git::kVcsId);
   if (isSvnDirectory)
      context.applicableVcs.push_back(svn::kVcsId);

   // remote urls
   if (isGitDirectory)
      context.gitRemoteOriginUrl = git::remoteOriginUrl(workingDir);
   if (isSvnDirectory)
      context.svnRepositoryRoot = svn::repositoryRoot(workingDir);

   return context;
}

} // namespace module_context
} // namespace session
} // namespace rstudio
