/*
 * RActiveSessions.cpp
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

#include <core/r_util/RActiveSessions.hpp>

#include <boost/bind.hpp>
#include <boost/foreach.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/StringUtils.hpp>
#include <core/FileSerializer.hpp>

#include <core/system/System.hpp>
#include <core/system/FileMonitor.hpp>

#include <core/r_util/RSessionContext.hpp>

#define kSessionDirPrefix "session-"

namespace rstudio {
namespace core {
namespace r_util {

namespace {


} // anonymous namespace


void ActiveSession::writeProperty(const std::string& name,
                                 const std::string& value) const
{
   FilePath propertyFile = propertiesPath_.childPath(name);
   Error error = core::writeStringToFile(propertyFile, value);
   if (error)
      LOG_ERROR(error);
}

std::string ActiveSession::readProperty(const std::string& name) const
{
   using namespace rstudio::core;
   FilePath readPath = propertiesPath_.childPath(name);
   if (readPath.exists())
   {
      std::string value;
      Error error = core::readStringFromFile(readPath, &value);
      if (error)
      {
         LOG_ERROR(error);
         return std::string();
      }
      return boost::algorithm::trim_copy(value);
   }
   else
   {
      return std::string();
   }
}

Error ActiveSessions::create(const std::string& project,
                             const std::string& workingDir,
                             bool initial,
                             std::string* pId) const
{
   // generate a new id (loop until we find a unique one)
   std::string id;
   FilePath dir;
   while (id.empty())
   {
      std::string candidateId = core::r_util::generateScopeId();
      dir = storagePath_.childPath(kSessionDirPrefix + candidateId);
      if (!dir.exists())
         id = candidateId;
   }

   // create the directory
   Error error = dir.ensureDirectory();
   if (error)
      return error;

   // write initial settings
   ActiveSession activeSession(id, dir);
   activeSession.setProject(project);
   activeSession.setWorkingDir(workingDir);
   activeSession.setInitial(initial);
   activeSession.setLastUsed();
   activeSession.setRunning(false);

   // return the id
   *pId = id;
   return Success();
}

namespace {

bool compareActivityLevel(boost::shared_ptr<ActiveSession> a,
                          boost::shared_ptr<ActiveSession> b)
{
   if (a->executing() == b->executing())
   {
      if (a->running() == b->running())
      {
         if (a->lastUsed() == b->lastUsed())
         {
            return a->id() > b->id();
         }
         else
         {
            return a->lastUsed() > b->lastUsed();
         }
      }
      else
      {
         return a->running();
      }
   }
   else
   {
      return a->executing();
   }
}

} // anonymous namespace

std::vector<boost::shared_ptr<ActiveSession> > ActiveSessions::list(
                                       const FilePath& userHomePath,
                                       bool projectSharingEnabled) const
{
   // list to return
   std::vector<boost::shared_ptr<ActiveSession> > sessions;

   // enumerate children and check for sessions
   std::vector<FilePath> children;
   Error error = storagePath_.children(&children);
   if (error)
   {
      LOG_ERROR(error);
      return sessions;
   }
   std::string prefix = kSessionDirPrefix;
   BOOST_FOREACH(const FilePath& child, children)
   {
      if (boost::algorithm::starts_with(child.filename(), prefix))
      {
         std::string id = child.filename().substr(prefix.length());
         boost::shared_ptr<ActiveSession> pSession = get(id);
         if (!pSession->empty())
         {
            if (pSession->validate(userHomePath, projectSharingEnabled))
            {
               sessions.push_back(pSession);
            }
            else
            {
               // remove sessions that don't have required properties
               // (they may be here as a result of a race condition where
               // they are removed but then suspended session data is
               // written back into them)
               Error error = pSession->destroy();
               if (error)
                  LOG_ERROR(error);
            }
         }
      }

   }

   // sort by activity level (most active sessions first)
   std::sort(sessions.begin(), sessions.end(), compareActivityLevel);

   // return
   return sessions;
}

size_t ActiveSessions::count(const FilePath& userHomePath,
                             bool projectSharingEnabled) const
{
   return list(userHomePath, projectSharingEnabled).size();
}

boost::shared_ptr<ActiveSession> ActiveSessions::get(const std::string& id) const
{
   FilePath scratchPath = storagePath_.childPath(kSessionDirPrefix + id);
   if (scratchPath.exists())
      return boost::shared_ptr<ActiveSession>(new ActiveSession(id,
                                                                scratchPath));
   else
      return emptySession(id);
}


boost::shared_ptr<ActiveSession> ActiveSessions::emptySession(
      const std::string& id)
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
                   pSessions->storagePath(),
                   false,
                   boost::function<bool(const FileInfo&)>(),
                   cb);
}

} // namespace r_util
} // namespace core
} // namespace rstudio



