/*
 * RActiveSessions.cpp
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

#include <core/r_util/RActiveSessions.hpp>

#include <boost/bind/bind.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/r_util/RActiveSessionsStorage.hpp>
#include <core/Log.hpp>
#include <core/StringUtils.hpp>
#include <core/FileSerializer.hpp>
#include <core/system/System.hpp>
#include <core/system/FileMonitor.hpp>
#include <core/r_util/RSessionContext.hpp>

#include <shared_core/SafeConvert.hpp>

using namespace boost::placeholders;

namespace rstudio {
namespace core {
namespace r_util {

ActiveSessions::ActiveSessions(const FilePath& rootStoragePath) : ActiveSessions(std::make_shared<FileActiveSessionsStorage>(FileActiveSessionsStorage(rootStoragePath)))
{
}

ActiveSessions::ActiveSessions(const std::shared_ptr<IActiveSessionsStorage> storage)
   : storage_(storage)
{

}

Error ActiveSessions::create(const std::string& project,
                             const std::string& workingDir,
                             bool initial,
                             std::string* pId) const
{
   // generate a new id (loop until we find a unique one)
   std::string id;
   while (id.empty())
   {
      std::string candidateId = core::r_util::generateScopeId();
      if (!storage_->hasSessionId(candidateId))
         id = candidateId;
   }

   boost::posix_time::ptime time = boost::posix_time::second_clock::universal_time();
   std::string isoTime = boost::posix_time::to_iso_extended_string(time);

   //Initial settings
   std::map<std::string, std::string> initialMetadata = {
      {"project", project},
      {"working_dir", workingDir},
      {"initial", initial ? "true" : "false"},
      {"running", "false"},
      {"last_used", isoTime},
      {"running", "false"},
      {"created", isoTime},
      {"launch_parameters", ""}
   };

   storage_->createSession(id, initialMetadata);

   // return the id if requested
   if (pId != nullptr)
   {
      *pId = id;
   }
   return Success();
}

std::vector<boost::shared_ptr<ActiveSession> > ActiveSessions::list(FilePath userHomePath, bool projectSharingEnabled) const
{
   return storage_->listSessions(userHomePath, projectSharingEnabled);
}

size_t ActiveSessions::count(const FilePath& userHomePath,
                             bool projectSharingEnabled) const
{
   return storage_->getSessionCount(userHomePath, projectSharingEnabled);
}

boost::shared_ptr<ActiveSession> ActiveSessions::get(const std::string& id) const
{
   return storage_->getSession(id);
}


boost::shared_ptr<ActiveSession> ActiveSessions::emptySession(const std::string& id) const
{
   return boost::shared_ptr<ActiveSession>(new ActiveSession(id));
}

namespace {

void notifyCountChanged(boost::shared_ptr<ActiveSessions> pSessions,
                        const FilePath& userHomePath,
                        bool projectSharingEnabled,
                        boost::function<void(size_t)> onCountChanged)
{
   onCountChanged(pSessions->count(userHomePath, projectSharingEnabled));
}

} // anonymous namespace

void trackActiveSessionCount(const FilePath& rootStoragePath,
                             const FilePath& userHomePath,
                             bool projectSharingEnabled,
                             boost::function<void(size_t)> onCountChanged)
{

   boost::shared_ptr<ActiveSessions> pSessions(
                                          new ActiveSessions(rootStoragePath));

   core::system::file_monitor::Callbacks cb;
   cb.onRegistered = boost::bind(notifyCountChanged,
                                 pSessions,
                                 userHomePath,
                                 projectSharingEnabled,
                                 onCountChanged);
   cb.onFilesChanged = boost::bind(notifyCountChanged,
                                   pSessions,
                                   userHomePath,
                                   projectSharingEnabled,
                                   onCountChanged);
   cb.onRegistrationError = boost::bind(log::logError, _1, ERROR_LOCATION);

   core::system::file_monitor::registerMonitor(
                   buildActiveSessionStoragePath(rootStoragePath),
                   false,
                   boost::function<bool(const FileInfo&)>(),
                   cb);
}

} // namespace r_util
} // namespace core
} // namespace rstudio
