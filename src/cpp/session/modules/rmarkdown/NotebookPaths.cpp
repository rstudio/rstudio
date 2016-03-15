/*
 * NotebookPaths.cpp
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

#include "SessionRmdNotebook.hpp"
#include "NotebookCache.hpp"
#include "NotebookPaths.hpp"

#include <ctime>

#include <core/FileSerializer.hpp>

#include <session/SessionModuleContext.hpp>
#include <session/SessionUserSettings.hpp>

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

FilePath cachePath(const std::string& nbCtxId)
{
   return notebookCacheRoot().childPath("paths-" + nbCtxId);
}

void cleanNotebookCache()
{
   FilePath cache = cachePath(notebookCtxId());
   Error error = core::readStringMapFromFile(cache, &s_idCache);

   // TODO: Clean up cache folders that aren't used. Needs some reformulation
   // of the way we form the folder paths--ATM the paths include only the 
   // context ID; they need to also include session ID, otherwise we could
   // nuke a cache folder used by another session

   for (std::map<std::string, std::string>::iterator it = s_idCache.begin();
        it != s_idCache.end(); 
        it++)
   {
      // clean up cache entries that don't exist
      if (!FilePath(it->first).exists())
         s_idCache.erase(it->first);
   }

   // write out updated cache
   error = writeStringMapToFile(cache, s_idCache);
   if (error)
      LOG_ERROR(error);
   s_cacheWriteTime = std::time(NULL);
}
   

} // anonymous namespace

Error notebookPathToId(const core::FilePath& path, const std::string& nbCtxId,
      std::string *pId)
{
   Error error;
   FilePath cache = cachePath(nbCtxId);
   if (!cache.exists())
   {
      // create folder to host cache if necessary
      if (!cache.parent().exists())
      {
         error = cache.parent().ensureDirectory();
         if (error)
            return error;
      }
   }
   else
   {
      // the cache exists; see if we need to reload
      if (cache.lastWriteTime() > s_cacheWriteTime) 
      {
         // TODO: this needs to be sensitive to the context ID 
         error = core::readStringMapFromFile(cache, &s_idCache);
         if (error)
            return error;
         s_cacheWriteTime = std::time(NULL);

         // schedule a cache cleanup (no urgency)
         module_context::scheduleDelayedWork(boost::posix_time::seconds(10),
            cleanNotebookCache, true);
      }
   }
   
   // check to see if the path is already in our lookup table
   std::map<std::string, std::string>::iterator it = 
      s_idCache.find(path.absolutePath());
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

   // insert the new ID and update caches
   s_idCache[path.absolutePath()] = id;
   error = writeStringMapToFile(cache, s_idCache);
   if (error)
      return error;
   s_cacheWriteTime = std::time(NULL);
   *pId = id;

   return Success();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio
