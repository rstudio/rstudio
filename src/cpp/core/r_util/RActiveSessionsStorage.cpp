#include <core/r_util/RActiveSessionsStorage.hpp>
#include <core/r_util/RActiveSessionStorage.hpp>
#include <core/r_util/RActiveSessions.hpp>
#include <algorithm>

#define kSessionDirPrefix "session-"

namespace rstudio {
namespace core {
namespace r_util {

namespace
{
   FilePath getSessionDirPath(const FilePath& storagePath, const std::string& sessionId)
   {
      return storagePath.completeChildPath(kSessionDirPrefix + sessionId);
   }
}

   static FilePath buildStoragePath(const FilePath& rootStoragePath)
   {
      return rootStoragePath.completeChildPath("sessions/active");
   }

   FileActiveSessionsStorage::FileActiveSessionsStorage(const FilePath& rootStoragePath)
   {
      storagePath_ = buildStoragePath(rootStoragePath);
      Error error = storagePath_.ensureDirectory();
      if (error)
      {
         LOG_ERROR(error);
      }
   }

   bool FileActiveSessionsStorage::hasSessionId(const std::string& sessionId) const
   {
      FilePath dir = getSessionDirPath(storagePath_, sessionId);
      return dir.exists();
   }

   namespace {

   bool compareActivityLevel(boost::shared_ptr<ActiveSession> a,
                           boost::shared_ptr<ActiveSession> b)
   {
      return *a > *b;
   }

   } // anonymous namespace

   core::Error FileActiveSessionsStorage::createSession(const std::string& id, std::map<std::string, std::string> initialProperties)
   {
      FilePath sessionScratchPath = storagePath_.completeChildPath(kSessionDirPrefix + id);
      Error error = sessionScratchPath.ensureDirectory();

      if(error)
      {
         return error;
      }

      FileActiveSessionStorage session = FileActiveSessionStorage{sessionScratchPath};
      error = session.writeProperties(initialProperties);
      return error;
   }

   std::vector<boost::shared_ptr<ActiveSession> > FileActiveSessionsStorage::listSessions(FilePath userHomePath, bool projectSharingEnabled) const
   {
      // list to return
      std::vector<boost::shared_ptr<ActiveSession> > sessions;

      // enumerate children and check for sessions
      std::vector<FilePath> children;
      Error error = storagePath_.getChildren(children);
      if (error)
      {
         LOG_ERROR(error);
         return sessions;
      }
      std::string prefix = kSessionDirPrefix;
      for (const FilePath& child : children)
      {
         if (boost::algorithm::starts_with(child.getFilename(), prefix))
         {
            std::string id = child.getFilename().substr(prefix.length());
            boost::shared_ptr<ActiveSession> pSession = getSession(id);
            if (!pSession->empty())
            {
               if (pSession->validate(userHomePath, projectSharingEnabled))
               {
                  // Cache the sort conditions to ensure compareActivityLevel will provide a strict weak ordering.
                  // Otherwise, the conditions on which we sort (e.g. lastUsed()) can be updated on disk during a sort
                  // causing an occasional segfault.
                  pSession->cacheSortConditions();
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

   size_t FileActiveSessionsStorage::getSessionCount(const FilePath& userHomePath, bool projectSharingEnabled) const
   {
      return listSessions(userHomePath, projectSharingEnabled).size();
   }

   // Returns a shared pointer to the session, or an empty session if it does not exist
   boost::shared_ptr<ActiveSession> FileActiveSessionsStorage::getSession(const std::string& id) const
   {
      FilePath scratchPath = storagePath_.completeChildPath(kSessionDirPrefix + id);
      if (scratchPath.exists())
         return boost::shared_ptr<ActiveSession>(new ActiveSession(id, scratchPath,
            std::make_shared<FileActiveSessionStorage>(FileActiveSessionStorage(scratchPath))));
      else
         return boost::shared_ptr<ActiveSession>(new ActiveSession(id));
   }
}
}
}