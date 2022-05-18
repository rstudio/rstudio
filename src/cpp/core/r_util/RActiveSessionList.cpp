/*
 * RActiveSessionList.cpp
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

#include <core/r_util/RActiveSessionList.hpp>

#include <boost/bind/bind.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/r_util/RActiveSessionListStorage.hpp>
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

ActiveSessionList::ActiveSessionList(const FilePath& rootStoragePath) : 
   ActiveSessionList(std::make_shared<FileActiveSessionListStorage>(FileActiveSessionListStorage(rootStoragePath)), rootStoragePath)
{
   
}

ActiveSessionList::ActiveSessionList(const std::shared_ptr<IActiveSessionListStorage> storage, const FilePath& rootStoragePath)
   : rootStoragePath_(rootStoragePath), storage_(storage)
{

}

Error ActiveSessionList::create(const std::string& project,
                             const std::string& workingDir,
                             bool initial,
                             const std::string& editor,
                             std::string* pId) const
{
   // generate a new id (loop until we find a unique one)
   std::string id;
   FilePath dir;
   while (id.empty())
   {
      std::string candidateId = core::r_util::generateScopeId();
      dir = rootStoragePath_.completeChildPath(kSessionDirPrefix + candidateId);
      if (!storage_->hasSessionId(candidateId) && !dir.exists())
         id = candidateId;
   }

   LOG_DEBUG_MESSAGE("Creating new session directory: " + dir.getAbsolutePath() + " for editor: " + editor + " with id: " + id);

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

   boost::shared_ptr<ActiveSession> activeSession = storage_->getSession(id);
   activeSession->setActivityState(kActivityStateLaunching, true);

   if (editor == kWorkbenchRStudio)
      activeSession->setLastResumed();

   // return the id if requested
   if (pId != nullptr)
   {
      *pId = id;
   }
   return Success();
}

std::vector<boost::shared_ptr<ActiveSession> > ActiveSessionList::list(FilePath userHomePath, bool projectSharingEnabled) const
{
   std::vector<std::string> sessionIds = storage_->listSessionIds();
   std::vector<boost::shared_ptr<ActiveSession>> sessions{};
   for(std::string id : sessionIds)
   {
      boost::shared_ptr<ActiveSession> candidateSession = storage_->getSession(id);
      if(candidateSession->validate(userHomePath, projectSharingEnabled))
      {
         candidateSession->cacheSortConditions();
         sessions.push_back(candidateSession);
      }
      else
      {
         // Logging a message because this also happens when the rworkspaces cannot access the project directory
         LOG_DEBUG_MESSAGE("Removing invalid session: " + candidateSession->id());

         // remove sessions that don't have required properties
         // (they may be here as a result of a race condition where
         // they are removed but then suspended session data is
         // written back into them)
         Error error = candidateSession->destroy();
         if (error)
            LOG_ERROR(error);
      }
   }
   return sessions;
}

size_t ActiveSessionList::count(const FilePath& userHomePath,
                             bool projectSharingEnabled) const
{
   return storage_->getSessionCount();
}

boost::shared_ptr<ActiveSession> ActiveSessionList::get(const std::string& id) const
{
   FilePath scratchPath = rootStoragePath_.completeChildPath(kSessionDirPrefix + id);
   if (scratchPath.exists() && storage_->hasSessionId(id))
   {
      
      LOG_DEBUG_MESSAGE("Found session: " + scratchPath.getAbsolutePath());
      return storage_->getSession(id);
   }
   else
   {
      LOG_DEBUG_MESSAGE("No session with path: " + scratchPath.getAbsolutePath());
      return emptySession(id);
   }
}


boost::shared_ptr<ActiveSession> ActiveSessionList::emptySession(const std::string& id) const
{
   return boost::shared_ptr<ActiveSession>(new ActiveSession(id));
}

namespace {

void notifyCountChanged(boost::shared_ptr<ActiveSessionList> pSessions,
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

   boost::shared_ptr<ActiveSessionList> pSessions(
                                          new ActiveSessionList(rootStoragePath));

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
