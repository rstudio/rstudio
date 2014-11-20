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
#include <boost/signals2.hpp>

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
   boost::function<void(const tree<rscore::FileInfo>&)> onMonitoringEnabled;
   boost::function<void(
         const std::vector<rscore::system::FileChangeEvent>&)> onFilesChanged;
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

   rscore::Error startup(const rscore::FilePath& projectFile,
                       std::string* pUserErrMsg);

   rscore::Error initialize();

public:
   // these functions can be called even when there is no project
   bool hasProject() const { return !file_.empty(); }

   // next session project path -- low level value used by suspend
   // and switch-to-project
   std::string nextSessionProject() const;
   void setNextSessionProject(const std::string& nextSessionProject);

   // last project path -- used to implement restore last project user setting
   rscore::FilePath lastProjectPath() const;
   void setLastProjectPath(const rscore::FilePath& lastProjectPath);

   const rscore::FilePath& file() const { return file_; }
   const rscore::FilePath& directory() const { return directory_; }
   const rscore::FilePath& scratchPath() const { return scratchPath_; }

   rscore::FilePath oldScratchPath() const;

   const rscore::r_util::RProjectConfig& config() const { return config_; }
   void setConfig(const rscore::r_util::RProjectConfig& config)
   {
      config_ = config;
      updateDefaultEncoding();
      updateBuildTargetPath();
      updatePackageInfo();
   }

   rscore::Error readVcsOptions(RProjectVcsOptions* pOptions) const;
   rscore::Error writeVcsOptions(const RProjectVcsOptions& options) const;

   rscore::Error readBuildOptions(RProjectBuildOptions* pOptions);
   rscore::Error writeBuildOptions(const RProjectBuildOptions& options);

   // code which needs to rely on the encoding should call this method
   // rather than getting the encoding off of the config (because the
   // config could have been created on another system with an encoding
   // not available here -- defaultEncoding reflects (if possible) a
   // local mapping of an unknown encoding and a fallback to UTF-8
   // if necessary
   std::string defaultEncoding() const;

   // computed absolute path to project build target directory
   const rscore::FilePath& buildTargetPath() const
   {
      return buildTargetPath_;
   }

   rscore::json::Object uiPrefs() const;

   rscore::json::Array openDocs() const;

   // current build options (note that these are not synchronized
   // accross processes!)
   const RProjectBuildOptions& buildOptions() const
   {
      return buildOptions_;
   }

   // current package info (if this is a package)
   const rscore::r_util::RPackageInfo& packageInfo() const
   {
      return packageInfo_;
   }

   // does this project context have a file monitor? (might not have one
   // if the user has disabled code indexing or if file monitoring failed
   // for this path)
   bool hasFileMonitor() const { return hasFileMonitor_; }

   // are we monitoring the specified directory? (used by other modules to
   // suppress file monitoring if the project already has it covered)
   bool isMonitoringDirectory(const rscore::FilePath& directory) const;

   // subscribe to file monitor notifications -- note that to ensure
   // receipt of the onMonitoringEnabled callback subscription should
   // occur during module initialization
   void subscribeToFileMonitor(const std::string& featureName,
                               const FileMonitorCallbacks& cb);

public:
   static rscore::r_util::RProjectBuildDefaults buildDefaults();
   static rscore::r_util::RProjectConfig defaultConfig();

private:
   // deferred init handler (this allows other modules to reliably subscribe
   // to our file monitoring events with no concern that they'll miss
   // onMonitoringEnabled)
   void onDeferredInit(bool newSession);

   // file monitor event handlers
   void fileMonitorRegistered(rscore::system::file_monitor::Handle handle,
                              const tree<rscore::FileInfo>& files);
   void fileMonitorFilesChanged(
                   const std::vector<rscore::system::FileChangeEvent>& events);
   void fileMonitorTermination(const rscore::Error& error);

   rscore::FilePath vcsOptionsFilePath() const;
   rscore::Error buildOptionsFile(rscore::Settings* pOptionsFile) const;

   void updateDefaultEncoding();
   void updateBuildTargetPath();
   void updatePackageInfo();

   void augmentRbuildignore();

private:
   rscore::FilePath file_;
   rscore::FilePath directory_;
   rscore::FilePath scratchPath_;
   rscore::r_util::RProjectConfig config_;
   std::string defaultEncoding_;
   rscore::FilePath buildTargetPath_;
   RProjectBuildOptions buildOptions_;
   rscore::r_util::RPackageInfo packageInfo_;

   bool hasFileMonitor_;
   std::vector<std::string> monitorSubscribers_;
   boost::signals2::signal<void(const tree<rscore::FileInfo>&)> onMonitoringEnabled_;
   boost::signals2::signal<void(const std::vector<rscore::system::FileChangeEvent>&)>
                                                            onFilesChanged_;
   boost::signals2::signal<void()> onMonitoringDisabled_;
};

ProjectContext& projectContext();


} // namespace projects
} // namesapce session

#endif // SESSION_PROJECTS_PROJECTS_HPP
