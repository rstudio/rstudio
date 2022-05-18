/*
 * RActiveSessionList.hpp
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

#include <core/Settings.hpp>
#include <core/r_util/RActiveSession.hpp>
#include <core/r_util/RActiveSessionListStorage.hpp>

#include <shared_core/Error.hpp>
#include <shared_core/FilePath.hpp>

namespace rstudio {
namespace core {
namespace r_util {

class ActiveSessionList : boost::noncopyable
{
public:
   explicit ActiveSessionList(const FilePath& rootStoragePath);
   explicit ActiveSessionList(const std::shared_ptr<IActiveSessionListStorage> storage, const FilePath& rootStoragePath);

   core::Error create(const std::string& project,
                      const std::string& working,
                      std::string* pId) const
   {
      return create(project, working, true, kWorkbenchRStudio, pId);
   }

   core::Error create(const std::string& project,
                      const std::string& working,
                      bool initial,
                      const std::string& editor,
                      std::string* pId) const;

   std::vector<boost::shared_ptr<ActiveSession> > list(FilePath userHomePath, bool projectSharingEnabled) const;

   size_t count(const FilePath& userHomePath,
                bool projectSharingEnabled) const;

   boost::shared_ptr<ActiveSession> get(const std::string& id) const;

   boost::shared_ptr<ActiveSession> emptySession(const std::string& id) const;

private:
   FilePath rootStoragePath_;
   std::shared_ptr<IActiveSessionListStorage> storage_;
};

class GlobalActiveSessionList : boost::noncopyable
{
public:
   explicit GlobalActiveSessionList(const FilePath& rootPath) : rootPath_(rootPath) {}
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
