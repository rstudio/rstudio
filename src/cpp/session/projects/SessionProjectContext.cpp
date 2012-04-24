/*
 * SessionProjectContext.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
#include <boost/algorithm/string/trim.hpp>

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

   // now add context id to form scratch path
   FilePath scratchPath = projectUserDir.complete(userSettings().contextId());
   Error error = scratchPath.ensureDirectory();
   if (error)
      return error;

   // return the path
   *pScratchPath = scratchPath;
   return Success();
}

FilePath ProjectContext::oldScratchPath() const
{
   // start from the standard .Rproj.user dir
   FilePath projectUserDir = directory().complete(".Rproj.user");
   if (!projectUserDir.exists())
      return FilePath();

   // add username if we can get one
   std::string username = core::system::username();
   if (!username.empty())
      projectUserDir = projectUserDir.complete(username);

   // if this path doesn't exist then bail
   if (!projectUserDir.exists())
      return FilePath();

   // see if an old scratch path using the old contextId is present
   // and if so return it
   FilePath oldPath = projectUserDir.complete(userSettings().oldContextId());
   if (oldPath.exists())
      return oldPath;
   else
      return FilePath();
}

// NOTE: this function is called very early in the process lifetime (from
// session::projects::startup) so can only have limited dependencies.
// specifically, it can rely on userSettings() being available, but can
// definitely NOT rely on calling into R. For initialization related tasks
// that need to run after R is available use the implementation of the
// initialize method (below)
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

   // assume true so that the initial files pane listing doesn't register
   // a duplicate monitor. if it turns out to be false then this can be
   // repaired by a single refresh of the files pane
   hasFileMonitor_ = config_.enableCodeIndexing;

   // return success
   return Success();

}

void ProjectContext::augmentRbuildignore()
{
   if (directory().childPath("DESCRIPTION").exists())
   {
      // constants
      const char * const kIgnoreRprojUser = "^\\.Rproj\\.user$";

      // create the file if it doesn't exists
      FilePath rbuildIgnorePath = directory().childPath(".Rbuildignore");
      if (!rbuildIgnorePath.exists())
      {
         Error error = writeStringToFile(rbuildIgnorePath,
                                         kIgnoreRprojUser + std::string("\n"),
                                         string_utils::LineEndingNative);
         if (error)
            LOG_ERROR(error);
      }
      else
      {
         // if .Rbuildignore exists, add .Rproj.user unless already there
         std::string strIgnore;
         Error error = core::readStringFromFile(
                                             rbuildIgnorePath,
                                             &strIgnore,
                                             string_utils::LineEndingPosix);
         if (error)
         {
            LOG_ERROR(error);
            return;
         }

         if (strIgnore.find(kIgnoreRprojUser) != std::string::npos)
            return;

         bool addExtraNewline = strIgnore.size() > 0
                                && strIgnore[strIgnore.size() - 1] != '\n';

         if (addExtraNewline)
            strIgnore += "\n";
         strIgnore += kIgnoreRprojUser;
         strIgnore += "\n";
         error = core::writeStringToFile(rbuildIgnorePath,
                                         strIgnore,
                                         string_utils::LineEndingNative);
         if (error)
            LOG_ERROR(error);
      }
   }
}

Error ProjectContext::initialize()
{
   if (hasProject())
   {
      // compute the default encoding
      updateDefaultEncoding();

      // augmewnt .Rbuildignore if this is a package
      augmentRbuildignore();

      // subscribe to deferred init (for initializing our file monitor)
      if (config().enableCodeIndexing)
      {
         module_context::events().onDeferredInit.connect(
                          boost::bind(&ProjectContext::onDeferredInit, this));
      }
   }

   return Success();
}


namespace {
const char * const kNextSessionProject = "next-session-project";
const char * const kLastProjectPath = "last-project-path";

FilePath settingsPath()
{
   FilePath settingsPath = module_context::userScratchPath().complete(
                                                        "projects_settings");
   Error error = settingsPath.ensureDirectory();
   if (error)
      LOG_ERROR(error);

   return settingsPath;
}

std::string readSetting(const char * const settingName)
{
   FilePath readPath = settingsPath().complete(settingName);
   if (readPath.exists())
   {
      std::string value;
      Error error = core::readStringFromFile(readPath, &value);
      if (error)
      {
         LOG_ERROR(error);
         return std::string();
      }
      boost::algorithm::trim(value);
      return value;
   }
   else
   {
      return std::string();
   }
}

void writeSetting(const char * const settingName, const std::string& value)
{
   FilePath writePath = settingsPath().complete(settingName);
   Error error = core::writeStringToFile(writePath, value);
   if (error)
      LOG_ERROR(error);
}

} // anonymous namespace

std::string ProjectContext::nextSessionProject() const
{
   return readSetting(kNextSessionProject);
}

void ProjectContext::setNextSessionProject(
                           const std::string& nextSessionProject)
{
   writeSetting(kNextSessionProject, nextSessionProject);
}


FilePath ProjectContext::lastProjectPath() const
{
   std::string path = readSetting(kLastProjectPath);
   if (!path.empty())
      return FilePath(path);
   else
      return FilePath();
}

void ProjectContext::setLastProjectPath(const FilePath& lastProjectPath)
{
   if (!lastProjectPath.empty())
      writeSetting(kLastProjectPath, lastProjectPath.absolutePath());
   else
      writeSetting(kLastProjectPath, "");
}


void ProjectContext::onDeferredInit()
{
   // kickoff file monitoring for this directory
   using namespace boost;
   core::system::file_monitor::Callbacks cb;
   cb.onRegistered = bind(&ProjectContext::fileMonitorRegistered,
                          this, _1, _2);
   cb.onRegistrationError = bind(&ProjectContext::fileMonitorTermination,
                                 this, _1);
   cb.onMonitoringError = bind(&ProjectContext::fileMonitorTermination,
                               this, _1);
   cb.onFilesChanged = bind(&ProjectContext::fileMonitorFilesChanged,
                            this, _1);
   cb.onUnregistered = bind(&ProjectContext::fileMonitorTermination,
                            this, Success());
   core::system::file_monitor::registerMonitor(
                                         directory(),
                                         true,
                                         module_context::fileListingFilter,
                                         cb);
}

void ProjectContext::fileMonitorRegistered(
                              core::system::file_monitor::Handle handle,
                              const tree<core::FileInfo>& files)
{
   // update state
   hasFileMonitor_ = true;

   // notify subscribers
   onMonitoringEnabled_(files);
}

void ProjectContext::fileMonitorFilesChanged(
                   const std::vector<core::system::FileChangeEvent>& events)
{
   // notify client (gwt)
   module_context::enqueFileChangedEvents(directory(), events);

   // notify subscribers
   onFilesChanged_(events);
}

void ProjectContext::fileMonitorTermination(const Error& error)
{
   // always log error
   if (error)
      LOG_ERROR(error);

   // if we have a file monitor then unwind it
   if (hasFileMonitor_)
   {
      // do this only once
      hasFileMonitor_ = false;

      // notify end-user if this was an error condition
      if (error)
      {
         // base error message
         boost::system::error_code ec = error.code();
         std::string dir = module_context::createAliasedPath(directory());
         boost::format fmt(
          "\nWarning message:\n"
          "File monitoring failed for project at \"%1%\"\n"
          "Error %2% (%3%)");
         std::string msg = boost::str(fmt % dir % ec.value() % ec.message());

         // enumeration of affected features
         if (!monitorSubscribers_.empty())
            msg.append("\nFeatures disabled:");
         for(std::size_t i=0; i<monitorSubscribers_.size(); ++i)
         {
            if (i > 0)
               msg.append(",");
            msg.append(" ");
            msg.append(monitorSubscribers_[i]);
         }

         // write to console
         module_context::consoleWriteError(msg);
      }

      // notify subscribers
      onMonitoringDisabled_();
   }
}

bool ProjectContext::isMonitoringDirectory(const FilePath& dir) const
{
   return hasProject() && hasFileMonitor() && dir.isWithin(directory());
}

void ProjectContext::subscribeToFileMonitor(const std::string& featureName,
                                            const FileMonitorCallbacks& cb)
{
   if (!featureName.empty())
      monitorSubscribers_.push_back(featureName);

   if (cb.onMonitoringEnabled)
      onMonitoringEnabled_.connect(cb.onMonitoringEnabled);
   if (cb.onFilesChanged)
      onFilesChanged_.connect(cb.onFilesChanged);
   if (cb.onMonitoringDisabled)
      onMonitoringDisabled_.connect(cb.onMonitoringDisabled);
}

std::string ProjectContext::defaultEncoding() const
{
   return defaultEncoding_;
}

void ProjectContext::updateDefaultEncoding()
{
   defaultEncoding_.clear();
   Error error = r::exec::RFunction(
                     ".rs.validateAndNormalizeEncoding",
                     config().encoding).call(&defaultEncoding_);
   if (error)
      LOG_ERROR(error);

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
}

json::Object ProjectContext::uiPrefs() const
{
   json::Object uiPrefs;
   uiPrefs["use_spaces_for_tab"] = config_.useSpacesForTab;
   uiPrefs["num_spaces_for_tab"] = config_.numSpacesForTab;
   uiPrefs["default_encoding"] = defaultEncoding();
   uiPrefs["default_sweave_engine"] = config_.defaultSweaveEngine;
   uiPrefs["default_latex_program"] = config_.defaultLatexProgram;
   uiPrefs["root_document"] = config_.rootDocument;
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
   defaultConfig.defaultSweaveEngine = userSettings().defaultSweaveEngine();
   defaultConfig.defaultLatexProgram = userSettings().defaultLatexProgram();
   defaultConfig.rootDocument = std::string();
   return defaultConfig;
}


namespace {

const char * const kVcsOverride = "activeVcsOverride";
const char * const kSshKeyPathOverride = "sshKeyPathOverride";

} // anonymous namespace

FilePath ProjectContext::vcsOptionsFilePath() const
{
   return scratchPath().childPath("vcs_options");
}

Error ProjectContext::readVcsOptions(RProjectVcsOptions* pOptions) const
{
   core::Settings settings;
   Error error = settings.initialize(vcsOptionsFilePath());
   if (error)
      return error;

   std::string vcsOverride = settings.get(kVcsOverride);

   pOptions->vcsOverride = module_context::normalizeVcsOverride(vcsOverride);

   return Success();
}

Error ProjectContext::writeVcsOptions(const RProjectVcsOptions& options) const
{
   core::Settings settings;
   Error error = settings.initialize(vcsOptionsFilePath());
   if (error)
      return error;

   settings.beginUpdate();
   settings.set(kVcsOverride, options.vcsOverride);
   settings.endUpdate();

   return Success();
}

} // namespace projects
} // namesapce session

