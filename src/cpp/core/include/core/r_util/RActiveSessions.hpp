/*
 * RActiveSessions.hpp
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


#ifndef CORE_R_UTIL_ACTIVE_SESSIONS_HPP
#define CORE_R_UTIL_ACTIVE_SESSIONS_HPP

#include <boost/noncopyable.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>
#include <core/Settings.hpp>
#include <core/r_util/RActiveSession.hpp>

namespace rstudio {
namespace core {
namespace r_util {
class ActiveSessions : boost::noncopyable
{
public:
   explicit ActiveSessions(const FilePath& rootStoragePath);

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

void trackActiveSessionCount(const FilePath& rootStoragePath,
                             const FilePath& userHomePath,
                             bool projectSharingEnabled,
                             boost::function<void(size_t)> onCountChanged);

} // namespace r_util
} // namespace core
} // namespace rstudio

#endif // CORE_R_UTIL_ACTIVE_SESSIONS_HPP
