/*
 * SessionSourceDatabaseSupervisor.cpp
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

#include "SessionSourceDatabaseSupervisor.hpp"

#include <vector>

#include <boost/foreach.hpp>
#include <boost/scope_exit.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/FileLock.hpp>
#include <core/BoostErrors.hpp>


#include <core/system/System.hpp>

#include <session/SessionModuleContext.hpp>
#include "session/SessionSourceDatabase.hpp"

using namespace core;

namespace session {
namespace source_database {
namespace supervisor {

namespace {

const char * const kSessionDirPrefix = "s-";

FilePath oldSourceDatabaseRoot()
{
   return
      module_context::scopedScratchPath().complete("source_database");
}

FilePath sourceDatabaseRoot()
{
   return module_context::scopedScratchPath().complete("sdb");
}

FilePath persistentTitledDir()
{
   return sourceDatabaseRoot().complete("per/t");
}

FilePath oldPersistentTitledDir()
{
   FilePath oldPath = module_context::oldScopedScratchPath();
   if (oldPath.exists())
      return oldPath.complete("source_database_v2/persistent/titled");
   else
      return FilePath();
}

FilePath persistentUntitledDir()
{
   return sourceDatabaseRoot().complete("per/u");
}

FilePath oldPersistentUntitledDir()
{
   FilePath oldPath = module_context::oldScopedScratchPath();
   if (oldPath.exists())
      return oldPath.complete("source_database_v2/persistent/untitled");
   else
      return FilePath();
}

FilePath sessionLockFilePath(const FilePath& sessionDir)
{
   return sessionDir.complete("lock_file");
}

// session dir lock (initialized by attachToSourceDatabase)
FileLock s_sessionDirLock;

Error removeSessionDir(const FilePath& sessionDir)
{
   // first remove children
   std::vector<FilePath> children;
   Error error = sessionDir.children(&children);
   if (error)
      LOG_ERROR(error);
   BOOST_FOREACH(const FilePath& filePath, children)
   {
      error = filePath.remove();
      if (error)
         LOG_ERROR(error);
   }

   // then remove dir
   return sessionDir.remove();
}

FilePath generateSessionDirPath()
{
   return module_context::uniqueFilePath(sourceDatabaseRoot(),
                                         kSessionDirPrefix);
}

bool isNotSessionDir(const FilePath& filePath)
{
   return !filePath.isDirectory() || !boost::algorithm::starts_with(
                                                filePath.filename(),
                                                kSessionDirPrefix);
}

Error enumerateSessionDirs(std::vector<FilePath>* pSessionDirs)
{
   // get the directories
   Error error = sourceDatabaseRoot().children(pSessionDirs);
   if (error)
      return error;

   // clean out non session dirs
   pSessionDirs->erase(std::remove_if(pSessionDirs->begin(),
                                      pSessionDirs->end(),
                                      isNotSessionDir),
                       pSessionDirs->end());

   // return success
   return Success();
}

void attemptToMoveSourceDbFiles(const FilePath& fromPath,
                                const FilePath& toPath)
{
   // enumerate the from path
   std::vector<FilePath> children;
   Error error = fromPath.children(&children);
   if (error)
      LOG_ERROR(error);

   // move the files
   BOOST_FOREACH(const FilePath& filePath, children)
   {
      // if the target path already exists then just generate a
      // new unique filename
      FilePath targetPath = toPath.complete(filePath.filename());
      if (targetPath.exists())
         targetPath = module_context::uniqueFilePath(toPath);

      Error error = filePath.move(targetPath);
      if (error)
         LOG_ERROR(error);
   }
}

// NOTE: the supervisor needs to return a session dir in order for the process
// to start. therefore, in the createSessionDir family of functions below
// once we successfully create and lock the session dir other errors (such
// as trying to move files into the session dir) are simply logged

Error createSessionDir(FilePath* pSessionDir)
{
   *pSessionDir = generateSessionDirPath();

   Error error = pSessionDir->ensureDirectory();
   if (error)
      return error;

   return s_sessionDirLock.acquire(sessionLockFilePath(*pSessionDir));
}

Error createSessionDirFromOldSourceDatabase(FilePath* pSessionDir)
{
   // move properties (if any) into new source database root
   FilePath propsPath = oldSourceDatabaseRoot().complete("properties");
   if (propsPath.exists())
   {
      FilePath newPropsPath = sourceDatabaseRoot().complete("prop");
      Error error = propsPath.move(newPropsPath);
      if (error)
         LOG_ERROR(error);
   }

   // move the old source database into a new dir
   *pSessionDir = generateSessionDirPath();
   Error error = oldSourceDatabaseRoot().move(*pSessionDir);
   if (error)
      LOG_ERROR(error);

   // if that failed we might still need to call ensureDirectory
   error = pSessionDir->ensureDirectory();
   if (error)
      return error;

   // acquire the lock
   return s_sessionDirLock.acquire(sessionLockFilePath(*pSessionDir));
}

Error createSessionDirFromPersistent(FilePath* pSessionDir)
{
   // create new session dir
   Error error = createSessionDir(pSessionDir);
   if (error)
      return error;

   // move persistent titled files
   if (persistentTitledDir().exists())
      attemptToMoveSourceDbFiles(persistentTitledDir(), *pSessionDir);

   // get legacy titled docs if they exist
   if (oldPersistentTitledDir().exists())
      attemptToMoveSourceDbFiles(oldPersistentTitledDir(), *pSessionDir);

   // move persistent untitled files
   if (persistentUntitledDir().exists())
      attemptToMoveSourceDbFiles(persistentUntitledDir(), *pSessionDir);

   // get legacy untitled docs if they exist
   if (oldPersistentUntitledDir().exists())
      attemptToMoveSourceDbFiles(oldPersistentUntitledDir(), *pSessionDir);

   // return success
   return Success();
}

bool reclaimOrphanedSession(const std::vector<FilePath>& sessionDirs,
                            FilePath* pSessionDir)
{
   BOOST_FOREACH(const FilePath& sessionDir, sessionDirs)
   {
      FilePath lockFilePath = sessionLockFilePath(sessionDir);
      if (!FileLock::isLocked(lockFilePath))
      {
         Error error = s_sessionDirLock.acquire(lockFilePath);
         if (!error)
         {
            *pSessionDir = sessionDir;
            return true;
         }
         else
         {
            LOG_ERROR(error);
         }
      }
   }

   return false;
}

bool noLockedSessionsExist(const std::vector<FilePath>& sessionDirs)
{
   BOOST_FOREACH(const FilePath& sessionDir, sessionDirs)
   {
      FilePath lockFilePath = sessionLockFilePath(sessionDir);
      if (FileLock::isLocked(lockFilePath))
         return false;
   }

   return true;
}

} // anonymous namespace


Error attachToSourceDatabase(FilePath* pSessionDir)
{  
   // check whether we will need to migrate -- ensure we do this only
   // one time so that if for whatever reason we can't migrate the
   // old source database we don't get stuck trying to do it every
   // time we start up
   bool needToMigrate = !sourceDatabaseRoot().exists() &&
                        oldSourceDatabaseRoot().exists();

   // ensure the root path exists
   Error error = sourceDatabaseRoot().ensureDirectory();
   if (error)
      return error;

   // check for existing sessions (use this to decide how to startup below)
   std::vector<FilePath> sessionDirs;
   error = enumerateSessionDirs(&sessionDirs);
   if (error)
      LOG_ERROR(error);

   // attempt to migrate if necessary
   if (needToMigrate)
      return createSessionDirFromOldSourceDatabase(pSessionDir);

   // if there is an orphan (crash) then reclaim it
   else if (reclaimOrphanedSession(sessionDirs, pSessionDir))
      return Success();

   // if there are no existing locked sessions then create from persistent
   else if (noLockedSessionsExist(sessionDirs))
      return createSessionDirFromPersistent(pSessionDir);

   // otherwise startup with a brand new session dir
   else
      return createSessionDir(pSessionDir);
}

Error detachFromSourceDatabase()
{
   // confirm that we are attached
   if (s_sessionDirLock.lockFilePath().empty())
      return core::pathNotFoundError(ERROR_LOCATION);

   // list all current source docs
   std::vector<boost::shared_ptr<SourceDocument> > sourceDocs;
   Error error = source_database::list(&sourceDocs);
   if (error)
      return error;

   // get references to persistent subdirs
   FilePath titledDir = persistentTitledDir();
   FilePath untitledDir = persistentUntitledDir();

   // first blow away the existing persistent titled dir
   error = titledDir.removeIfExists();
   if (error)
      LOG_ERROR(error);

   // ensure both directories exist -- if they don't it is a fatal error
   error = titledDir.ensureDirectory();
   if (error)
      return error;
   error = untitledDir.ensureDirectory();
   if (error)
      return error;

   // now write the source database entries to the appropriate places
   BOOST_FOREACH(boost::shared_ptr<SourceDocument> pDoc, sourceDocs)
   {
      if (pDoc->isUntitled())
      {
         // compute the target path (manage uniqueness since this
         // directory is appended to from multiple processes who
         // could have created docs with the same id)
         FilePath targetPath = untitledDir.complete(pDoc->id());
         if (targetPath.exists())
            targetPath = module_context::uniqueFilePath(untitledDir);

         error = pDoc->writeToFile(targetPath);
         if (error)
            LOG_ERROR(error);
      }
      else
      {
         error = pDoc->writeToFile(titledDir.complete(pDoc->id()));
         if (error)
            LOG_ERROR(error);
      }
   }

   // record session dir (parent of lock file)
   FilePath sessionDir = s_sessionDirLock.lockFilePath().parent();

   // give up our lock
   error = s_sessionDirLock.release();
   if (error)
      LOG_ERROR(error);

   // remove the session directory
   return removeSessionDir(sessionDir);
}


} // namespace supervisor
} // namespace source_database
} // namespace session



