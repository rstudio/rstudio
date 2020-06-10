/*
 * SessionProjects.hpp
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

#ifndef SESSION_PROJECTS_PROJECTS_HPP
#define SESSION_PROJECTS_PROJECTS_HPP

#include <vector>
#include <map>

#include <boost/utility.hpp>
#include <boost/shared_ptr.hpp>

#include <core/BoostSignals.hpp>
#include <core/FileInfo.hpp>
#include <shared_core/FilePath.hpp>
#include <core/Settings.hpp>

#include <core/system/FileMonitor.hpp>
#include <core/system/FileChangeEvent.hpp>

#include <shared_core/json/Json.hpp>

#include <core/collection/Tree.hpp>

#include <core/r_util/RProjectFile.hpp>
#include <core/r_util/RSourceIndex.hpp>
#include <core/r_util/RPackageInfo.hpp>

namespace rstudio {
namespace session {
namespace projects {


// file monitoring callbacks (all callbacks are optional)
struct FileMonitorCallbacks
{
   boost::function<void(const tree<core::FileInfo>&)> onMonitoringEnabled;
   boost::function<void(
         const std::vector<core::system::FileChangeEvent>&)> onFilesChanged;
   boost::function<void()> onMonitoringDisabled;
};

// file monitor ignore context
struct FileMonitorFilterContext
{
   std::vector<std::string> ignoredComponents;
   bool ignoreObjectFiles;
};

// vcs options
struct RProjectVcsOptions
{
   std::string vcsOverride;
};

// build options
struct RProjectBuildOptions
{
   RProjectBuildOptions() :
      previewWebsite(true),
      livePreviewWebsite(true),
      websiteOutputFormat(),
      autoRoxygenizeForCheck(true),
      autoRoxygenizeForBuildPackage(true),
      autoRoxygenizeForBuildAndReload(false)
   {
   }

   std::string makefileArgs;
   bool previewWebsite;
   bool livePreviewWebsite;
   std::string websiteOutputFormat;
   bool autoRoxygenizeForCheck;
   bool autoRoxygenizeForBuildPackage;
   bool autoRoxygenizeForBuildAndReload;
};

class ProjectContext : boost::noncopyable
{
public:
   ProjectContext()
      : isNewProject_(false),
        hasFileMonitor_(false)
   {
   }
   
   virtual ~ProjectContext() {}

   core::Error startup(const core::FilePath& projectFile,
                       std::string* pUserErrMsg);

   core::Error initialize();

   // these functions can be called even when there is no project
   bool hasProject() const { return !file_.isEmpty(); }
   
   // Path to the .RProj file representing the project
   const core::FilePath& file() const { return file_; }

   // Path to the directory in which the project resides
   const core::FilePath& directory() const { return directory_; }

   // Path to the project user scratch path; user-specific and stored in .Rproj.user
   const core::FilePath& scratchPath() const { return scratchPath_; }

   // Path to storage shared among users; stored in .Rproj.user
   const core::FilePath& sharedScratchPath() const { return sharedScratchPath_; }

   // Path to external user and project-specific storage folder outside .Rproj.user (in e.g.
   // .rstudio or .rstudio-desktop)
   const core::FilePath& externalStoragePath() const { return storagePath_; }

   core::FilePath oldScratchPath() const;
   core::FilePath websitePath() const;

   // return website path containing given file, or empty path if not
   // part of a website
   core::FilePath fileUnderWebsitePath(const core::FilePath& file) const;

   const core::r_util::RProjectConfig& config() const { return config_; }
   void setConfig(const core::r_util::RProjectConfig& config)
   {
      config_ = config;
      updateDefaultEncoding();
      updateBuildTargetPath();
      updatePackageInfo();
      onConfigChanged();
   }

   // signal emitted when config changes
   RSTUDIO_BOOST_SIGNAL<void()> onConfigChanged;

   core::Error readVcsOptions(RProjectVcsOptions* pOptions) const;
   core::Error writeVcsOptions(const RProjectVcsOptions& options) const;

   core::Error readBuildOptions(RProjectBuildOptions* pOptions);
   core::Error writeBuildOptions(const RProjectBuildOptions& options);

   // update the website output type
   void setWebsiteOutputFormat(const std::string& websiteOutputFormat);

   // code which needs to rely on the encoding should call this method
   // rather than getting the encoding off of the config (because the
   // config could have been created on another system with an encoding
   // not available here -- defaultEncoding reflects (if possible) a
   // local mapping of an unknown encoding and a fallback to UTF-8
   // if necessary
   std::string defaultEncoding() const;

   // computed absolute path to project build target directory
   const core::FilePath& buildTargetPath() const
   {
      return buildTargetPath_;
   }

   core::json::Object uiPrefs() const;

   core::json::Array openDocs() const;

   // current build options (note that these are not synchronized
   // across processes!)
   const RProjectBuildOptions& buildOptions() const
   {
      return buildOptions_;
   }

   const core::r_util::RPackageInfo& packageInfo() const
   {
      return packageInfo_;
   }
   
   // is this an R package project?
   bool isPackageProject();
   
   // is this a new project? (ie: has not been opened or initialized before)
   bool isNewProject() const { return isNewProject_; }

   // does this project context have a file monitor? (might not have one
   // if the user has disabled code indexing or if file monitoring failed
   // for this path)
   bool hasFileMonitor() const { return hasFileMonitor_; }

   // are we monitoring the specified directory? (used by other modules to
   // suppress file monitoring if the project already has it covered)
   bool isMonitoringDirectory(const core::FilePath& directory) const;

   // subscribe to file monitor notifications -- note that to ensure
   // receipt of the onMonitoringEnabled callback subscription should
   // occur during module initialization
   void subscribeToFileMonitor(const std::string& featureName,
                               const FileMonitorCallbacks& cb);

   // can this project be shared with other users?
   bool supportsSharing();

   // can we browse in the parent directories of this project?
   bool parentBrowseable();

public:
   static core::r_util::RProjectBuildDefaults buildDefaults();
   static core::r_util::RProjectConfig defaultConfig();

private:
   // deferred init handler (this allows other modules to reliably subscribe
   // to our file monitoring events with no concern that they'll miss
   // onMonitoringEnabled)
   void onDeferredInit(bool newSession);

   // file monitor event handlers
   void fileMonitorRegistered(core::system::file_monitor::Handle handle,
                              const tree<core::FileInfo>& files);
   void fileMonitorFilesChanged(
                   const std::vector<core::system::FileChangeEvent>& events);
   void fileMonitorTermination(const core::Error& error);
   bool fileMonitorFilter(const core::FileInfo& fileInfo,
                          const FileMonitorFilterContext& context) const;

   core::FilePath vcsOptionsFilePath() const;
   core::Error buildOptionsFile(core::Settings* pOptionsFile) const;

   void updateDefaultEncoding();
   void updateBuildTargetPath();
   void updatePackageInfo();

   void augmentRbuildignore();

   // adds default open docs if specified in the project and it has
   // never been opened before
   void addDefaultOpenDocs();

private:
   core::FilePath file_;
   core::FilePath directory_;
   core::FilePath scratchPath_;
   core::FilePath sharedScratchPath_;
   core::FilePath storagePath_;
   core::r_util::RProjectConfig config_;
   std::string defaultEncoding_;
   core::FilePath buildTargetPath_;
   RProjectBuildOptions buildOptions_;
   core::r_util::RPackageInfo packageInfo_;
   bool isNewProject_;

   bool hasFileMonitor_;
   std::vector<std::string> monitorSubscribers_;
   RSTUDIO_BOOST_SIGNAL<void(const tree<core::FileInfo>&)> onMonitoringEnabled_;
   RSTUDIO_BOOST_SIGNAL<void(const std::vector<core::system::FileChangeEvent>&)>
                                                            onFilesChanged_;
   RSTUDIO_BOOST_SIGNAL<void()> onMonitoringDisabled_;
};

ProjectContext& projectContext();

core::json::Array websiteOutputFormatsJson();

} // namespace projects
} // namespace session
} // namespace rstudio

#endif // SESSION_PROJECTS_PROJECTS_HPP
