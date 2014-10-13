/*
 * SessionProjects.hpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
#include <boost/foreach.hpp>
#include <boost/signals.hpp>

#include <core/FileInfo.hpp>
#include <core/FilePath.hpp>
#include <core/Settings.hpp>

#include <core/system/FileMonitor.hpp>
#include <core/system/FileChangeEvent.hpp>

#include <core/json/Json.hpp>

#include <core/collection/Tree.hpp>

#include <core/r_util/RProjectFile.hpp>
#include <core/r_util/RSourceIndex.hpp>
#include <core/r_util/RPackageInfo.hpp>
 
namespace session {
namespace projects {


// file monitoring callbacks (all callbacks are optional)
struct FileMonitorCallbacks
{
   boost::function<void(const tree<rstudiocore::FileInfo>&)> onMonitoringEnabled;
   boost::function<void(
         const std::vector<rstudiocore::system::FileChangeEvent>&)> onFilesChanged;
   boost::function<void()> onMonitoringDisabled;
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
      autoRoxygenizeForCheck(true),
      autoRoxygenizeForBuildPackage(true),
      autoRoxygenizeForBuildAndReload(false)
   {
   }

   std::string makefileArgs;
   bool autoRoxygenizeForCheck;
   bool autoRoxygenizeForBuildPackage;
   bool autoRoxygenizeForBuildAndReload;
};

class ProjectContext : boost::noncopyable
{
public:
   ProjectContext()
      : hasFileMonitor_(false)
   {
   }
   virtual ~ProjectContext() {}

   rstudiocore::Error startup(const rstudiocore::FilePath& projectFile,
                       std::string* pUserErrMsg);

   rstudiocore::Error initialize();

public:
   // these functions can be called even when there is no project
   bool hasProject() const { return !file_.empty(); }

   // next session project path -- low level value used by suspend
   // and switch-to-project
   std::string nextSessionProject() const;
   void setNextSessionProject(const std::string& nextSessionProject);

   // last project path -- used to implement restore last project user setting
   rstudiocore::FilePath lastProjectPath() const;
   void setLastProjectPath(const rstudiocore::FilePath& lastProjectPath);

   const rstudiocore::FilePath& file() const { return file_; }
   const rstudiocore::FilePath& directory() const { return directory_; }
   const rstudiocore::FilePath& scratchPath() const { return scratchPath_; }

   rstudiocore::FilePath oldScratchPath() const;

   const rstudiocore::r_util::RProjectConfig& config() const { return config_; }
   void setConfig(const rstudiocore::r_util::RProjectConfig& config)
   {
      config_ = config;
      updateDefaultEncoding();
      updateBuildTargetPath();
      updatePackageInfo();
   }

   rstudiocore::Error readVcsOptions(RProjectVcsOptions* pOptions) const;
   rstudiocore::Error writeVcsOptions(const RProjectVcsOptions& options) const;

   rstudiocore::Error readBuildOptions(RProjectBuildOptions* pOptions);
   rstudiocore::Error writeBuildOptions(const RProjectBuildOptions& options);

   // code which needs to rely on the encoding should call this method
   // rather than getting the encoding off of the config (because the
   // config could have been created on another system with an encoding
   // not available here -- defaultEncoding reflects (if possible) a
   // local mapping of an unknown encoding and a fallback to UTF-8
   // if necessary
   std::string defaultEncoding() const;

   // computed absolute path to project build target directory
   const rstudiocore::FilePath& buildTargetPath() const
   {
      return buildTargetPath_;
   }

   rstudiocore::json::Object uiPrefs() const;

   rstudiocore::json::Array openDocs() const;

   // current build options (note that these are not synchronized
   // accross processes!)
   const RProjectBuildOptions& buildOptions() const
   {
      return buildOptions_;
   }

   // current package info (if this is a package)
   const rstudiocore::r_util::RPackageInfo& packageInfo() const
   {
      return packageInfo_;
   }

   // does this project context have a file monitor? (might not have one
   // if the user has disabled code indexing or if file monitoring failed
   // for this path)
   bool hasFileMonitor() const { return hasFileMonitor_; }

   // are we monitoring the specified directory? (used by other modules to
   // suppress file monitoring if the project already has it covered)
   bool isMonitoringDirectory(const rstudiocore::FilePath& directory) const;

   // subscribe to file monitor notifications -- note that to ensure
   // receipt of the onMonitoringEnabled callback subscription should
   // occur during module initialization
   void subscribeToFileMonitor(const std::string& featureName,
                               const FileMonitorCallbacks& cb);

public:
   static rstudiocore::r_util::RProjectBuildDefaults buildDefaults();
   static rstudiocore::r_util::RProjectConfig defaultConfig();

private:
   // deferred init handler (this allows other modules to reliably subscribe
   // to our file monitoring events with no concern that they'll miss
   // onMonitoringEnabled)
   void onDeferredInit(bool newSession);

   // file monitor event handlers
   void fileMonitorRegistered(rstudiocore::system::file_monitor::Handle handle,
                              const tree<rstudiocore::FileInfo>& files);
   void fileMonitorFilesChanged(
                   const std::vector<rstudiocore::system::FileChangeEvent>& events);
   void fileMonitorTermination(const rstudiocore::Error& error);

   rstudiocore::FilePath vcsOptionsFilePath() const;
   rstudiocore::Error buildOptionsFile(rstudiocore::Settings* pOptionsFile) const;

   void updateDefaultEncoding();
   void updateBuildTargetPath();
   void updatePackageInfo();

   void augmentRbuildignore();

private:
   rstudiocore::FilePath file_;
   rstudiocore::FilePath directory_;
   rstudiocore::FilePath scratchPath_;
   rstudiocore::r_util::RProjectConfig config_;
   std::string defaultEncoding_;
   rstudiocore::FilePath buildTargetPath_;
   RProjectBuildOptions buildOptions_;
   rstudiocore::r_util::RPackageInfo packageInfo_;

   bool hasFileMonitor_;
   std::vector<std::string> monitorSubscribers_;
   boost::signal<void(const tree<rstudiocore::FileInfo>&)> onMonitoringEnabled_;
   boost::signal<void(const std::vector<rstudiocore::system::FileChangeEvent>&)>
                                                            onFilesChanged_;
   boost::signal<void()> onMonitoringDisabled_;
};

ProjectContext& projectContext();


} // namespace projects
} // namesapce session

#endif // SESSION_PROJECTS_PROJECTS_HPP
