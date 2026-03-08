/*
 * RActiveSessions.cpp
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
#include <boost/make_shared.hpp>

#include <core/Log.hpp>
#include <core/StringUtils.hpp>
#include <core/FileSerializer.hpp>
#include <core/r_util/RActiveSessionsStorage.hpp>
#include <core/r_util/RSessionContext.hpp>
#include <core/system/FileMonitor.hpp>
#include <core/system/System.hpp>

#include <shared_core/SafeConvert.hpp>

using namespace boost::placeholders;

namespace rstudio {
namespace core {
namespace r_util {
namespace {


} // anonymous namespace

void ActiveSession::writeJsonProperty(const std::string& name, const core::json::Value& value) const
{
   writeProperty(name, value.writeFormatted());
}

core::json::Value ActiveSession::readJsonProperty(const std::string& name) const
{
   json::Value value;
   std::string s = readProperty(name);
   if (s.empty())
      return json::Value();
   if (value.parse(s))
   {
      LOG_WARNING_MESSAGE("failed to parse property '" + name + "' of session " + id_);
   }
   return value;
}

const std::string ActiveSession::kCreated = "created";
const std::string ActiveSession::kExecuting = "executing";
const std::string ActiveSession::kInitial = "initial";
const std::string ActiveSession::kLastUsed = "last_used";
const std::string ActiveSession::kLabel = "label";
const std::string ActiveSession::kProject = "project";
const std::string ActiveSession::kProjectId = "project_id";
const std::string ActiveSession::kSavePromptRequired = "save_prompt_required";
const std::string ActiveSession::kRunning = "running";
const std::string ActiveSession::kRVersion = "r_version";
const std::string ActiveSession::kRVersionHome = "r_version_home";
const std::string ActiveSession::kRVersionLabel = "r_version_label";
const std::string ActiveSession::kWorkingDir = "working_directory";
const std::string ActiveSession::kActivityState = "activity_state";
const std::string ActiveSession::kLastStateUpdated = "last_state_updated";
const std::string ActiveSession::kEditor = "editor";
const std::string ActiveSession::kLastResumed = "last_resumed";
const std::string ActiveSession::kSuspendTimestamp = "suspend_timestamp";
const std::string ActiveSession::kBlockingSuspend = "blocking_suspend";
const std::string ActiveSession::kLaunchParameters = "launch_parameters";
const std::string ActiveSession::kSuspendSize = "suspend_size";
const std::string ActiveSession::kWorkbench = "workbench";
const std::string ActiveSession::kId = "id";
const std::string ActiveSession::kDisplayName = "display_name";

ActiveSessions::ActiveSessions(std::shared_ptr<IActiveSessionsStorage> storage, const FilePath& rootStoragePath) :
   storage_(storage)
{
   storagePath_ = storagePath(rootStoragePath);
}

ActiveSessions::ActiveSessions(std::shared_ptr<IActiveSessionsStorage> storage) :
   storage_(storage)
{
}

Error ActiveSessions::create(const std::string& project,
                             const std::string& workingDir,
                             const json::Value& launchParams,
                             bool initial,
                             const std::string& editor,
                             std::string* pId) const
{
   // generate a new id (loop until we find a unique one)
   std::string id;
   while (id.empty())
   {
      bool hasId = true;
      std::string candidateId = core::r_util::generateScopeId();
      Error error = storage_->hasSessionId(candidateId, &hasId);
      if (error)
         return error;

      if (!hasId)
         id = candidateId;
   }

   double now = date_time::millisecondsSinceEpoch();
   std::string millisTime = safe_convert::numberToString(now);

   //Initial settings
   std::map<std::string, std::string> initialMetadata = {
      {ActiveSession::kProject, project},
      {ActiveSession::kWorkingDir, workingDir},
      {ActiveSession::kInitial, initial ? "1" : "0"},
      {ActiveSession::kRunning, "0"},
      {ActiveSession::kLastUsed, millisTime},
      {ActiveSession::kCreated, millisTime},
      {ActiveSession::kLaunchParameters, ""},
      {ActiveSession::kLabel, project == kProjectNone ? workingDir : project},
      {ActiveSession::kActivityState, kActivityStateLaunching},
      {ActiveSession::kEditor, editor}
   };

   if (editor == kWorkbenchRStudio)
     initialMetadata.emplace(ActiveSession::kLastResumed, ActiveSession::getNowAsPTimestamp());

   if (launchParams.isObject())
   {
      initialMetadata[ActiveSession::kLaunchParameters] = launchParams.writeFormatted();
      initialMetadata[ActiveSession::kLabel] = launchParams.getObject()["name"].getString();
   }
   LOG_DEBUG_MESSAGE("Creating new session: " + id + ":" + editor + " project: " + project + " dir: " + workingDir);

   auto session = get(id);
   session->writeProperties(initialMetadata);


   // return the id if requested
   if (pId != NULL)
      *pId = id;
   
   return Success();
}

namespace
{

bool compareActivityLevel(boost::shared_ptr<ActiveSession> a,
                          boost::shared_ptr<ActiveSession> b)
{
   return *a > *b;
}

} // anonymous namespace

std::vector<boost::shared_ptr<ActiveSession> > ActiveSessions::list(bool validate,
                                                                    std::vector<boost::shared_ptr<ActiveSession>>* invalidSessions) const
{
   // Delegate to the property-caching overload with an empty set (= all properties)
   return list(validate, {}, invalidSessions);
}

std::vector<boost::shared_ptr<ActiveSession> > ActiveSessions::list(bool validate,
                                                                    const std::set<std::string>& propertiesToCache,
                                                                    std::vector<boost::shared_ptr<ActiveSession>>* invalidSessions) const
{
   // Try batch loading all session properties in one call
   std::map<std::string, std::map<std::string, std::string>> batchProperties;
   Error batchError = storage_->listSessionProperties(propertiesToCache, &batchProperties);
   bool hasBatchData = !batchError && !batchProperties.empty();

   std::vector<std::string> sessionIds;
   if (hasBatchData)
   {
      // Use session IDs from the batch result
      for (const auto& kv : batchProperties)
         sessionIds.push_back(kv.first);
   }
   else
   {
      // Fall back to listing IDs separately
      sessionIds = storage_->listSessionIds();
   }

   std::vector<boost::shared_ptr<ActiveSession>> sessions{};

   for(const std::string& id : sessionIds)
   {
      boost::shared_ptr<ActiveSession> candidateSession = get(id);

      // Pre-populate cache from batch data if available
      if (hasBatchData)
      {
         auto it = batchProperties.find(id);
         if (it != batchProperties.end())
            candidateSession->populateCache(it->second);
      }

      if (validate)
      {
         if (candidateSession->validate())
         {
            // Cache the sort conditions to ensure compareActivityLevel will provide a strict weak ordering.
            // Otherwise, the conditions on which we sort (e.g. lastUsed()) can be updated on disk during a sort
            // causing an occasional segfault.
            candidateSession->cacheSortConditions();
            sessions.push_back(candidateSession);
         }
         else
         {
            if (invalidSessions != nullptr)
            {
               LOG_DEBUG_MESSAGE("Returning invalid session: " + candidateSession->id());
               // the call will remove sessions that don't have required properties
               // (they may be here as a result of a race condition where
               // they are removed but then suspended session data is
               // written back into them)
               invalidSessions->push_back(candidateSession);
            }
            else
               LOG_DEBUG_MESSAGE("Skipping invalid session in list: " + candidateSession->id());
         }
      }
      else
         sessions.push_back(candidateSession);
   }

   // sort by activity level (most active sessions first)
   std::sort(sessions.begin(), sessions.end(), compareActivityLevel);
   return sessions;
}

size_t ActiveSessions::count() const
{
   return storage_->getSessionCount();
}

boost::shared_ptr<ActiveSession> ActiveSessions::get(const std::string& id) const
{
   std::shared_ptr<IActiveSessionStorage> candidateStorage = storage_->getSessionStorage(id);
   if (candidateStorage != nullptr)
   {
      if (storagePath_.isEmpty()) // rpc or db storage
         return boost::shared_ptr<ActiveSession>(new ActiveSession(id, candidateStorage));
      else // file storage
         return boost::shared_ptr<ActiveSession>(new ActiveSession(id, storagePath_.completeChildPath(kSessionDirPrefix + id), candidateStorage));
   }
   else
      return boost::shared_ptr<ActiveSession>(new ActiveSession(id));
}

boost::shared_ptr<ActiveSession> ActiveSessions::get(const std::string& id,
                                                     const std::set<std::string>& propertiesToCache) const
{
   boost::shared_ptr<ActiveSession> session = get(id);
   if (!session->empty())
   {
      if (propertiesToCache.empty())
      {
         // Load all properties
         session->loadAllProperties();
      }
      else
      {
         // Load specific properties into cache
         std::map<std::string, std::string> props;
         Error error = session->readProperties(propertiesToCache, &props);
         if (error)
            LOG_ERROR(error);
      }
   }
   return session;
}


boost::shared_ptr<ActiveSession> ActiveSessions::emptySession(const std::string& id) const
{
   return boost::shared_ptr<ActiveSession>(new ActiveSession(id));
}

std::vector<boost::shared_ptr<GlobalActiveSession> >
GlobalActiveSessions::list() const
{
   std::vector<boost::shared_ptr<GlobalActiveSession> > sessions;

   // get all active sessions for the system
   FilePath activeSessionsDir = rootPath_;
   if (!activeSessionsDir.exists())
      return sessions; // no active sessions exist

   std::vector<FilePath> sessionFiles;
   Error error = activeSessionsDir.getChildren(sessionFiles);
   if (error)
   {
      LOG_ERROR(error);
      return sessions;
   }

   for (const FilePath& sessionFile : sessionFiles)
   {
      sessions.push_back(boost::shared_ptr<GlobalActiveSession>(new GlobalActiveSession(sessionFile)));
   }

   return sessions;
}

boost::shared_ptr<GlobalActiveSession>
GlobalActiveSessions::get(const std::string& id) const
{
   FilePath sessionFile = rootPath_.completeChildPath(id);
   if (!sessionFile.exists())
      return boost::shared_ptr<GlobalActiveSession>();

   return boost::shared_ptr<GlobalActiveSession>(new GlobalActiveSession(sessionFile));
}

namespace {

void notifyCountChanged(boost::shared_ptr<ActiveSessions> pSessions,
                        boost::function<void(size_t)> onCountChanged)
{
   onCountChanged(pSessions->count());
}

} // anonymous namespace

void trackActiveSessionCount(std::shared_ptr<IActiveSessionsStorage> storage,
                             const FilePath& rootStoragePath,
                             boost::function<void(size_t)> onCountChanged)
{

   boost::shared_ptr<ActiveSessions> pSessions(
                                          new ActiveSessions(storage, rootStoragePath));

   core::system::file_monitor::Callbacks cb;
   cb.onRegistered = boost::bind(notifyCountChanged, pSessions, onCountChanged);
   cb.onFilesChanged = boost::bind(notifyCountChanged, pSessions, onCountChanged);
   cb.onRegistrationError = boost::bind(log::logError, _1, ERROR_LOCATION);

   core::system::file_monitor::registerMonitor(
                   ActiveSessions::storagePath(rootStoragePath),
                   false,
                   boost::function<bool(const FileInfo&)>(),
                   cb);

}

} // namespace r_util
} // namespace core
} // namespace rstudio
