/*
 * SessionProjectContext.cpp
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

#include <session/projects/SessionProjects.hpp>

#include <map>

#include <boost/format.hpp>
#include <boost/algorithm/string/trim.hpp>

#include <core/FileSerializer.hpp>
#include <core/r_util/RPackageInfo.hpp>
#include <core/r_util/RProjectFile.hpp>
#include <core/r_util/RSessionContext.hpp>

#include <core/system/FileMonitor.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionScopes.hpp>

#include <session/projects/ProjectsSettings.hpp>
#include <session/projects/SessionProjectSharing.hpp>

#include <session/prefs/UserPrefs.hpp>
#include <session/prefs/UserState.hpp>

#include <sys/stat.h>

#include "SessionProjectFirstRun.hpp"

#define kStorageFolder "projects"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace projects {

namespace {

static std::unique_ptr<r_util::RPackageInfo> s_pIndexedPackageInfo = nullptr;

void onDescriptionChanged()
{
   s_pIndexedPackageInfo.reset();

   std::unique_ptr<r_util::RPackageInfo> pInfo(new r_util::RPackageInfo);
   Error error = pInfo->read(projectContext().buildTargetPath());
   if (error)
      LOG_ERROR(error);

   pInfo.swap(s_pIndexedPackageInfo);
}

void onProjectFilesChanged(const std::vector<core::system::FileChangeEvent>& events)
{
   FilePath descPath = projectContext().buildTargetPath().completeChildPath("DESCRIPTION");
   for (auto& event : events)
   {
      auto& info = event.fileInfo();
      if (info.absolutePath() == descPath.getAbsolutePath())
      {
         onDescriptionChanged();
         break;
      }
   }
}

}  // anonymous namespace


Error computeScratchPaths(const FilePath& projectFile, 
      FilePath* pScratchPath, FilePath* pSharedScratchPath)
{
   // ensure project user dir
   FilePath projectUserDir = projectFile.getParent().completePath(".Rproj.user");
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
   if (pScratchPath)
   {
      FilePath scratchPath = projectUserDir.completePath(prefs::userState().contextId());
      Error error = scratchPath.ensureDirectory();
      if (error)
         return error;

      // return the path
      *pScratchPath = scratchPath;
   }

   // add "shared" to form shared path (shared among all sessions that have
   // this project open)
   if (pSharedScratchPath)
   {
      FilePath sharedScratchPath = projectUserDir.completePath("shared");
      Error error = sharedScratchPath.ensureDirectory();
      if (error)
         return error;

      // return the path
      *pSharedScratchPath = sharedScratchPath;
   }

   return Success();
}

FilePath ProjectContext::oldScratchPath() const
{
   // start from the standard .Rproj.user dir
   FilePath projectUserDir = directory().completePath(".Rproj.user");
   if (!projectUserDir.exists())
      return FilePath();

   // add username if we can get one
   std::string username = core::system::username();
   if (!username.empty())
      projectUserDir = projectUserDir.completePath(username);

   // if this path doesn't exist then bail
   if (!projectUserDir.exists())
      return FilePath();

   return FilePath();
}

FilePath ProjectContext::websitePath() const
{
   if (hasProject() && !buildTargetPath().isEmpty() && r_util::isWebsiteDirectory(buildTargetPath()))
      return buildTargetPath();
   else
      return FilePath();
}

FilePath ProjectContext::fileUnderWebsitePath(const core::FilePath& file) const
{
   // first check same folder; this will catch building simple R Markdown websites 
   // even without an RStudio project in play
   if (r_util::isWebsiteDirectory(file.getParent()))
      return file.getParent();
   
   // otherwise see if this file is under a website project
   if (!websitePath().isEmpty() && file.isWithin(websitePath()))
      return websitePath();
   
   return FilePath();
}

// NOTE: this function is called very early in the process lifetime (from
// session::projects::startup) so can only have limited dependencies.
// specifically, it can rely on userPrefs() being available, but can
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
      isNewProject_ = true;
      return pathNotFoundError(projectFile.getAbsolutePath(), ERROR_LOCATION);
   }

   // test for writeabilty of parent
   if (!file_utils::isDirectoryWriteable(projectFile.getParent()))
   {
      *pUserErrMsg = "the project directory is not writeable";
      return systemError(boost::system::errc::permission_denied,
                         ERROR_LOCATION);
   }

   // check to see whether or not this project has been opened before
   FilePath projectUserPath = projectFile.getParent().completePath(".Rproj.user");
   if (projectUserPath.exists())
   {
      FilePath contextPath = projectUserPath.completePath(prefs::userState().contextId());
      isNewProject_ = !contextPath.exists();
   }
   else
   {
      isNewProject_ = true;
   }

   // calculate project scratch path
   FilePath scratchPath;
   FilePath sharedScratchPath;
   Error error = computeScratchPaths(projectFile, &scratchPath,
         &sharedScratchPath);
   if (error)
   {
      *pUserErrMsg = "unable to initialize project - " + error.getSummary();
      return error;
   }

   // read project file config
   bool providedDefaults;
   r_util::RProjectConfig config;
   error = r_util::readProjectFile(projectFile,
                                   defaultConfig(),
                                   buildDefaults(),
                                   &config,
                                   &providedDefaults,
                                   pUserErrMsg);
   if (error)
      return error;

   // update package install args with new defaults (one time only)
   ProjectsSettings projSettings(options().userScratchPath());
   const char * kUpdatePackageInstallDefault = "update-pkg-install-default";
   if (projSettings.readSetting(kUpdatePackageInstallDefault).empty())
   {
      projSettings.writeSetting(kUpdatePackageInstallDefault, "1");
      if (r_util::updateSetPackageInstallArgsDefault(&config))
         providedDefaults = true;
   }

   // if we provided defaults then re-write the project file
   // with the defaults
   if (providedDefaults)
   {
      error = r_util::writeProjectFile(projectFile, buildDefaults(), config);
      if (error)
         LOG_ERROR(error);
   }

   // initialize members
   file_ = projectFile;
   directory_ = file_.getParent();
   scratchPath_ = scratchPath;
   sharedScratchPath_ = sharedScratchPath;
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
   if (isPackageProject())
   {
      // constants
      const char * const kIgnoreRproj = R"(^.*\.Rproj$)";
      const char * const kIgnoreRprojUser = R"(^\.Rproj\.user$)";
      const char * const kIgnoreRCheck = R"(\.Rcheck$)";
      const char * const kIgnorePkgTarGz = R"(.*\.tar\.gz$)";
      const char * const kIgnorePkgTgz = R"(.*\.tgz$)";
      const std::string newLine = "\n";

      std::string ignoreLines = kIgnoreRproj + newLine +
                                kIgnoreRprojUser + newLine;
      
      if (session::options().packageOutputInPackageFolder())
      {
         // if package build is writing output into the package directory,
         // exclude those files, too
         std::string packageName = r_util::packageNameFromDirectory(directory());
         if (!packageName.empty())
         {
            packageName.insert(0, "^");
            ignoreLines.append(packageName + kIgnoreRCheck + newLine);
            ignoreLines.append(packageName + kIgnorePkgTarGz + newLine);
            ignoreLines.append(packageName + kIgnorePkgTgz + newLine);
         }
      }

      // create the file if it doesn't exists
      FilePath rbuildIgnorePath = directory().completeChildPath(".Rbuildignore");
      if (!rbuildIgnorePath.exists())
      {
         Error error = writeStringToFile(rbuildIgnorePath,
                                         ignoreLines,
                                         string_utils::LineEndingNative);
         if (error)
            LOG_ERROR(error);
      }
      else
      {
         // if .Rbuildignore exists, add *.Rproj and .Rproj.user unless
         // they are already there

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

         // NOTE: we don't search for the full kIgnoreRproj to account
         // for previous less precisely specified .Rproj entries
         bool hasRProj = strIgnore.find(R"(\.Rproj$)") != std::string::npos;
         bool hasRProjUser = strIgnore.find(kIgnoreRprojUser) != std::string::npos;
         bool hasAllPackageExclusions = true;

         bool addExtraNewline = strIgnore.size() > 0
                                && strIgnore[strIgnore.size() - 1] != '\n';

         if (addExtraNewline)
            strIgnore += newLine;
         if (!hasRProj)
            strIgnore += kIgnoreRproj + newLine;
         if (!hasRProjUser)
            strIgnore += kIgnoreRprojUser + newLine;

         if (session::options().packageOutputInPackageFolder())
         {
            // if package build is writing output into the package directory,
            // make sure we have those exclusions
            std::string packageName = r_util::packageNameFromDirectory(directory());
            if (!packageName.empty())
            {
               packageName.insert(0, "^");
               
               if (strIgnore.find(packageName + kIgnoreRCheck) == std::string::npos)
               {
                  hasAllPackageExclusions = false;
                  strIgnore += packageName + kIgnoreRCheck + newLine;
               }
               if (strIgnore.find(packageName + kIgnorePkgTarGz) == std::string::npos)
               {
                  hasAllPackageExclusions = false;
                  strIgnore += packageName + kIgnorePkgTarGz + newLine;
               }
               if (strIgnore.find(packageName + kIgnorePkgTgz) == std::string::npos)
               {
                  hasAllPackageExclusions = false;
                  strIgnore += packageName + kIgnorePkgTgz + newLine;
               }
            }
         }

         if (hasRProj && hasRProjUser && hasAllPackageExclusions)
            return;

         error = core::writeStringToFile(rbuildIgnorePath,
                                         strIgnore,
                                         string_utils::LineEndingNative);
         if (error)
            LOG_ERROR(error);
      }
   }
}

SEXP rs_getProjectDirectory()
{
   SEXP absolutePathSEXP = R_NilValue;
   if (projectContext().hasProject())
   {
      r::sexp::Protect protect;
      absolutePathSEXP = r::sexp::create(
         projectContext().directory().getAbsolutePath(), &protect);
   }
   return absolutePathSEXP;
}

SEXP rs_hasFileMonitor()
{
   r::sexp::Protect protect;
   return r::sexp::create(projectContext().hasFileMonitor(), &protect);
}

Error ProjectContext::initialize()
{
   using namespace module_context;

   RS_REGISTER_CALL_METHOD(rs_getProjectDirectory);
   RS_REGISTER_CALL_METHOD(rs_hasFileMonitor);

   std::string projectId(kProjectNone);

   if (hasProject())
   {
      // update activeSession
      activeSession().setProject(createAliasedPath(directory()));

      // update scratch paths
      Error error = computeScratchPaths(file_, &scratchPath_, &sharedScratchPath_);
      if (error)
          LOG_ERROR(error);

      // read build options for the side effect of updating buildOptions_
      RProjectBuildOptions buildOptions;
      error = readBuildOptions(&buildOptions);
      if (error)
         LOG_ERROR(error);

      // compute the build target path
      updateBuildTargetPath();

      // update package info
      updatePackageInfo();

      // compute the default encoding
      updateDefaultEncoding();

      // augment .Rbuildignore if this is a package
      augmentRbuildignore();

      // subscribe to deferred init (for initializing our file monitor)
      if (config().enableCodeIndexing)
      {
         module_context::events().onDeferredInit.connect(
                      boost::bind(&ProjectContext::onDeferredInit, this, _1));
      }

      // compute project ID
      projectId = projectToProjectId(module_context::userScratchPath(), FilePath(),
                                     directory().getAbsolutePath()).id();

      // add default open docs if we have them
      addDefaultOpenDocs();
   }
   else
   {
      // update activeSession
      activeSession().setProject(kProjectNone);
   }

   // compute storage path from project ID
   storagePath_ = module_context::userScratchPath().completePath(kStorageFolder).completePath(projectId);
   
   return Success();
}

namespace {

std::vector<std::string> fileMonitorIgnoredComponents()
{
   // first, built-in ignores
   std::vector<std::string> ignores = {

      // don't monitor things in .Rproj.user
      "/.Rproj.user",

      // ignore things within a .git folder
      "/.git",

      // ignore files within an renv or packrat library
      "/renv/library",
      "/renv/staging",
      "/packrat/lib"

   };
   
   // now add user-defined ignores
   json::Array userIgnores = prefs::userPrefs().fileMonitorIgnoredComponents();
   for (auto&& userIgnore : userIgnores)
      if (userIgnore.isString())
         ignores.push_back(userIgnore.getString());
   
   // return vector of ignored components
   return ignores;
   
}

} // end anonymous namespace

void ProjectContext::onDeferredInit(bool newSession)
{
   // update DESCRIPTION file index
   if (projectContext().isPackageProject())
      onDescriptionChanged();

   // kickoff file monitoring for this directory
   using boost::bind;
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

   FileMonitorFilterContext context;
   context.ignoreObjectFiles = prefs::userPrefs().hideObjectFiles();
   context.ignoredComponents = fileMonitorIgnoredComponents();
   
   core::system::file_monitor::registerMonitor(
         directory(),
         true,
         boost::bind(&ProjectContext::fileMonitorFilter, this, _1, context),
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

   // own handler
   onProjectFilesChanged(events);

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
         std::string dir = module_context::createAliasedPath(directory());
         boost::format fmt(
          "\nWarning message:\n"
          "File monitoring failed for project at \"%1%\"\n"
          "Error %2% (%3%)");
         std::string msg = boost::str(fmt % dir % error.getCode() % error.getMessage());

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

         // write final newline
         msg.append("\n");

         // write to console
         module_context::consoleWriteError(msg);
      }

      // notify subscribers
      onMonitoringDisabled_();
   }
}

bool ProjectContext::fileMonitorFilter(
      const FileInfo& fileInfo,
      const FileMonitorFilterContext& context) const
{
   // note that we check for the component occurring anywhere in the
   // path as the Windows file monitor watches all files within the
   // monitored directory recursively (irrespective of the filter)
   // and so we need the filter to apply to files which are 'ignored'
   // and yet still monitored in ignored sub-directories
   std::string path = fileInfo.absolutePath();
   for (auto&& component : context.ignoredComponents)
      if (boost::algorithm::icontains(path, component))
         return false;
   
   return module_context::fileListingFilter(fileInfo, context.ignoreObjectFiles);
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

void ProjectContext::updateBuildTargetPath()
{
   if (config().buildType == r_util::kBuildTypeNone)
   {
      buildTargetPath_ = FilePath();
   }
   else
   {
      // determine the relative build target
      std::string buildTarget;
      if (config().buildType == r_util::kBuildTypePackage)
         buildTarget = config().packagePath;
      else if (config().buildType == r_util::kBuildTypeMakefile)
         buildTarget = config().makefilePath;
      else if (config().buildType == r_util::kBuildTypeWebsite)
         buildTarget = config().websitePath;
      else if (config().buildType == r_util::kBuildTypeCustom)
         buildTarget = config().customScriptPath;

      // determine the path
      if (boost::algorithm::starts_with(buildTarget, "~/") ||
               FilePath::isRootPath(buildTarget))
      {
         buildTargetPath_ = module_context::resolveAliasedPath(buildTarget);
      }
      else
      {
         buildTargetPath_= projects::projectContext().directory().completeChildPath(
            buildTarget);
      }
   }
}

void ProjectContext::updatePackageInfo()
{
   if (config().buildType == r_util::kBuildTypePackage)
   {
      Error error = packageInfo_.read(buildTargetPath());
      if (error)
         LOG_ERROR(error);
   }
}

json::Object ProjectContext::uiPrefs() const
{
   using namespace r_util;

   json::Object uiPrefs;
   uiPrefs[kUseSpacesForTab] = config_.useSpacesForTab;
   uiPrefs[kNumSpacesForTab] = config_.numSpacesForTab;
   uiPrefs[kAutoAppendNewline] = config_.autoAppendNewline;
   uiPrefs[kStripTrailingWhitespace] = config_.stripTrailingWhitespace;
   uiPrefs[kDefaultEncoding] = defaultEncoding();
   uiPrefs[kDefaultSweaveEngine] = config_.defaultSweaveEngine;
   uiPrefs[kDefaultLatexProgram] = config_.defaultLatexProgram;
   uiPrefs[kRootDocument] = config_.rootDocument;
   uiPrefs[kUseRoxygen] = !config_.packageRoxygenize.empty();
   
   // python prefs -- only activate when non-empty
   if (!config_.pythonType.empty() ||
       !config_.pythonVersion.empty() ||
       !config_.pythonPath.empty())
   {
      uiPrefs[kPythonType] = config_.pythonType;
      uiPrefs[kPythonVersion] = config_.pythonVersion;
      uiPrefs[kPythonPath] = config_.pythonPath;
   }

   // markdown prefs -- all have 'use default' option so write them conditionally
   if (config_.markdownWrap != kMarkdownWrapUseDefault)
   {
      uiPrefs[kVisualMarkdownEditingWrap] = boost::algorithm::to_lower_copy(config_.markdownWrap);
      if (config_.markdownWrap == kMarkdownWrapColumn)
         uiPrefs[kVisualMarkdownEditingWrapAtColumn] = config_.markdownWrapAtColumn;
   }
   
   if (config_.markdownReferences != kMarkdownReferencesUseDefault)
      uiPrefs[kVisualMarkdownEditingReferencesLocation] = boost::algorithm::to_lower_copy(config_.markdownReferences);
   
   if (config_.markdownCanonical != DefaultValue)
      uiPrefs[kVisualMarkdownEditingCanonical] = config_.markdownCanonical == YesValue;

   // zotero prefs (only activate when non-empty)
   if (config_.zoteroLibraries.has_value())
      uiPrefs[kZoteroLibraries] = json::toJsonArray(config_.zoteroLibraries.get());

   // spelling prefs (only activate when non-empty)
   if (!config_.spellingDictionary.empty())
      uiPrefs[kSpellingDictionaryLanguage] = config_.spellingDictionary;

   return uiPrefs;
}

json::Array ProjectContext::openDocs() const
{
   json::Array openDocsJson;
   std::vector<std::string> docs = projects::collectFirstRunDocs(scratchPath());
   for (const std::string& doc : docs)
   {
      FilePath docPath = directory().completeChildPath(doc);
      openDocsJson.push_back(module_context::createAliasedPath(docPath));
   }
   return openDocsJson;
}

r_util::RProjectBuildDefaults ProjectContext::buildDefaults()
{
   r_util::RProjectBuildDefaults buildDefaults;
   buildDefaults.useDevtools = prefs::userPrefs().useDevtools();
   return buildDefaults;
}

r_util::RProjectConfig ProjectContext::defaultConfig()
{
   // setup defaults for project file
   r_util::RProjectConfig defaultConfig;
   defaultConfig.rVersion = r_util::RVersionInfo(kRVersionDefault);
   defaultConfig.useSpacesForTab = prefs::userPrefs().useSpacesForTab();
   defaultConfig.numSpacesForTab = prefs::userPrefs().numSpacesForTab();
   defaultConfig.autoAppendNewline = prefs::userPrefs().autoAppendNewline();
   defaultConfig.stripTrailingWhitespace =
                              prefs::userPrefs().stripTrailingWhitespace();
   if (!prefs::userPrefs().defaultEncoding().empty())
      defaultConfig.encoding = prefs::userPrefs().defaultEncoding();
   else
      defaultConfig.encoding = "UTF-8";
   defaultConfig.defaultSweaveEngine = prefs::userPrefs().defaultSweaveEngine();
   defaultConfig.defaultLatexProgram = prefs::userPrefs().defaultLatexProgram();
   defaultConfig.rootDocument = std::string();
   defaultConfig.buildType = std::string();
   defaultConfig.tutorialPath = std::string();
   defaultConfig.packageUseDevtools = prefs::userPrefs().useDevtools();
   return defaultConfig;
}


namespace {

const char * const kVcsOverride = "activeVcsOverride";

} // anonymous namespace

FilePath ProjectContext::vcsOptionsFilePath() const
{
   return scratchPath().completeChildPath("vcs_options");
}

Error ProjectContext::buildOptionsFile(Settings* pOptionsFile) const
{
   return pOptionsFile->initialize(scratchPath().completeChildPath("build_options"));
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

Error ProjectContext::readBuildOptions(RProjectBuildOptions* pOptions)
{
   core::Settings optionsFile;
   Error error = buildOptionsFile(&optionsFile);
   if (error)
      return error;

   pOptions->makefileArgs = optionsFile.get("makefile_args");
   pOptions->previewWebsite = optionsFile.getBool("preview_website", true);
   pOptions->livePreviewWebsite = optionsFile.getBool("live_preview_website", true);
   pOptions->websiteOutputFormat = optionsFile.get("website_output_format", "all");
   pOptions->autoRoxygenizeForCheck = optionsFile.getBool(
                                       "auto_roxygenize_for_check",
                                       true);
   pOptions->autoRoxygenizeForBuildPackage = optionsFile.getBool(
                                       "auto_roxygenize_for_build_package",
                                       true);
   pOptions->autoRoxygenizeForBuildAndReload = optionsFile.getBool(
                                       "auto_roxygenize_for_build_and_reload",
                                       false);

   // opportunistically sync in-memory representation to what we read from disk
   buildOptions_ = *pOptions;

   return Success();
}

Error ProjectContext::writeBuildOptions(const RProjectBuildOptions& options)
{
   core::Settings optionsFile;
   Error error = buildOptionsFile(&optionsFile);
   if (error)
      return error;

   optionsFile.beginUpdate();
   optionsFile.set("makefile_args", options.makefileArgs);
   optionsFile.set("preview_website", options.previewWebsite);
   optionsFile.set("live_preview_website", options.livePreviewWebsite);
   optionsFile.set("website_output_format", options.websiteOutputFormat);
   optionsFile.set("auto_roxygenize_for_check",
                   options.autoRoxygenizeForCheck);
   optionsFile.set("auto_roxygenize_for_build_package",
                   options.autoRoxygenizeForBuildPackage);
   optionsFile.set("auto_roxygenize_for_build_and_reload",
                   options.autoRoxygenizeForBuildAndReload);
   optionsFile.endUpdate();

   // opportunistically sync in-memory representation to what we wrote to disk
   buildOptions_ = options;

   return Success();
}

void ProjectContext::setWebsiteOutputFormat(
                           const std::string& websiteOutputFormat)
{
   core::Settings optionsFile;
   Error error = buildOptionsFile(&optionsFile);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   optionsFile.set("website_output_format", websiteOutputFormat);

   buildOptions_.websiteOutputFormat = websiteOutputFormat;

}

bool ProjectContext::isPackageProject()
{
   if (s_pIndexedPackageInfo != nullptr)
      return s_pIndexedPackageInfo->type() == kPackageType;

   return r_util::isPackageDirectory(directory());
}

bool ProjectContext::supportsSharing()
{
   // never supports sharing if disabled explicitly
   if (!core::system::getenv(kRStudioDisableProjectSharing).empty())
      return false;

   // otherwise, check to see whether shared storage is configured
   return !options().getOverlayOption(kSessionSharedStoragePath).empty();
}

// attempts to determine whether we can browse above the project folder
bool ProjectContext::parentBrowseable()
{
#ifdef _WIN32
   // we don't need to know this on Windows, and we'd need to compute it very
   // differently
   return true;
#else
   bool browse = true;
   Error error = directory().getParent().isReadable(browse);
   if (error)
   {
      // if we can't figure it out, presume it to be browseable (this preserves
      // existing behavior) 
      LOG_ERROR(error);
      return true;
   }
   return browse;
#endif
}

void ProjectContext::addDefaultOpenDocs()
{
   std::string defaultOpenDocs = config().defaultOpenDocs;
   if (!defaultOpenDocs.empty() && isNewProject())
   {
      std::vector<std::string> docs;
      boost::algorithm::split(docs, defaultOpenDocs, boost::is_any_of(":"));

      for (std::string& doc : docs)
      {
         boost::algorithm::trim(doc);

         FilePath docPath = directory().completePath(doc);
         if (docPath.exists())
         {
            addFirstRunDoc(scratchPath(), doc);
         }
      }
   }
}

} // namespace projects
} // namespace session
} // namespace rstudio

