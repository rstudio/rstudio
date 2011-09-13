/*
 * SessionProjectContext.cpp
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

#include <session/projects/SessionProjects.hpp>

#include <map>

#include <boost/format.hpp>

#include <core/FileSerializer.hpp>
#include <core/r_util/RProjectFile.hpp>

#include <core/system/FileMonitor.hpp>

#include <r/RExec.hpp>

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>

using namespace core;

namespace session {
namespace projects {

namespace {

bool canWriteToProjectDir(const FilePath& projectDirPath)
{
   FilePath testFile = projectDirPath.complete(core::system::generateUuid());
   Error error = core::writeStringToFile(testFile, "test");
   if (error)
   {
      return false;
   }
   else
   {
      error = testFile.removeIfExists();
      if (error)
         LOG_ERROR(error);

      return true;
   }
}

}  // anonymous namespace


Error computeScratchPath(const FilePath& projectFile, FilePath* pScratchPath)
{
   // ensure project user dir
   FilePath projectUserDir = projectFile.parent().complete(".Rproj.user");
   if (!projectUserDir.exists())
   {
      // create
      Error error = projectUserDir.ensureDirectory();
      if (error)
         return error;

      // mark hidden if we are on win32
#ifdef _WIN32
      error = core::system::makeFileHidden(projectUserDir);
      if (error)
         return error;
#endif
   }

   // create user subdirectory if we have a username
   std::string username = core::system::username();
   if (!username.empty())
   {
      projectUserDir = projectUserDir.complete(username);
      Error error = projectUserDir.ensureDirectory();
      if (error)
         return error;
   }

   // now add context id to form scratch path
   FilePath scratchPath = projectUserDir.complete(userSettings().contextId());
   Error error = scratchPath.ensureDirectory();
   if (error)
      return error;

   // return the path
   *pScratchPath = scratchPath;
   return Success();
}

// NOTE: this function is called very early in the process lifetime (from
// session::projects::startup) so can only have limited dependencies.
// specifically, it can rely on userSettings() and persistentState() being
// available, but cannot definitely NOT rely on calling into R. For
// initialization related tasks that need to run after R is available use
// the implementation of the initialize method (below)
Error ProjectContext::startup(const FilePath& projectFile,
                              std::string* pUserErrMsg)
{
   // test for project file existence
   if (!projectFile.exists())
   {
      *pUserErrMsg = "the project file does not exist";
      return pathNotFoundError(projectFile.absolutePath(), ERROR_LOCATION);
   }

   // test for writeabilty of parent
   if (!canWriteToProjectDir(projectFile.parent()))
   {
      *pUserErrMsg = "the project directory is not writeable";
      return systemError(boost::system::errc::permission_denied,
                         ERROR_LOCATION);
   }

   // calculate project scratch path
   FilePath scratchPath;
   Error error = computeScratchPath(projectFile, &scratchPath);
   if (error)
   {
      *pUserErrMsg = "unable to initialize project - " + error.summary();
      return error;
   }

   // read project file config
   bool providedDefaults;
   r_util::RProjectConfig config;
   error = r_util::readProjectFile(projectFile,
                                   defaultConfig(),
                                   &config,
                                   &providedDefaults,
                                   pUserErrMsg);
   if (error)
      return error;

   // if we provided defaults then re-write the project file
   // with the defaults
   if (providedDefaults)
   {
      error = r_util::writeProjectFile(projectFile, config);
      if (error)
         LOG_ERROR(error);
   }

   // initialize members
   file_ = projectFile;
   directory_ = file_.parent();
   scratchPath_ = scratchPath;
   config_ = config;

   // assume true so that the initial files pane listing doesn't register a duplicate
   // monitor. if it turns out to be false then this can be repaired by a single
   // refresh of the files pane
   hasFileMonitor_ = true;

   // return success
   return Success();

}

Error ProjectContext::initialize()
{
   if (hasProject())
   {
      // compute the default encoding
      Error error = r::exec::RFunction(
                        ".rs.validateAndNormalizeEncoding",
                        config().encoding).call(&defaultEncoding_);
      if (error)
         return error;

      // if the default encoding is empty then change to UTF-8 and
      // and enque a warning
      if (defaultEncoding_.empty())
      {
         // fallback
         defaultEncoding_ = "UTF-8";

         // enque a warning
         json::Object msgJson;
         msgJson["severe"] = false;
         boost::format fmt(
           "Project text encoding '%1%' not available (using UTF-8). "
           "You can specify an alternate text encoding via Project Options.");
         msgJson["message"] = boost::str(fmt % config().encoding);
         ClientEvent event(client_events::kShowWarningBar, msgJson);
         module_context::enqueClientEvent(event);
      }

      // kickoff file monitoring for this directory
      core::system::file_monitor::Callbacks cb;
      cb.onRegistered = boost::bind(&ProjectContext::onFileMonitorRegistered, this, _2);
      cb.onRegistrationError = boost::bind(&ProjectContext::onFileMonitorRegistrationError,
                                            this, _1);
      cb.onUnregistered = boost::bind(&ProjectContext::onFileMonitorUnregistered, this);
      cb.onMonitoringError = boost::bind(core::log::logError, _1, ERROR_LOCATION);
      cb.onFilesChanged = boost::bind(module_context::enqueFileChangedEvents,
                                       directory(), _1);
      core::system::file_monitor::registerMonitor(
                                            directory(),
                                            true,
                                            module_context::fileListingFilter,
                                            cb);
   }

   return Success();
}


void ProjectContext::onFileMonitorRegistered(const tree<core::FileInfo>& files)
{
   hasFileMonitor_ = true;

   // TODO: scan files
}

void ProjectContext::onFileMonitorRegistrationError(const Error& error)
{
   LOG_ERROR(error);

   hasFileMonitor_ = false;

   // TODO: fire event to disable code search UI
}

void ProjectContext::onFileMonitorUnregistered()
{
   hasFileMonitor_ = false;

   // TODO: fire event to disable code search UI (note: what happens if this event
   // gets fired during shutdown -- I think we are ok because the client event
   // service has already been stopped
}

bool ProjectContext::isMonitoringDirectory(const FilePath& dir) const
{
   return hasProject() && hasFileMonitor_ && dir.isWithin(directory());
}

std::string ProjectContext::defaultEncoding() const
{
   return defaultEncoding_;
}

json::Object ProjectContext::uiPrefs() const
{
   json::Object uiPrefs;
   uiPrefs["use_spaces_for_tab"] = config_.useSpacesForTab;
   uiPrefs["num_spaces_for_tab"] = config_.numSpacesForTab;
   uiPrefs["default_encoding"] = defaultEncoding();
   return uiPrefs;
}


r_util::RProjectConfig ProjectContext::defaultConfig()
{
   // setup defaults for project file
   r_util::RProjectConfig defaultConfig;
   defaultConfig.useSpacesForTab = userSettings().useSpacesForTab();
   defaultConfig.numSpacesForTab = userSettings().numSpacesForTab();
   if (!userSettings().defaultEncoding().empty())
      defaultConfig.encoding = userSettings().defaultEncoding();
   else
      defaultConfig.encoding = "UTF-8";
   return defaultConfig;
}

} // namespace projects
} // namesapce session

