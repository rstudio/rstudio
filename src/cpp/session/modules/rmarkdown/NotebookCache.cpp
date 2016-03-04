/*
 * NotebookCache.cpp
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
#include "SessionRnbParser.hpp"
#include "NotebookCache.hpp"
#include "NotebookChunkDefs.hpp"

#include <session/SessionUserSettings.hpp>
#include <session/SessionModuleContext.hpp>
#include <session/SessionSourceDatabase.hpp>

#include <r/RExec.hpp>
#include <r/RRoutines.hpp>

using namespace rstudio::core;

namespace rstudio {
namespace session {
namespace modules {
namespace rmarkdown {
namespace notebook {
namespace {

bool copyCacheItem(const FilePath& from,
                   const FilePath& to,
                   const FilePath& path)
{

   std::string relativePath = path.relativePath(from);
   FilePath target = to.complete(relativePath);

   Error error = path.isDirectory() ?
                     target.ensureDirectory() :
                     path.copy(target);
   if (error)
      LOG_ERROR(error);

   return true;
}

Error copyCache(const FilePath& from, const FilePath& to)
{
   Error error = to.ensureDirectory();
   if (error)
      return error;

   return from.childrenRecursive(
             boost::bind(copyCacheItem, from, to, _2));
}

void onDocRemoved(const std::string& docId, const std::string& docPath)
{
   Error error;

   FilePath cacheFolder = chunkCacheFolder(docPath, docId);
   FilePath defFile = chunkDefinitionsPath(docPath, docId, 
         userSettings().contextId());
   if (!docPath.empty() && defFile.exists())
   {
      // for saved documents, we want to keep the cache folder around even when
      // the document is closed, but only if the chunk definitions aren't out
      // of sync.
      FilePath docFile = module_context::resolveAliasedPath(docPath);
      std::time_t writeTime;
      error = getChunkDefs(docPath, docId, &writeTime, NULL);

      if (writeTime <= docFile.lastWriteTime())
      {
         // the doc has been saved since the last time the chunks defs were
         // updated, so no work to do here
         return;
      }
   }
   error = cacheFolder.removeIfExists();
   if (error)
      LOG_ERROR(error);
}

void onDocRenamed(const std::string& oldPath, 
                  boost::shared_ptr<source_database::SourceDocument> pDoc)
{
   Error error;
   bool removeOldDir = false;

   // compute cache folders and ignore if we can't safely adjust them
   FilePath oldCacheDir = chunkCacheFolder(oldPath, pDoc->id());
   FilePath newCacheDir = chunkCacheFolder(pDoc->path(), pDoc->id());
   if (!oldCacheDir.exists() || newCacheDir.exists())
      return;

   // if the doc was previously unsaved, we can just move the whole folder 
   // to its newly saved location
   if (oldPath.empty())
   {
      error = oldCacheDir.move(newCacheDir);
      if (error) 
      {
         // if we can't move the cache to the new location, we'll fall back to
         // copy/remove
         removeOldDir = true;
      }
      else
         return;
   }

   error = copyCache(oldCacheDir, newCacheDir);
   if (error)
   {
      LOG_ERROR(error);
   }
   else if (removeOldDir) 
   {
      // remove old dir if we couldn't move the folder above
      error = oldCacheDir.remove();
      if (error)
         LOG_ERROR(error);
   }
}

void onDocAdded(const std::string& id)
{
   std::string path;
   Error error = source_database::getPath(id, &path);
   if (error)
   {
      LOG_ERROR(error);
      return;
   }

   // ignore empty paths and non-R Markdown files
   if (path.empty())
      return;
   FilePath docPath = module_context::resolveAliasedPath(path);
   if (docPath.extensionLowerCase() != ".rmd")
      return;

   FilePath cachePath = chunkCacheFolder(path, id);
   FilePath nbPath = docPath.parent().complete(docPath.stem() + ".Rnb");

   if (!cachePath.exists() && nbPath.exists())
   {
      // we have a saved representation, but no cache -- populate the cache
      // from the saved representation
      error = parseRnb(nbPath, cachePath);
      if (error)
      {
         LOG_ERROR(error);
         return;
      }
   }

   // TODO: consider write times of document, cache, and .Rnb -- are there
   // combinations which would suggest we should overwrite the cache with the
   // contents of the notebook?
}

FilePath unsavedNotebookCache()
{
   return module_context::sessionScratchPath().childPath("unsaved-notebooks");
}

SEXP rs_populateNotebookCache(SEXP fileSEXP)
{
   std::string file = r::sexp::safeAsString(fileSEXP);
   FilePath cacheFolder = 
      chunkCacheFolder(file, "", userSettings().contextId());
   Error error = parseRnb(module_context::resolveAliasedPath(file), 
                          cacheFolder);
   if (error) 
      LOG_ERROR(error);

   r::sexp::Protect rProtect;
   return r::sexp::create(cacheFolder.absolutePath(), &rProtect);
}

} // anonymous namespace

FilePath chunkCacheFolder(const std::string& docPath, const std::string& docId,
      const std::string& contextId)
{
   FilePath folder;
   std::string stem;

   if (docPath.empty()) 
   {
      // the doc hasn't been saved, so keep its chunk output in the scratch
      // path
      folder = unsavedNotebookCache();
      stem = docId;
   }
   else
   {
      // the doc has been saved, so keep its chunk output alongside the doc
      // itself
      FilePath path = module_context::resolveAliasedPath(docPath);

#ifndef _WIN32
      // on non-Windows, use unix hidden folder 
      stem = ".";
#endif

      stem += path.stem();
      stem += "-" + contextId;
      folder = path.parent();
   }

   return folder.childPath(stem + ".Rnb.cached");
}

Error ensureCacheFolder(const FilePath& folder)
{
   Error error = folder.ensureDirectory();
   if (error)
      return error;
#ifdef _WIN32
   // on Windows, mark the directory hidden after creating it
   error = core::system::makeFileHidden(folder);
   if (error)
   {
      // non-fatal
      LOG_ERROR(error);
   }
#endif
   return error;
}

FilePath chunkCacheFolder(const std::string& docPath, const std::string& docId)
{
   return chunkCacheFolder(docPath, docId, userSettings().contextId());
}

Error initCache()
{
   source_database::events().onDocRenamed.connect(onDocRenamed);
   source_database::events().onDocRemoved.connect(onDocRemoved);
   source_database::events().onDocAdded.connect(onDocAdded);

   RS_REGISTER_CALL_METHOD(rs_populateNotebookCache, 1);

   return Success();
}

} // namespace notebook
} // namespace rmarkdown
} // namespace modules
} // namespace session
} // namespace rstudio

