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

#include <core/Exec.hpp>
#include <core/StringUtils.hpp>
#include <core/system/Environment.hpp>
#include <core/system/Process.hpp>
#include <core/system/ShellUtils.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/projects/SessionProjects.hpp>

#include "vcs/SessionVCSUtils.hpp"

#include "SessionSVN.hpp"
#include "SessionGit.hpp"

#include "SessionConsoleProcess.hpp"

#include "config.h"

#ifdef RSTUDIO_SERVER
#include <core/system/Crypto.hpp>
#endif

using namespace core;

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

   setAskPassWindow(request.sourceWindow);

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

   pResponse->setResult(pCP->toJson());

   return Success();
}

class NullFileDecorationContext : public FileDecorationContext
{
   void decorateFile(const FilePath&, json::Object*) const
   {
   }
};

} // anonymous namespace

boost::shared_ptr<FileDecorationContext> fileDecorationContext(
                                            const core::FilePath& rootDir)
{
   if (git::isGitEnabled())
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
   return getTrueHomeDir().childPath(".ssh");
}

void enqueueRefreshEvent()
{
   vcs_utils::enqueueRefreshEvent();
}

namespace {

std::string s_askPassWindow;
module_context::WaitForMethodFunction s_waitForAskPass;

void onClientInit()
{
   s_askPassWindow = "";
}

} // anonymous namespace

void setAskPassWindow(const std::string& window)
{
   s_askPassWindow = window;
}

void setAskPassWindow(const json::JsonRpcRequest& request)
{
   setAskPassWindow(request.sourceWindow);
}

Error askForPassword(const std::string& prompt,
                     const std::string& rememberPrompt,
                     PasswordInput* pInput)
{
   json::Object payload;
   payload["prompt"] = prompt;
   payload["remember_prompt"] = rememberPrompt;
   payload["window"] = s_askPassWindow;
   ClientEvent askPassEvent(client_events::kAskPass, payload);

   // wait for method
   core::json::JsonRpcRequest request;
   if (!s_waitForAskPass(&request, askPassEvent))
   {
      return systemError(boost::system::errc::operation_canceled,
                         ERROR_LOCATION);
   }

   // read params
   json::Value value;
   bool remember;
   Error error = json::readParams(request.params, &value, &remember);
   if (error)
      return error;

   // null passphrase means dialog was cancelled
   if (!json::isType<std::string>(value))
   {
      pInput->cancelled = true;
      return Success();
   }

   // read inputs
   pInput->remember = remember;
   pInput->password = value.get_value<std::string>();

   // decrypt if necessary
#ifdef RSTUDIO_SERVER
   if (options().programMode() == kSessionProgramModeServer)
   {
      // In server mode, passphrases are encrypted
      error = core::system::crypto::rsaPrivateDecrypt(
                                             pInput->password,
                                             &pInput->password);
      if (error)
         return error;
   }
#endif

   return Success();
}

core::Error initialize()
{
   git::initialize();
   svn::initialize();

   module_context::events().onClientInit.connect(onClientInit);

   // register waitForMethod handler
   s_waitForAskPass = module_context::registerWaitForMethod(
                                                "askpass_completed");


   // http endpoints
   using boost::bind;
   using namespace module_context;
   ExecBlock initBlock ;
   initBlock.addFunctions()
      (bind(registerRpcMethod, "vcs_clone", vcsClone));
   Error error = initBlock.execute();
   if (error)
      return error;

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
   else if (userSettings().svnEnabled() &&
            (vcsOptions.vcsOverride == svn::kVcsId))
   {
      if (svn::isSvnInstalled() && svn::isSvnDirectory(workingDir))
         return svn::initializeSvn(workingDir);
      return Success();
   }

   if (git::isGitInstalled() && git::isGitDirectory(workingDir))
   {
      return git::initializeGit(workingDir);
   }
   else if (userSettings().svnEnabled() &&
            svn::isSvnInstalled()
            && svn::isSvnDirectory(workingDir))
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
