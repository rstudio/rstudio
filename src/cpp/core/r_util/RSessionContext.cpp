/*
 * RSessionContext.cpp
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

#include <core/r_util/RSessionContext.hpp>

#include <iostream>

#include <boost/format.hpp>

#include <boost/algorithm/string/trim.hpp>
#include <boost/algorithm/string/regex.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <shared_core/FilePath.hpp>
#include <core/Settings.hpp>
#include <core/FileSerializer.hpp>

#include <core/http/Util.hpp>
#include <core/http/URL.hpp>

#include <core/system/System.hpp>
#include <core/system/Environment.hpp>

#include <core/r_util/RActiveSessions.hpp>
#include <core/r_util/RProjectFile.hpp>

#ifndef _WIN32
#include <sys/stat.h>
#include <unistd.h>
#include <core/system/PosixUser.hpp>
#endif

#include <shared_core/SafeConvert.hpp>
#include <core/system/Environment.hpp>

#include "config.h"

// must be included after config.h for RSTUDIO_SERVER define
#include <core/system/UserObfuscation.hpp>

#define kSessionSuffix         "-d"
#define kProjectNone           "none"
#define kRstudioMinimumUserId  "RSTUDIO_MINIMUM_USER_ID"

namespace rstudio {
namespace core {
namespace r_util {

SessionScope SessionScope::fromProject(
                               const std::string& project,
                               const std::string& id,
                               const FilePathToProjectId& filePathToProjectId)
{
   if (project != kProjectNone)
   {
      ProjectId projectId = filePathToProjectId(project);
      return SessionScope(projectId, id);
   }
   else
   {
      return projectNone(id);
   }
}


std::string SessionScope::projectPathForScope(
                               const SessionScope& scope,
                               const ProjectIdToFilePath& projectIdToFilePath)
{
   return projectIdToFilePath(scope.projectId());
}


SessionScope SessionScope::fromProjectId(const ProjectId& project,
                                         const std::string& id)
{
   return SessionScope(project, id);
}

SessionScope SessionScope::projectNone(const std::string& id)
{
   return SessionScope(ProjectId(kProjectNoneId), id);
}

SessionScope SessionScope::jupyterLabSession(const std::string& id)
{
   // note: project ID is currently unused as it is meaningless
   // in the context of Jupyter sessions
   return SessionScope(ProjectId(kJupyterLabId), id);
}

SessionScope SessionScope::jupyterNotebookSession(const std::string& id)
{
   // note: project ID is currently unused as it is meaningless
   // in the context of Jupyter sessions
   return SessionScope(ProjectId(kJupyterNotebookId), id);
}

SessionScope SessionScope::vscodeSession(const std::string& id)
{
   // note: project ID is currently unused as it is meaningless
   // in the context of external workbenches
   return SessionScope(ProjectId(kVSCodeId), id);
}

bool SessionScope::isProjectNone() const
{
   return project_.id() == kProjectNoneId;
}

bool SessionScope::isWorkspaces() const
{
   return project_.id() == kWorkspacesId;
}

bool SessionScope::isJupyter() const
{
   return isJupyterLab() || isJupyterNotebook();
}

bool SessionScope::isJupyterLab() const
{
   return project_.id() == kJupyterLabId;
}

bool SessionScope::isJupyterNotebook() const
{
   return project_.id() == kJupyterNotebookId;
}

bool SessionScope::isVSCode() const
{
   return project_.id() == kVSCodeId;
}

std::string SessionScope::workbench() const
{
   if (isJupyter())
      return isJupyterLab() ? kWorkbenchJupyterLab : kWorkbenchJupyterNotebook;
   else if (isVSCode())
      return kWorkbenchVSCode;
   else
      return kWorkbenchRStudio;
}

// This function is intended to tell us whether a given path corresponds to an
// RStudio shared project owned by another user but shared with this user. It
// is primarily used to disallow opening shared projects when they're disabled.
// For this reason, it returns false for projects which are shared using
// ordinary filesystem attributes.
bool isSharedPath(const std::string& projectPath,
                  const core::FilePath& userHomePath)
{
#ifndef _WIN32
   Error error;
   // ensure this is a real path
   FilePath projectDir = FilePath::resolveAliasedPath(projectPath,
                                                      userHomePath);
   if (!projectDir.exists())
      return false;

   struct stat st;
   if (::stat(projectDir.getAbsolutePath().c_str(), &st) == 0)
   {
      // not shared if we own the directory
      if (st.st_uid == ::geteuid())
         return false;

      // not shared if file permissions give everyone access
      if (st.st_mode & (S_IROTH | S_IWOTH | S_IXOTH)) 
         return false;

      core::system::User user;
      error = core::system::User::getCurrentUser(user);
      if (error)
      {
         LOG_ERROR(error);
         return false;
      }

      // not shared if our group owns the directory 
      if (st.st_gid == user.getGroupId())
         return false;

#ifndef __APPLE__
      // not shared if we're in any of the groups that own the directory
      // (note that this checks supplementary group IDs only, so the check
      // against the primary group ID above is still required)
      if (::group_member(st.st_gid))
         return false;
#endif 

      // if we got this far, we likely have access due to project sharing
      // (ACLs)
      return true;
   }
   else
   {
      error = systemError(errno, ERROR_LOCATION);
      error.addProperty("path", projectDir.getAbsolutePath());
      LOG_ERROR(error);
   }

#endif
   return false;
}

SessionScopeState validateSessionScope(const SessionScope& scope,
                          const core::FilePath& userHomePath,
                          const core::FilePath& userScratchPath,
                          core::r_util::ProjectIdToFilePath projectIdToFilePath,
                          bool projectSharingEnabled,
                          std::string* pProjectFilePath)
{
   // does this session exist?
   r_util::ActiveSessions activeSessions(userScratchPath);
   boost::shared_ptr<r_util::ActiveSession> pSession
                                          = activeSessions.get(scope.id());
   if (pSession->empty() || !pSession->validate(userHomePath,
                                                projectSharingEnabled))
      return ScopeInvalidSession;

   // if this isn't project none then check if the project exists
   if (!scope.isProjectNone())
   {
      // lookup the project path by id
      std::string project = r_util::SessionScope::projectPathForScope(
               scope,
               projectIdToFilePath);
      if (project.empty())
         return ScopeMissingProject;

      // if session points to another project then the scope is invalid
      if (project != pSession->project())
         return ScopeInvalidProject;

      // get the path to the project directory
      FilePath projectDir = FilePath::resolveAliasedPath(project, userHomePath);
      if (!projectDir.exists())
         return ScopeMissingProject;

      // get the path to the project file
      FilePath projectPath = r_util::projectFromDirectory(projectDir);
      if (!projectPath.exists())
         return ScopeMissingProject;

      // record path to project file
      *pProjectFilePath = projectPath.getAbsolutePath();
   }
   else
   {
      // if the session project isn't project none then it's invalid
      if (pSession->project() != kProjectNone)
         return ScopeInvalidProject;
   }

   // if we got this far the scope is valid, do one final check for
   // trying to open a shared project if sharing is disabled
   if (!projectSharingEnabled &&
       r_util::isSharedPath(*pProjectFilePath, userHomePath))
   {
      return r_util::ScopeMissingProject;
   }
   else
   {
      return ScopeValid;
   }
}

std::string urlPathForSessionScope(const SessionScope& scope)
{
   // get a URL compatible project path
   std::string project = http::util::urlEncode(scope.projectId().asString());
   boost::algorithm::replace_all(project, "%2F", "/");

   // create url
   boost::format fmt("/s/%1%%2%/");
   return boost::str(fmt % project % scope.id());
}

void parseSessionUrl(const std::string& url,
                     SessionScope* pScope,
                     std::string* pUrlPrefix,
                     std::string* pUrlWithoutPrefix,
                     std::string* pBaseUrl)
{
   static boost::regex re("/s/([A-Fa-f0-9]{5})([A-Fa-f0-9]{8})([A-Fa-f0-9]{8})(/|$)");

   boost::smatch match;
   if (regex_utils::search(url, match, re))
   {
      if (pScope)
      {
         std::string user = http::util::urlDecode(match[1]);
         std::string project = http::util::urlDecode(match[2]);
         std::string id = match[3];
         *pScope = r_util::SessionScope::fromProjectId(
                  ProjectId(project, user), id);
      }
      if (pUrlPrefix)
      {
         *pUrlPrefix = match[0];
      }
      if (pUrlWithoutPrefix)
      {
         *pUrlWithoutPrefix = boost::algorithm::replace_first_copy(
                                   url, std::string(match[0]), "/");
      }
      if (pBaseUrl)
      {
         http::URL urlObj(url.substr(0, url.find(match[0])));
         *pBaseUrl = urlObj.path();
      }
   }
   else
   {
      if (pScope)
         *pScope = SessionScope();
      if (pUrlPrefix)
         *pUrlPrefix = std::string();
      if (pUrlWithoutPrefix)
         *pUrlWithoutPrefix = url;
      if (pBaseUrl)
         *pBaseUrl = std::string();
   }
}

std::string createSessionUrl(const std::string& hostPageUrl,
                             const SessionScope& scope)
{
   // build scope path for project (e.g. /s/BAF43..../).
   std::string scopePath = urlPathForSessionScope(scope);

   // determine the host scope path
   std::string hostScopePath;
   parseSessionUrl(hostPageUrl, nullptr, &hostScopePath, nullptr);

   // if we got a scope path then take everything before
   // it and append our target scope path
   if (!hostScopePath.empty())
   {
      // extract the base url
      size_t pos = hostPageUrl.find(hostScopePath);
      std::string baseUrl = hostPageUrl.substr(0, pos);

      // complete the url and return it
      return baseUrl + scopePath;
   }
   else
   {
      // completely unexpected that we'd pass a host page url
      // with no scope path!  log a warning and just complete
      // against the root of the host page url

      LOG_WARNING_MESSAGE("Attempted to create session url from "
                          "non prefixed host page " + hostPageUrl);

      return http::URL::complete(hostPageUrl, scopePath);
   }
}


std::ostream& operator<< (std::ostream& os, const SessionContext& context)
{
   os << context.username;
   if (!context.scope.project().empty())
      os << " -- " << context.scope.project();
   if (!context.scope.id().empty())
      os << " [" << context.scope.id() << "]";
   return os;
}


std::string sessionScopeFile(std::string prefix,
                             const SessionScope& scope)
{   
   // resolve project path
   ProjectId projectId = scope.projectId();
   std::string project = scope.isProjectNone() || scope.isWorkspaces() ?
            projectId.id() : projectId.asString();

   if (!project.empty())
   {
      // pluralize in the presence of project context so there
      // is no conflict when switching between single and multi-session
      if (!scope.project().empty())
         prefix += "s";

      if (!boost::algorithm::starts_with(project, "/"))
         project = "/" + project;

      if (!scope.id().empty())
      {
         if (!boost::algorithm::ends_with(project, "/"))
            project = project + "/";
      }
   }

   // return file path
   return prefix + project + scope.id();
}

std::string sessionScopePrefix(const std::string& username)
{
   return username + kSessionSuffix;
}

std::string sessionScopesPrefix(const std::string& username)
{
   // pluralize the prefix so there is no conflict when switching
   // between the single file and directory based schemas
   return username + kSessionSuffix "s";
}

std::string sessionContextFile(const SessionContext& context)
{
   return sessionScopeFile(sessionScopePrefix(context.username), context.scope);
}

std::string generateScopeId()
{
   std::vector<std::string> reserved;

   // reserved ids we are using now
   reserved.push_back(kProjectNoneId);
   reserved.push_back(kWorkspacesId);
   reserved.push_back(kJupyterLabId);
   reserved.push_back(kJupyterNotebookId);

   // a few more for future expansion
   reserved.push_back("f468a750");
   reserved.push_back("6ae9dc1b");
   reserved.push_back("1d717df9");
   reserved.push_back("6d3c4c0e");
   reserved.push_back("a142989c");
   reserved.push_back("c2612a98");
   reserved.push_back("2b84c99a");

   return generateScopeId(reserved);
}

std::string generateScopeId(const std::vector<std::string>& reserved)
{
   // generate id
   std::string id = core::string_utils::toLower(
                                 core::system::generateShortenedUuid());

   // ensure 8 chracters
   if (id.length() != kProjectIdLen)
   {
      if (id.length() > kProjectIdLen)
      {
         id = id.substr(0, kProjectIdLen);
      }
      else
      {
         size_t diff = kProjectIdLen - id.length();
         std::string pad(diff, 'f');
         id += pad;
      }
   }

   // try again if this id is reserved
   if (std::find(reserved.begin(), reserved.end(), id) != reserved.end())
      return generateScopeId(reserved);
   else
      return id;
}

static uid_t s_minUid = 0;

void setMinUid(uid_t uid)
{
   s_minUid = uid;
}

namespace {

// max user ID string (length of 5)
constexpr uid_t MAX_UID = 0xFFFFF;

uid_t getMinUid()
{
   std::string minUserEnv = core::system::getenv(kRstudioMinimumUserId);
   if (minUserEnv.empty())
      return s_minUid;

   return safe_convert::stringTo<uid_t>(minUserEnv, 0);
}

} // anonymous namespace

std::string obfuscatedUserId(uid_t uid)
{
   if (uid > MAX_UID)
   {
      // if large uids are being used, we want to wrap them back to a value lower than
      // the maximum allowed uid to prevent duplicate ID strings
      // if the difference between this uid and the minimum allowed value is less than
      // the maximum allowed uid, use it instead so we do not have to worry about duplicates
      // as the resulting obfuscated ID will not have to be truncated
      //
      // if the difference is still greater than the max uid, we will have to live with truncation
      // so do not bother remapping the uid
      uid_t minUid = getMinUid();
      if (uid >= minUid && (uid - minUid < MAX_UID))
         uid -= minUid;
   }

   std::ostringstream ustr;
   ustr << std::setw(kUserIdLen) << std::setfill('0') << std::hex
        << OBFUSCATE_USER_ID(uid);
   return ustr.str().substr(0, kUserIdLen);
}

} // namespace r_util
} // namespace core 
} // namespace rstudio


