/*
 * RActiveSessions.hpp
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Log.hpp>
#include <core/Settings.hpp>
#include <core/DateTime.hpp>
#include <core/SafeConvert.hpp>

#include <core/r_util/RSessionContext.hpp>
#include <core/r_util/RProjectFile.hpp>

namespace rstudio {
namespace core {
namespace r_util {

class ActiveSession : boost::noncopyable
{
private:
   friend class ActiveSessions;
   ActiveSession() {}

   explicit ActiveSession(const std::string& id) : id_(id) 
   {
   }

   explicit ActiveSession(const std::string& id, const FilePath& scratchPath)
      : id_(id), scratchPath_(scratchPath)
   {
      core::Error error = scratchPath_.ensureDirectory();
      if (error)
         LOG_ERROR(error);

      propertiesPath_ = scratchPath_.childPath("properites");
      error = propertiesPath_.ensureDirectory();
      if (error)
         LOG_ERROR(error);
   }

public:

   bool empty() const { return scratchPath_.empty(); }

   std::string id() const { return id_; }

   const FilePath& scratchPath() const { return scratchPath_; }

   std::string project() const
   {
      if (!empty())
         return readProperty("project");
      else
         return std::string();
   }

   void setProject(const std::string& project)
   {
      if (!empty())
         writeProperty("project", project);
   }

   std::string workingDir() const
   {
      if (!empty())
         return readProperty("working-dir");
      else
         return std::string();
   }

   void setWorkingDir(const std::string& workingDir)
   {
      if (!empty())
         writeProperty("working-dir", workingDir);
   }

   bool initial() const
   {
      if (!empty())
      {
         std::string value = readProperty("initial");
         if (!value.empty())
            return safe_convert::stringTo<bool>(value, false);
         else
            return false;
      }
      else
         return false;
   }

   void setInitial(bool initial)
   {
      if (!empty())
      {
         std::string value = safe_convert::numberToString(initial);
         writeProperty("initial", value);
      }
   }

   double lastUsed() const
   {
      if (!empty())
      {
         std::string value = readProperty("last-used");
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

   void setLastUsed()
   {
      if (!empty())
      {
         double now = date_time::millisecondsSinceEpoch();
         std::string value = safe_convert::numberToString(now);
         writeProperty("last-used", value);
      }
   }

   bool executing() const
   {
      if (!empty())
      {
         std::string value = readProperty("executing");
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
         writeProperty("executing", value);
      }
   }

   bool savePromptRequired() const
   {
      if (!empty())
      {
         std::string value = readProperty("save_prompt_required");
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
         writeProperty("save_prompt_required", value);
      }
   }


   bool running() const
   {
      if (!empty())
      {
         std::string value = readProperty("running");
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
      if (!empty())
         return readProperty("r-version");
      else
         return std::string();
   }

   std::string rVersionHome()
   {
      if (!empty())
         return readProperty("r-version-home");
      else
         return std::string();
   }

   void setRVersion(const std::string& rVersion,
                    const std::string& rVersionHome)
   {
      if (!empty())
      {
         writeProperty("r-version", rVersion);
         writeProperty("r-version-home", rVersionHome);
      }
   }

   std::string label()
   {
      if (!empty())
         return readProperty("label");
      else
         return std::string();
   }

   void setLabel(const std::string& label)
   {
      if (!empty())
         writeProperty("label", label);
   }

   void beginSession(const std::string& rVersion,
                     const std::string& rVersionHome)
   {
      setLastUsed();
      setRunning(true);
      setRVersion(rVersion, rVersionHome);
   }

   void endSession()
   {
      setLastUsed();
      setRunning(false);
      setExecuting(false);
   }

   uintmax_t suspendSize()
   {
      FilePath suspendPath = scratchPath_.complete("suspended-session-data");
      if (!suspendPath.exists())
         return 0;

      return suspendPath.sizeRecursive();
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
            r_util::isSharedPath(projectPath.absolutePath(), userHomePath))
           return false;
      }

      // validated!
      return true;
   }

private:

   void setRunning(bool running)
   {
      if (!empty())
      {
         std::string value = safe_convert::numberToString(running);
         writeProperty("running", value);
      }
   }

   void writeProperty(const std::string& name, const std::string& value) const;
   std::string readProperty(const std::string& name) const;

private:
   std::string id_;
   FilePath scratchPath_;
   FilePath propertiesPath_;
};


class ActiveSessions : boost::noncopyable
{
public:
   explicit ActiveSessions(const FilePath& rootStoragePath)
   {
      storagePath_ = rootStoragePath.childPath("sessions/active");
      Error error = storagePath_.ensureDirectory();
      if (error)
         LOG_ERROR(error);
   }

   core::Error create(const std::string& project,
                      const std::string& working,
                      std::string* pId) const
   {
      return create(project, working, false, pId);
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

   static boost::shared_ptr<ActiveSession> emptySession(const std::string& id);

private:
   core::FilePath storagePath_;
};

void trackActiveSessionCount(const FilePath& rootStoragePath,
                             const FilePath& userHomePath,
                             bool projectSharingEnabled,
                             boost::function<void(size_t)> onCountChanged);


} // namespace r_util
} // namespace core
} // namespace rstudio

#endif // CORE_R_UTIL_ACTIVE_SESSIONS_HPP
