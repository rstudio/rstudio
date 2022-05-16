/*
 * RActiveSession.hpp
 *
 * Copyright (C) 2022 by RStudio, PBC
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


#ifndef CORE_R_UTIL_ACTIVE_SESSION_HPP
#define CORE_R_UTIL_ACTIVE_SESSION_HPP

#include <boost/noncopyable.hpp>

#include <core/Log.hpp>
#include <core/Settings.hpp>
#include <core/DateTime.hpp>

#include <core/r_util/RSessionContext.hpp>
#include <core/r_util/RProjectFile.hpp>
#include <core/r_util/RActiveSessionStorage.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <shared_core/SafeConvert.hpp>
#include <shared_core/json/Json.hpp>

namespace rstudio {
namespace core {
namespace r_util {

class ActiveSession : boost::noncopyable
{
public:
   explicit ActiveSession(const std::string& id) : id_(id) 
   {
   }

   explicit ActiveSession(
      const std::string& id,
      const FilePath& scratchPath,
      std::shared_ptr<IActiveSessionStorage> storage) : id_(id), scratchPath_(scratchPath), storage_(storage)
   {
   }

   const std::string kExecuting = "executing";
   const std::string kInitial = "initial";
   const std::string kLabel = "label";
   const std::string kLastUsed = "last_used";
   const std::string kProject = "project";
   const std::string kSavePromptRequired = "save_prompt_required";
   const std::string kRunning = "running";
   const std::string kRVersion = "r_version";
   const std::string kRVersionHome = "r_version_home";
   const std::string kRVersionLabel = "r_version_label";
   const std::string kWorkingDir = "working_dir";
   const std::string kLastResumed = "last_resumed";
   const std::string kSuspendTimestamp = "suspend_timestamp";
   const std::string kBlockingSuspend = "blocking_suspend";

   bool empty() const
   { 
      bool empty = true;
      storage_->isEmpty(&empty);
      return empty;
   }

   std::string id() const
   {
      return id_;
   }

   std::string readProperty(const std::string& propertyName) const
   {
      std::string value;
      if (!empty())
      {
         Error error = storage_->readProperty(propertyName, &value);
         if (error)
            LOG_ERROR(error);
      }

      return value;
   }

   void writeProperty(const std::string& propertyName, const std::string& value)
   {
      if (!empty())
      {
         Error error = storage_->writeProperty(propertyName, value);
         if (error)
            LOG_ERROR(error);
      }
   }

   std::string project() const
   {
      return readProperty(kProject);
   }

   void setProject(const std::string& project) 
   {
      writeProperty(kProject, project);
   }

   std::string workingDir() const
   {
      return readProperty(kWorkingDir);
   }

   void setWorkingDir(const std::string& workingDir)
   {
      writeProperty(kWorkingDir, workingDir);
   }

   bool initial() const
   {
      if (!empty())
      {
         std::string value = readProperty(kInitial);

         if (!value.empty())
         {
            return safe_convert::stringTo<bool>(value, false);
         }
         else
         {
            return false;
         }
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
      std::string value = safe_convert::numberToString(initial);
      writeProperty(kInitial, value);
   }

   void setBlockingSuspend(json::Array blocking)
   {
      writeProperty(kBlockingSuspend, blocking.writeFormatted());
   }

   boost::posix_time::ptime suspensionTime() const
   {
      return ptimeTimestampProperty(kSuspendTimestamp);
   }

   void setSuspensionTime(const boost::posix_time::ptime value = boost::posix_time::second_clock::universal_time())
   {
      setPtimeTimestampProperty(kSuspendTimestamp, value);
   }

   boost::posix_time::ptime lastResumed() const
   {
      return ptimeTimestampProperty(kLastResumed);
   }

   void setLastResumed(const boost::posix_time::ptime value = boost::posix_time::second_clock::universal_time())
   {
      setPtimeTimestampProperty(kLastResumed, value);
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
      std::string value = readProperty(kExecuting);

      if (!value.empty())
         return safe_convert::stringTo<bool>(value, false);
      else
         return false;
   }

   void setExecuting(bool executing)
   {
      std::string value = safe_convert::numberToString(executing);
      writeProperty(kExecuting, value);
   }

   bool savePromptRequired() const
   {
      std::string value = readProperty(kSavePromptRequired);

      if (!value.empty())
         return safe_convert::stringTo<bool>(value, false);
      else
         return false;
   }

   void setSavePromptRequired(bool savePromptRequired)
   {
         std::string value = safe_convert::numberToString(savePromptRequired);
         writeProperty(kSavePromptRequired, value);
   }


   bool running() const
   {
      std::string value = readProperty(kRunning);

      if (!value.empty())
      {
         return safe_convert::stringTo<bool>(value, false);
      }
      else
      {
         return false;
      }
   }

   std::string rVersion()
   {
      return readProperty(kRVersion);
   }

   std::string rVersionLabel()
   {
      return readProperty(kRVersionLabel);
   }

   std::string rVersionHome()
   {
      return readProperty(kRVersionHome);
   }

   void setRVersion(const std::string& rVersion,
                    const std::string& rVersionHome,
                    const std::string& rVersionLabel = "")
   {
         writeProperty(kRVersion, rVersion);
         writeProperty(kRVersionHome, rVersionHome);
         writeProperty(kRVersionLabel, rVersionLabel);
   }

   // historical note: this will be displayed as the session name
   std::string label()
   {
      return readProperty(kLabel);
   }

   // historical note: this will be displayed as the session name
   void setLabel(const std::string& label)
   {
      writeProperty(kLabel, label);
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

   uintmax_t suspendSize()
   {
      FilePath suspendPath = scratchPath_.completePath("suspended-session-data");
      if (!suspendPath.exists())
         return 0;

      return suspendPath.getSizeRecursive();
   }

   core::Error destroy()
   {
      if (!empty())
         return storage_->destroy();
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

   

            return sortConditions_.lastUsed_ > rhs.sortConditions_.lastUsed_;
         }

         return sortConditions_.running_;
      }
      
      return sortConditions_.executing_;
   }
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

   FilePath scratchPath() const
   {
      return scratchPath_;
   }

   private:

   void setTimestampProperty(const std::string& property)
   {
      double now = date_time::millisecondsSinceEpoch();
      std::string value = safe_convert::numberToString(now);
      writeProperty(property, value);
   }

   double timestampProperty(const std::string& property) const
   {
      std::string value = readProperty(property);

      if (!value.empty())
         return safe_convert::stringTo<double>(value, 0);
      else
         return 0;
   }

   void setPtimeTimestampProperty(const std::string& property, const boost::posix_time::ptime& time)
   {
      if (!empty())
      {
         std::string suspendTime = boost::posix_time::to_iso_extended_string(time);
         writeProperty(property, suspendTime);
      }
   }

   boost::posix_time::ptime ptimeTimestampProperty(const std::string property) const
   {
      if (!empty())
      {
         std::string value = "Value Not Read";
         try
         {
            value = readProperty(property);
            if (value.empty())
               return boost::posix_time::not_a_date_time;

            // posix_time::from_iso_extended_string can't parse not_a_date_time correctly, so handling it here
            if (value == boost::posix_time::to_iso_extended_string(boost::posix_time::not_a_date_time))
               return boost::posix_time::not_a_date_time;

            boost::posix_time::ptime retVal = boost::posix_time::from_iso_extended_string(value);

            if (retVal.is_not_a_date_time())
               return boost::posix_time::not_a_date_time;

            return retVal;
         }
         catch (std::exception const& e)
         {
            LOG_INFO_MESSAGE("Failure reading property " + property + ": " + std::string(e.what()) + ". Property contents: " + value);
         }
      }
      return boost::posix_time::not_a_date_time;
   }

   void setRunning(bool running)
   {
         std::string value = safe_convert::numberToString(running);
         writeProperty(kRunning, value);
   }

   std::string id_;
   FilePath scratchPath_;
   std::shared_ptr<IActiveSessionStorage> storage_;
   FilePath propertiesPath_;
   SortConditions sortConditions_;
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

} // namespace r_util
} // namespace core
} // namespace rstudio

#endif // CORE_R_UTIL_R_ACTIVE_SESSION_STORAGE
