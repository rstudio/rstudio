/*
 *
 * SessionSourceDatabaseSupervisor.cpp
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

#include "SessionSourceDatabaseSupervisor.hpp"

#ifdef _WIN32
# include <winsock2.h>
# include <windows.h>
#endif

#include <vector>

#include <boost/foreach.hpp>
#include <boost/scope_exit.hpp>
#include <boost/algorithm/string/predicate.hpp>

#include <core/Error.hpp>
#include <core/FilePath.hpp>
#include <core/FileSerializer.hpp>
#include <core/FileLock.hpp>
#include <core/FileUtils.hpp>
#include <core/BoostErrors.hpp>

#include <r/session/RSession.hpp>

#include <core/system/System.hpp>

#include <session/SessionOptions.hpp>
#include <session/SessionModuleContext.hpp>
#include "session/SessionSourceDatabase.hpp"

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace source_database {
namespace supervisor {

namespace {

const char * const kSessionDirPrefix = "s-";

FilePath sdbSourceDatabaseRoot()
{
   return module_context::scopedScratchPath().complete("sdb");
}

FilePath sourceDatabaseRoot()
{
   return module_context::scopedScratchPath().complete(kSessionSourceDatabasePrefix);
}

FilePath mostRecentTitledDir()
{
   return module_context::scopedScratchPath().complete(kSessionSourceDatabasePrefix "/mt");
}

FilePath mostRecentUntitledDir()
{
   return module_context::scopedScratchPath().complete(kSessionSourceDatabasePrefix "/mu");
}

FilePath persistentTitledDir(bool multiSession = true)
{
   if (multiSession && !module_context::activeSession().empty())
   {
      std::string id = module_context::activeSession().id();
      return sourceDatabaseRoot().complete("per/t/" + id);
   }
   else
   {
      return sourceDatabaseRoot().complete("per/t");
   }
}

FilePath oldPersistentTitledDir()
{
   FilePath oldPath = module_context::oldScopedScratchPath();
   if (oldPath.exists())
      return oldPath.complete("source_database_v2/persistent/titled");
   else
      return FilePath();
}

FilePath persistentUntitledDir(bool multiSession = true)
{
   if (multiSession && !module_context::activeSession().empty())
   {
      std::string id = module_context::activeSession().id();
      return sourceDatabaseRoot().complete("per/u/" + id);
   }
   else
   {
      return sourceDatabaseRoot().complete("per/u");
   }
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

FilePath sessionSuspendFilePath(const FilePath& sessionDir)
{
   return sessionDir.complete("suspend_file");
}

FilePath sessionRestartFilePath(const FilePath& sessionDir)
{
   return sessionDir.complete("restart_file");
}

// session dir lock (lock is acquired within 'attachToSourceDatabase()')
boost::shared_ptr<FileLock> createSessionDirLock()
{
   // always use advisory locks on Desktop, but allow server lock scheme
   // to be configurable
   bool isServer = session::options().programMode() == kSessionProgramModeServer;
   if (isServer)
      return FileLock::createDefault();
   else
      return FileLock::create(FileLock::LOCKTYPE_ADVISORY);
}

FileLock& sessionDirLock()
{
   static boost::shared_ptr<FileLock> instance = createSessionDirLock();
   return *instance;
}

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
      // skip directories (directories can exist because multi-session
      // mode writes top level directories into the /sdb/t and /sdb/u
      // stores -- these directories correspond to the persistent docs
      // of particular long-running sessions)
      if (filePath.isDirectory())
         continue;

      // if the target path already exists then skip it and log
      // (we used to generate a new uniqueFilePath however this
      // caused the filename and id (stored in the source doc)
      // to get out of sync, making documents unclosable. The
      // chance of file with the same name already existing is
      // close to zero (collision probability of uniqueFilePath)
      // so it's no big deal to punt here.
      FilePath targetPath = toPath.complete(filePath.filename());
      if (targetPath.exists())
      {
         LOG_WARNING_MESSAGE("Skipping source db move from: " +
                             filePath.absolutePath() + " to " +
                             targetPath.absolutePath());

         Error error = filePath.remove();
         if (error)
            LOG_ERROR(error);

         continue;
      }

      Error error = filePath.move(targetPath);
      if (error)
         LOG_ERROR(error);
   }
}

// NOTE: the supervisor needs to return a session dir in order for the process
// to start. therefore, in the createSessionDir family of functions below
// once we successfully create and lock the session dir other errors (such
// as trying to move files into the session dir) are simply logged

Error createSessionDir()
{
   Error error = sessionDirPath().ensureDirectory();
   if (error)
      return error;

   // attempt to acquire the lock. if we can't then we still continue
   // so we can support filesystems that don't have file locks.
   error = sessionDirLock().acquire(sessionLockFilePath(sessionDirPath()));
   if (error)
      LOG_ERROR(error);

   return Success();
}

Error createSessionDirFromPersistent()
{
   // note whether we are in multi-session mode
   bool multiSession = !module_context::activeSession().empty();

   // create new session dir
   Error error = createSessionDir();
   if (error)
      return error;

   // move persistent titled files. if we don't have any this is a
   // brand new instantiation of this project within this session
   // so we try to grab the MRU list
   if (persistentTitledDir().exists())
   {
      attemptToMoveSourceDbFiles(persistentTitledDir(), sessionDirPath());
   }
   // check for the most recent titled directory
   else if (multiSession && mostRecentTitledDir().exists())
   {
      attemptToMoveSourceDbFiles(mostRecentTitledDir(), sessionDirPath());
   }
   // last resort: if we are in multi-session mode see if there is a set of
   // mono-session source documents that we can migrate
   else if (multiSession && persistentTitledDir(false).exists())
   {
      attemptToMoveSourceDbFiles(persistentTitledDir(false), sessionDirPath());
   }

   // get legacy titled docs if they exist
   if (oldPersistentTitledDir().exists())
      attemptToMoveSourceDbFiles(oldPersistentTitledDir(), sessionDirPath());

   // move persistent untitled files
   if (persistentUntitledDir().exists())
   {
      attemptToMoveSourceDbFiles(persistentUntitledDir(), sessionDirPath());
   }
   // check for the most recent untitled directory
   else if (multiSession && mostRecentUntitledDir().exists())
   {
      attemptToMoveSourceDbFiles(mostRecentUntitledDir(), sessionDirPath());
   }
   // last resort: if we are in multi-session mode see if there is a set of
   // mono-session source documents that we can migrate
   else if (multiSession && persistentUntitledDir(false).exists())
   {
       attemptToMoveSourceDbFiles(persistentUntitledDir(false), sessionDirPath());
   }

   // get legacy untitled docs if they exist
   if (oldPersistentUntitledDir().exists())
      attemptToMoveSourceDbFiles(oldPersistentUntitledDir(), sessionDirPath());

   // return success
   return Success();
}

bool reclaimOrphanedSession()
{
   // check for existing sessions
   std::vector<FilePath> sessionDirs;
   Error error = enumerateSessionDirs(&sessionDirs);
   if (error)
   {
      LOG_ERROR(error);
      return false;
   }

   BOOST_FOREACH(const FilePath& sessionDir, sessionDirs)
   {
      // if the suspend file exists, this session is only sleeping, not dead
      if (sessionSuspendFilePath(sessionDir).exists())
         continue;

      FilePath restartFile = sessionRestartFilePath(sessionDir);
      if (restartFile.exists())
      {
         if (std::time(NULL) - restartFile.lastWriteTime() > 
              (1000 * 60 * 5))
         {
            // the file exists, but it's more than five minutes old, so 
            // something went wrong 
            Error error = restartFile.remove();
            if (error)
               LOG_ERROR(error);
         }
         else
         {
            // the restart file exists and is new, so it represents a 
            // session currently undergoing a suspend for restart -- leave
            // it alone
            continue;
         }
      }

      FilePath lockFilePath = sessionLockFilePath(sessionDir);
      if (!sessionDirLock().isLocked(lockFilePath))
      {
         // adopt by giving the session dir our own name
         Error error = sessionDir.move(sessionDirPath());
         if (error)
            LOG_ERROR(error);
         else
         {
            error = sessionDirLock().acquire(
                  sessionLockFilePath(sessionDirPath()));
            if (!error)
            {
               return true;
            }
            else
            {
               LOG_ERROR(error);
            }
         }
      }
   }

   return false;
}

Error removeAndRecreate(const FilePath& dir)
{
   // blow it away if it exists then recreate it
   Error error = dir.removeIfExists();
   if (error)
      LOG_ERROR(error);
   return dir.ensureDirectory();
}

} // anonymous namespace


// NOTE: we attempt to use file locks to coordinate between disperate
// processes all attempting to open a session in the same context (project
// or global). Locks are used to implement recovery of crashed sessions
// as follows: if there is an existing source-db directory on disk that
// is NOT locked then it's presumed to be an orphan (resulting from a crash)
// and we should initialize with this directory to "recover" it
//
// Unfortunately, some file systems (mostly remote network volumes) don't
// support file-locking. In these cases we need to gracefully fall back
// to some sane behavior. To implement this we use the following scheme:
//
//  (1) Always attempt to call FileLock::acquire to create an advisory lock
//      but if it fails we still allow the process to start up.
//
//  (2) When checking for "orphan" source-db directories we try to acquire
//      a lock on them -- for volumes that don't support locks this will
//      always be an error so we'll never be able to recover an orphan dir
//
// In some multi-machine cases it's actually possible for two proccesses
// to both get a lock on the same file. For this reason if we are running
// multi-machine (i.e. load balancing enabled) we don't attempt orphan
// recovery because it could result in one session stealing the other's
// source database out from under it.
//

Error attachToSourceDatabase()
{  
   // this session may already have a source database; if it does, re-acquire a
   // lock and then use it
   FilePath existingSdb = sessionDirPath();
   if (existingSdb.exists())
      return sessionDirLock().acquire(sessionLockFilePath(existingSdb));
   
   // migrate from 'sdb' to current folder layout if needed
   bool needsSdbMigration =
         !sourceDatabaseRoot().exists() &&
         sdbSourceDatabaseRoot().exists();
   
   if (needsSdbMigration)
   {
      Error error =
            sdbSourceDatabaseRoot().copyDirectoryRecursive(sourceDatabaseRoot());
      
      if (error)
         LOG_ERROR(error);
   }

   // ensure the root path exists
   Error error = sourceDatabaseRoot().ensureDirectory();
   if (error)
      return error;

   // if there is an orphan (crash) then reclaim it.
   if (reclaimOrphanedSession())
   {
      return Success();
   }

   // attempt to create from persistent
   else
   {
      return createSessionDirFromPersistent();
   }
}

// preserve documents for re-opening in a future session
Error saveMostRecentDocuments()
{
   // only do this for multi-session contexts
   if (!module_context::activeSession().empty())
   {
      // most recent docs is last one wins so we remove and recreate
      FilePath mostRecentDir = mostRecentTitledDir();
      Error error = removeAndRecreate(mostRecentDir);
      if (error)
         return error;

      // untitled are aggregated (so we never lose unsaved docs)
      // so we just ensure the directory exists)
      FilePath mostRecentDirUntitled = mostRecentUntitledDir();
      error = mostRecentDirUntitled.ensureDirectory();
      if (error)
         return error;

      // list all current source docs
      std::vector<boost::shared_ptr<SourceDocument> > sourceDocs;
      error = source_database::list(&sourceDocs);
      if (error)
         return error;

      // write the docs into the mru directories
      BOOST_FOREACH(boost::shared_ptr<SourceDocument> pDoc, sourceDocs)
      {
         FilePath targetDir = pDoc->isUntitled() ? mostRecentDirUntitled :
                                                   mostRecentDir;

         Error error = pDoc->writeToFile(targetDir.childPath(pDoc->id()));
         if (error)
            LOG_ERROR(error);
      }
   }

   return Success();
}

Error detachFromSourceDatabase()
{
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
            targetPath = file_utils::uniqueFilePath(untitledDir);

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
   FilePath sessionDir = sessionDirLock().lockFilePath().parent();

   // give up our lock
   error = sessionDirLock().release();
   if (error)
      LOG_ERROR(error);

   // remove the session directory
   return removeSessionDir(sessionDir);
}

FilePath sessionDirPath()
{
   return sourceDatabaseRoot().complete(kSessionDirPrefix +
         module_context::activeSession().id());
}

void suspendSourceDatabase(int status)
{
   // write a sentinel so we can differentiate between a sdb that's orphaned
   // from a crash, and an sdb that represents a suspended session
   Error error = status == EX_CONTINUE ? 
      sessionRestartFilePath(sessionDirPath()).ensureFile() : 
      sessionSuspendFilePath(sessionDirPath()).ensureFile();
   if (error)
      LOG_ERROR(error);
}

void resumeSourceDatabase()
{
   Error error = sessionSuspendFilePath(sessionDirPath()).removeIfExists();
   if (error)
      LOG_ERROR(error);
   error = sessionRestartFilePath(sessionDirPath()).removeIfExists();
   if (error)
      LOG_ERROR(error);
}

} // namespace supervisor
} // namespace source_database
} // namespace session
} // namespace rstudio



