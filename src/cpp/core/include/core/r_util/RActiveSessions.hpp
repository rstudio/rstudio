/*
 * RActiveSessions.hpp
 *
 * Copyright (C) 2021 by RStudio, PBC
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


#ifndef CORE_R_UTIL_ACTIVE_SESSIONS_HPP
#define CORE_R_UTIL_ACTIVE_SESSIONS_HPP

#include <boost/noncopyable.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/Log.hpp>
#include <core/Settings.hpp>
#include <core/DateTime.hpp>
#include <shared_core/SafeConvert.hpp>

#include <core/r_util/RSessionContext.hpp>
#include <core/r_util/RProjectFile.hpp>
#include <core/r_util/RActiveSessionStorage.hpp>

namespace rstudio {
namespace core {
namespace r_util {

class ActiveSession : boost::noncopyable
{
private:

   friend class ActiveSessions;
   explicit ActiveSession(std::shared_ptr<IActiveSessionStorage> storage) {}

   ActiveSession(
      std::shared_ptr<IActiveSessionStorage> storage,
      const std::string& id) :
         storage_(storage),
         id_(id) 
   {
   }

   ActiveSession(
      std::shared_ptr<IActiveSessionStorage> storage,
      const std::string& id,
      const FilePath& scratchPath) : 
         storage_(storage),
         id_(id),
         scratchPath_(scratchPath)
   {
      core::Error error = scratchPath_.ensureDirectory();
      if (error)
         LOG_ERROR(error);
   }

   std::string const kExecuting = "executing";
   std::string const kInitial = "initial";
   std::string const kLabel = "label";
   std::string const kLastUsed = "last_used";
   std::string const kProject = "project";
   std::string const kSavePromptRequired = "save_prompt_required";
   std::string const kSessionSuspendData = "suspended_session_data";
   std::string const kRunning = "running";
   std::string const kRVersion = "r_version";
   std::string const kRVersionHome = "r_version_home";
   std::string const kRVersionLabel = "r_version_label";
   std::string const kWorkingDir = "working_directory";

 public:

   bool empty() const { return scratchPath_.isEmpty(); }

   std::string id() const { return id_; }

   const FilePath& scratchPath() const { return scratchPath_; }

   std::string project() const
   {
      std::string project;
      if (!empty())
      {
         Error error = storage_->readProperty(id_, kProject, &project);
         if (error)
            LOG_ERROR(error);
      }

      return project;
   }

   void setProject(const std::string& project)
   {
      if (!empty())
      {
         Error error = storage_->writeProperty(id_, kProject, project);
         if (error)
            LOG_ERROR(error);
      }
   }

   std::string workingDir() const
   {
      std::string value;
      if (!empty())
      {
         Error error = storage_->readProperty(id_, kWorkingDir, &value);
         if (error)
            LOG_ERROR(error);
      }
      return value;
   }

   void setWorkingDir(const std::string& workingDir)
   {
      if (!empty())
      {
         Error error = storage_->writeProperty(id_, kWorkingDir, workingDir);
         if (error)
            LOG_ERROR(error);
      }
   }

   bool initial() const
   {
      if (!empty())
      {
         std::string value;
         Error error = storage_->readProperty(id_, kInitial, &value);
         
         if (error)
            LOG_ERROR(error);

         if (!value.empty())
            return safe_convert::stringTo<bool>(value, false);
         else
            return false;
      }
      else
      {
         // if empty, we are likely in desktop mode (as we have no specified scratch path)
         // in this default case, we want initial to be true, since every time the session
         // is started, we should start in the default working directory
         return true;
      }
   }

   void setInitial(bool initial)
   {
      if (!empty())
      {
         std::string value = safe_convert::numberToString(initial);
         Error error = storage_->writeProperty(id_, kInitial, value);
         if (error)
            LOG_ERROR(error);
      }
   }

   double lastUsed() const
   {
      return timestampProperty(kLastUsed);
   }

   void setLastUsed()
   {
      setTimestampProperty(kLastUsed);
   }

   bool executing() const
   {
      if (!empty())
      {
         std::string value = "";
         Error error = storage_->readProperty(id_, kExecuting, &value);

         if (error)
            LOG_ERROR(error);

         if (!value.empty())
            return safe_convert::stringTo<bool>(value, false);
         else
            return false;
      }
      else
         return false;
   }

   void setExecuting(bool executing)
   {
      if (!empty())
      {
         std::string value = safe_convert::numberToString(executing);
         Error error = storage_->writeProperty(id_, kExecuting, value);

         if (error)
            LOG_ERROR(error);
      }
   }

   bool savePromptRequired() const
   {
      if (!empty())
      {
         std::string value = "";
         Error error = storage_->readProperty(id_, kSavePromptRequired, &value);

         if (error)
            LOG_ERROR(error);

         if (!value.empty())
            return safe_convert::stringTo<bool>(value, false);
         else
            return false;
      }
      else
         return false;
   }

   void setSavePromptRequired(bool savePromptRequired)
   {
      if (!empty())
      {
         std::string value = safe_convert::numberToString(savePromptRequired);
         Error error = storage_->writeProperty(id_, kSavePromptRequired, value);

         if (error)
            LOG_ERROR(error);
      }
   }


   bool running() const
   {
      if (!empty())
      {
         std::string value = "";
         Error error = storage_->readProperty(id_, kRunning, &value);

         if (error)
            LOG_ERROR(error);

         if (!value.empty())
            return safe_convert::stringTo<bool>(value, false);
         else
            return false;
      }
      else
         return false;
   }

   std::string rVersion()
   {
      std::string value;
      if (!empty())
      {  
         Error error = storage_->readProperty(id_, kRVersion, &value);

         if (error)
            LOG_ERROR(error);
      }
      return value;
   }

   std::string rVersionLabel()
   {
      std::string value;
      if (!empty())
      {
         Error error = storage_->readProperty(id_, kRVersionLabel, &value);

         if (error)
            LOG_ERROR(error);
         
      }
      return value;
   }

   std::string rVersionHome()
   {
      std::string value;
      if (!empty())
      {
         Error error = storage_->readProperty(id_, kRVersionHome, &value);
         
         if (error)
            LOG_ERROR(error);
      }
      return std::string();
   }

   void setRVersion(const std::string& rVersion,
                    const std::string& rVersionHome,
                    const std::string& rVersionLabel = "")
   {
      if (!empty())
      {
         Error error = storage_->writeProperty(id_, kRVersion, rVersion);

         if (error)
            LOG_ERROR(error);

         error = storage_->writeProperty(id_, kRVersionHome, rVersionHome);

         if (error)
            LOG_ERROR(error);

         error = storage_->writeProperty(id_, kRVersionLabel, rVersionLabel);

         if (error)
            LOG_ERROR(error);
      }
   }

   // historical note: this will be displayed as the session name
   std::string label()
   {
      std::string value;
      if (!empty())
      {
         Error error = storage_->readProperty(id_, kLabel, &value);
         
         if (error)
            LOG_ERROR(error);
      }
      return value;
   }

   // historical note: this will be displayed as the session name
   void setLabel(const std::string& label)
   {
      if (!empty())
      {
         Error error = storage_->writeProperty(id_, kLabel, label);

         if (error)
            LOG_ERROR(error);
      }
   }

   void beginSession(const std::string& rVersion,
                     const std::string& rVersionHome,
                     const std::string& rVersionLabel = "")
   {
      setLastUsed();
      setRunning(true);
      setRVersion(rVersion, rVersionHome, rVersionLabel);
   }

   void endSession()
   {
      setLastUsed();
      setRunning(false);
      setExecuting(false);
   }

   core::Error destroy()
   {
      if (!empty())
         return scratchPath_.removeIfExists();
      else
         return Success();
   }

   bool validate(const FilePath& userHomePath,
                 bool projectSharingEnabled) const
   {
      // ensure the scratch path and properties paths exist
      if (!scratchPath_.exists() || !propertiesPath_.exists())
         return false;

      // ensure the properties are there
      if (project().empty() || workingDir().empty() || (lastUsed() == 0))
          return false;

      // for projects validate that the base directory still exists
      std::string theProject = project();
      if (theProject != kProjectNone)
      {
         FilePath projectDir = FilePath::resolveAliasedPath(theProject,
                                                            userHomePath);
         if (!projectDir.exists())
            return false;

        // check for project file
        FilePath projectPath = r_util::projectFromDirectory(projectDir);
        if (!projectPath.exists())
           return false;

        // if we got this far the scope is valid, do one final check for
        // trying to open a shared project if sharing is disabled
        if (!projectSharingEnabled &&
            r_util::isSharedPath(projectPath.getAbsolutePath(), userHomePath))
           return false;
      }

      // validated!
      return true;
   }
   
   bool operator>(const ActiveSession& rhs) const
   {
      if (sortConditions_.executing_ == rhs.sortConditions_.executing_)
      {
         if (sortConditions_.running_ == rhs.sortConditions_.running_)
         {
            if (sortConditions_.lastUsed_ == rhs.sortConditions_.lastUsed_)
               return id() > rhs.id();

            return sortConditions_.lastUsed_ > rhs.sortConditions_.lastUsed_;
         }

         return sortConditions_.running_;
      }
      
      return sortConditions_.executing_;
   }

 private:
   struct SortConditions
   {
      SortConditions() :
         executing_(false),
         running_(false),
         lastUsed_(0)
      {
         
      }

      bool executing_;
      bool running_;
      double lastUsed_;
   };

   void cacheSortConditions()
   {
      sortConditions_.executing_ = executing();
      sortConditions_.running_ = running();
      sortConditions_.lastUsed_ = lastUsed();
   }
 

   void setTimestampProperty(const std::string& property)
   {
      if (!empty())
      {
         double now = date_time::millisecondsSinceEpoch();
         std::string value = safe_convert::numberToString(now);
        storage_->writeProperty(id_, property, value);
      }
   }

   double timestampProperty(const std::string& property) const
   {
      if (!empty())
      {
         std::string value = "";
         Error error = storage_->readProperty(id_, property, &value);

         if (error)
            LOG_ERROR(error);

         if (!value.empty())
            return safe_convert::stringTo<double>(value, 0);
         else
            return 0;
      }
      else
      {
         return 0;
      }
   }


   void setRunning(bool running)
   {
      if (!empty())
      {
         std::string value = safe_convert::numberToString(running);
         Error error = storage_->writeProperty(id_, kRunning, value);

         if(error) 
            LOG_ERROR(error);
      }
   }

   std::shared_ptr<IActiveSessionStorage> storage_;
   std::string id_;
   FilePath scratchPath_;
   FilePath propertiesPath_;
   SortConditions sortConditions_;
};

class ActiveSessions : boost::noncopyable
{
public:
   explicit ActiveSessions(const FilePath& rootStoragePath)
   {
      storagePath_ = storagePath(rootStoragePath);
      storage_ = ActiveSessionStorageFactory::getActiveSessionStorage();
      Error error = storagePath_.ensureDirectory();
      if (error)
         LOG_ERROR(error);
   }

   static FilePath storagePath(const FilePath& rootStoragePath)
   {
      return rootStoragePath.completeChildPath("sessions/active");
   }

   core::Error create(const std::string& project,
                      const std::string& working,
                      std::string* pId) const
   {
      return create(project, working, true, pId);
   }

   core::Error create(const std::string& project,
                      const std::string& working,
                      bool initial,
                      std::string* pId) const;

   std::vector<boost::shared_ptr<ActiveSession> > list(
                                    const FilePath& userHomePath,
                                    bool projectSharingEnabled) const;

   size_t count(const FilePath& userHomePath,
                bool projectSharingEnabled) const;

   boost::shared_ptr<ActiveSession> get(const std::string& id) const;

   FilePath storagePath() const { return storagePath_; }

   boost::shared_ptr<ActiveSession> emptySession(
      const std::string& id) const;

private:
   core::FilePath storagePath_;
   std::shared_ptr<IActiveSessionStorage> storage_;
};

// active session as tracked by rserver processes
// these are stored in a common location per rserver
// so that the server process can keep track of all
// active sessions, regardless of users running them
class GlobalActiveSession : boost::noncopyable
{
public:
   explicit GlobalActiveSession(const FilePath& path) : filePath_(path)
   {
      settings_.initialize(filePath_);
   }

   virtual ~GlobalActiveSession() {}

   std::string sessionId() { return settings_.get("sessionId", ""); }
   void setSessionId(const std::string& sessionId) { settings_.set("sessionId", sessionId); }

   std::string username() { return settings_.get("username", ""); }
   void setUsername(const std::string& username) { settings_.set("username", username); }

   std::string userHomeDir() { return settings_.get("userHomeDir", ""); }
   void setUserHomeDir(const std::string& userHomeDir) { settings_.set("userHomeDir", userHomeDir); }

   int sessionTimeoutKillHours() { return settings_.getInt("sessionTimeoutKillHours", 0); }
   void setSessionTimeoutKillHours(int val) { settings_.set("sessionTimeoutKillHours", val); }

   core::Error destroy() { return filePath_.removeIfExists(); }

private:
   core::Settings settings_;
   core::FilePath filePath_;
};

class GlobalActiveSessions : boost::noncopyable
{
public:
   explicit GlobalActiveSessions(const FilePath& rootPath) : rootPath_(rootPath) {}
   std::vector<boost::shared_ptr<GlobalActiveSession> > list() const;
   boost::shared_ptr<GlobalActiveSession> get(const std::string& id) const;

private:
   core::FilePath rootPath_;
};

void trackActiveSessionCount(std::shared_ptr<IActiveSessionStorage> storage,
                             const FilePath& rootStoragePath,
                             const FilePath& userHomePath,
                             bool projectSharingEnabled,
                             boost::function<void(size_t)> onCountChanged);


} // namespace r_util
} // namespace core
} // namespace rstudio

#endif // CORE_R_UTIL_ACTIVE_SESSIONS_HPP
