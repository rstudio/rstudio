/*
 * RActiveSessions.hpp
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


#ifndef CORE_R_UTIL_ACTIVE_SESSIONS_HPP
#define CORE_R_UTIL_ACTIVE_SESSIONS_HPP

#include <boost/noncopyable.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/Log.hpp>
#include <core/Settings.hpp>
#include <core/DateTime.hpp>

namespace rstudio {
namespace core {
namespace r_util {

class ActiveSession : boost::noncopyable
{
private:
   friend class ActiveSessions;
   ActiveSession() {}
   explicit ActiveSession(const std::string& id, const FilePath& scratchPath)
      : id_(id), scratchPath_(scratchPath)
   {
      core::Error error = scratchPath_.ensureDirectory();
      if (error)
         LOG_ERROR(error);

      error = properties_.initialize(scratchPath_.childPath("properites"));
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
         return properties_.get("project");
      else
         return std::string();
   }

   void setProject(const std::string& project)
   {
      if (!empty())
         properties_.set("project", project);
   }

   std::string workingDir() const
   {
      if (!empty())
         return properties_.get("working-dir");
      else
         return std::string();
   }

   void setWorkingDir(const std::string& workingDir)
   {
      if (!empty())
         properties_.set("working-dir", workingDir);
   }

   double lastUsed() const
   {
      if (!empty())
         return properties_.getDouble("last-used");
      else
         return 0;
   }

   void setLastUsed()
   {
      if (!empty())
         properties_.set("last-used", date_time::millisecondsSinceEpoch());
   }

   bool running() const
   {
      if (!empty())
         return properties_.getBool("running");
      else
         return false;
   }

   void setRunning(bool running)
   {
      if (!empty())
         properties_.set("running", running);
   }

   core::Error destroy()
   {
      if (!empty())
         return scratchPath_.removeIfExists();
      else
         return Success();
   }

   bool hasRequiredProperties() const
   {
      return !project().empty() &&
             !workingDir().empty();
   }

private:
   std::string id_;
   FilePath scratchPath_;
   Settings properties_;
};


class ActiveSessions : boost::noncopyable
{
public:
   explicit ActiveSessions(const FilePath& rootStoragePath)
   {
      storagePath_ = rootStoragePath.childPath("session-storage");
      Error error = storagePath_.ensureDirectory();
      if (error)
         LOG_ERROR(error);
   }

   core::Error create(const std::string& project,
                      const std::string& working,
                      std::string* pId) const;

   std::vector<boost::shared_ptr<ActiveSession> > list() const;

   size_t count() const;

   boost::shared_ptr<ActiveSession> get(const std::string& id) const;

   FilePath storagePath() const { return storagePath_; }

   static boost::shared_ptr<ActiveSession> emptySession();

private:
   core::FilePath storagePath_;
};

void trackActiveSessionCount(const FilePath& rootStoragePath,
                             boost::function<void(size_t)> onCountChanged);


} // namespace r_util
} // namespace core
} // namespace rstudio

#endif // CORE_R_UTIL_ACTIVE_SESSIONS_HPP
