/*
 * NotebookPaths.cpp
 *
 * Copyright (C) 2020 by RStudio, PBC
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

#include "SessionRmdNotebook.hpp"
#include "NotebookCache.hpp"
#include "NotebookPaths.hpp"

#include <ctime>

#include <core/FileSerializer.hpp>
#include <core/FileLock.hpp>

#include <session/SessionModuleContext.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {
namespace {

// a mapping of paths to the corresponding IDs
std::map<std::string, std::string> s_idCache;
std::time_t s_cacheWriteTime = 0;

// lock instance for protecting paths mapping file from concurrent writes/reads
// in multiple sessions
FileLock& nbPathLock()
{
   static boost::shared_ptr<FileLock> instance = FileLock::createDefault();
   return *instance;
}

// scope guard for above
class PathLockGuard : boost::noncopyable
{
public:
   PathLockGuard()
   {
      error_ = nbPathLock().acquire(
         notebookCacheRoot().completeChildPath("lock_file"));
   }

   ~PathLockGuard()
   {
      if (!error_)
      {
         error_ = nbPathLock().release();
         if (error_)
            LOG_ERROR(error_);
      }
   }

   Error error()
   {
      return error_;
   }

private:
   Error error_;
};

FilePath cachePath()
{
   return notebookCacheRoot().completeChildPath("paths");
}

void cleanNotebookPathMap()
{
   PathLockGuard guard;
   if (guard.error())
   {
      // this work is janitoral and runs on every session boot so no need to
      // reschedule aggressively if we can't lock the map
      return;
   }

   FilePath cache = cachePath();
   Error error = core::readStringMapFromFile(cache, &s_idCache);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // loop over entries (conditionalize increment so we don't attempt to
   // move forward over an invalidated iterator)
   for (std::map<std::string, std::string>::iterator it = s_idCache.begin();
        it != s_idCache.end();)
   {
      // clean up cache entries that refer to files that don't exist
      if (!FilePath(it->first).exists())
         s_idCache.erase(it++);
      else
         ++it;
   }

   // write out updated cache
   error = writeStringMapToFile(cache, s_idCache);
   if (error)
      LOG_ERROR(error);
   s_cacheWriteTime = std::time(nullptr);
}

Error synchronizeCache()
{
   Error error;
   FilePath cache = cachePath();
   if (!cache.exists())
   {
      // create folder to host cache if necessary
      if (!cache.getParent().exists())
      {
         error = cache.getParent().ensureDirectory();
         if (error)
            return error;
      }
   }
   else
   {
      // the cache exists; see if we need to reload
      if (cache.getLastWriteTime() > s_cacheWriteTime)
      {
         // attempt to lock the file for reading
         PathLockGuard guard;
         if (guard.error())
            return guard.error();

         error = core::readStringMapFromFile(cache, &s_idCache);
         if (error)
            return error;
         s_cacheWriteTime = std::time(nullptr);

         // schedule a path map cleanup (no urgency)
         module_context::scheduleDelayedWork(boost::posix_time::seconds(10),
            cleanNotebookPathMap, true);
      }
   }
   return Success();
}


} // anonymous namespace

Error notebookPathToId(const core::FilePath& path, std::string *pId)
{
   Error error = synchronizeCache();
   if (error)
      return error;
   
   // check to see if the path is already in our lookup table
   std::map<std::string, std::string>::iterator it = 
      s_idCache.find(path.getAbsolutePath());
   if (it != s_idCache.end())
   {
      *pId = it->second;
      return Success();
   }

   // need to generate a new ID for this path; make sure we don't collide with
   // an existing ID
   std::string id;
   bool existing;
   do 
   {
      existing = false;
      id = core::system::generateShortenedUuid();
      for (it = s_idCache.begin(); it != s_idCache.end(); it++) 
      {
         if (it->second == id)
         {
            existing = true;
            break;
         }
      }
   } while (existing);

   // lock and update the cache
   PathLockGuard guard;
   if (guard.error())
      return error;

   // insert the new ID and update caches
   s_idCache[path.getAbsolutePath()] = id;
   error = writeStringMapToFile(cachePath(), s_idCache);
   if (error)
      return error;
   s_cacheWriteTime = std::time(nullptr);
   *pId = id;

   return Success();
}

core::Error notebookIdToPath(const std::string& id, core::FilePath* pPath)
{
   Error error = synchronizeCache();
   if (error)
      return error;
   
   for (std::map<std::string, std::string>::iterator it = s_idCache.begin();
        it != s_idCache.end();
        it++)
   {
      if (it->second == id)
      {
         *pPath = FilePath(it->first);
         return Success();
      }
   }

   return systemError(boost::system::errc::no_such_file_or_directory,
                      ERROR_LOCATION);
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
